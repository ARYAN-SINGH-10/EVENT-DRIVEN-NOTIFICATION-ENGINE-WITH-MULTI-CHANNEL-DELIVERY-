package com.notificationengine.compliance;

import com.notificationengine.entity.ComplianceRecord;
import com.notificationengine.entity.ComplianceRecord.ConsentStatus;
import com.notificationengine.entity.ComplianceRecord.ConsentType;
import com.notificationengine.exception.ComplianceViolationException;
import com.notificationengine.repository.ComplianceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Manages user consent records for GDPR compliance.
 *
 * <p>Before sending any marketing notification, consent must be verified.
 * Transactional notifications (e.g., order confirmations) may bypass marketing consent.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsentManager {

    private final ComplianceRecordRepository complianceRecordRepository;

    @Value("${app.compliance.data-retention-days:365}")
    private int dataRetentionDays;

    /**
     * Records a consent decision for a user.
     */
    @Transactional
    public ComplianceRecord recordConsent(String userId, ConsentType consentType,
                                         ConsentStatus status, String ipAddress,
                                         String userAgent, String consentVersion) {
        Optional<ComplianceRecord> existing =
                complianceRecordRepository.findByUserIdAndConsentType(userId, consentType);

        ComplianceRecord record = existing.orElseGet(() -> ComplianceRecord.builder()
                .userId(userId)
                .consentType(consentType)
                .build());

        record.setConsentStatus(status);
        record.setIpAddress(ipAddress);
        record.setUserAgent(userAgent);
        record.setConsentVersion(consentVersion);

        if (status == ConsentStatus.GRANTED) {
            record.setConsentGivenAt(LocalDateTime.now());
            record.setDataRetentionUntil(LocalDateTime.now().plusDays(dataRetentionDays));
        } else if (status == ConsentStatus.REVOKED) {
            record.setConsentRevokedAt(LocalDateTime.now());
        } else if (status == ConsentStatus.DELETION_REQUESTED) {
            record.setDeletionRequestedAt(LocalDateTime.now());
        }

        ComplianceRecord saved = complianceRecordRepository.save(record);
        log.info("[CONSENT] userId={} consentType={} status={}", userId, consentType, status);
        return saved;
    }

    /**
     * Checks if a user has granted the required consent for a given notification type.
     *
     * @throws ComplianceViolationException if consent is not granted
     */
    public void assertConsentGranted(String userId, ConsentType consentType) {
        boolean hasConsent = complianceRecordRepository.existsByUserIdAndConsentTypeAndConsentStatus(
                userId, consentType, ConsentStatus.GRANTED);

        if (!hasConsent) {
            throw new ComplianceViolationException(userId,
                    "Required consent not granted: " + consentType);
        }
    }

    /**
     * Returns true if the user has granted consent, false otherwise.
     */
    public boolean hasConsent(String userId, ConsentType consentType) {
        return complianceRecordRepository.existsByUserIdAndConsentTypeAndConsentStatus(
                userId, consentType, ConsentStatus.GRANTED);
    }

    /**
     * Revokes all consents for a user (GDPR right to erasure).
     */
    @Transactional
    public void revokeAllConsents(String userId) {
        int count = complianceRecordRepository.revokeAllConsentsForUser(userId, LocalDateTime.now());
        log.info("[CONSENT] Revoked {} consents for userId={}", count, userId);
    }
}
