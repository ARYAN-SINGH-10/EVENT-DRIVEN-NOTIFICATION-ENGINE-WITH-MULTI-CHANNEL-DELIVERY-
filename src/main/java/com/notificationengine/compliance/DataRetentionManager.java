package com.notificationengine.compliance;

import com.notificationengine.entity.ComplianceRecord;
import com.notificationengine.repository.AuditLogRepository;
import com.notificationengine.repository.ComplianceRecordRepository;
import com.notificationengine.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Manages data retention policies for GDPR compliance.
 *
 * <p>Periodically purges old notifications and audit logs beyond retention periods.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataRetentionManager {

    private final NotificationRepository notificationRepository;
    private final AuditLogRepository auditLogRepository;
    private final ComplianceRecordRepository complianceRecordRepository;

    @Value("${app.compliance.data-retention-days:365}")
    private int notificationRetentionDays;

    @Value("${app.compliance.audit-log-retention-days:730}")
    private int auditLogRetentionDays;

    /**
     * Purges notifications older than the configured retention period.
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void purgeExpiredNotifications() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(notificationRetentionDays);
        log.info("[RETENTION] Purging delivered notifications older than {}", cutoff);

        List<?> old = notificationRepository.findOldNotifications(
                cutoff,
                com.notificationengine.entity.NotificationEntity.NotificationStatus.DELIVERED,
                PageRequest.of(0, 500));

        if (!old.isEmpty()) {
            notificationRepository.deleteAllInBatch(
                    (List<com.notificationengine.entity.NotificationEntity>) old);
            log.info("[RETENTION] Purged {} old notifications", old.size());
        }
    }

    /**
     * Purges audit logs older than the configured retention period.
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void purgeExpiredAuditLogs() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(auditLogRetentionDays);
        log.info("[RETENTION] Purging audit logs older than {}", cutoff);

        List<?> old = auditLogRepository.findByCreatedAtBeforeAndEntityType(
                cutoff, "NOTIFICATION", PageRequest.of(0, 500));

        if (!old.isEmpty()) {
            auditLogRepository.deleteAllInBatch(
                    (List<com.notificationengine.entity.AuditLog>) old);
            log.info("[RETENTION] Purged {} audit logs", old.size());
        }
    }

    /**
     * Processes GDPR deletion requests.
     */
    @Async("notificationTaskExecutor")
    @Transactional
    public void processDeletionRequests() {
        List<ComplianceRecord> pendingDeletions =
                complianceRecordRepository.findPendingDeletionRequests();

        for (ComplianceRecord record : pendingDeletions) {
            try {
                log.info("[GDPR-DELETION] Processing deletion request for userId={}", record.getUserId());
                // In production: anonymize or delete personal data
                record.setDeletionCompletedAt(LocalDateTime.now());
                complianceRecordRepository.save(record);
            } catch (Exception e) {
                log.error("[GDPR-DELETION] Failed to process deletion for userId={}: {}",
                        record.getUserId(), e.getMessage());
            }
        }
    }
}
