package com.notificationengine.service.impl;

import com.notificationengine.entity.DndAuditLog;
import com.notificationengine.entity.DndAuditLog.ComplianceAction;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.repository.DndAuditLogRepository;
import com.notificationengine.service.ConsentManagementService;
import com.notificationengine.service.DndComplianceEngine;
import com.notificationengine.service.DndRegistryService;
import com.notificationengine.taxonomy.FinancialEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DndComplianceEngineImpl implements DndComplianceEngine {

    private final DndRegistryService dndRegistryService;
    private final ConsentManagementService consentManagementService;
    private final DndAuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public boolean validateCompliance(String userId, String phoneNumber, ChannelType channel, FinancialEventType taxonomy) {
        log.debug("[COMPLIANCE-ENGINE] Validating compliance for user={} recipient={} channel={} event={}",
                userId, com.notificationengine.util.NotificationUtil.maskRecipient(phoneNumber, channel), channel, taxonomy);

        boolean isPromotional = taxonomy.name().startsWith("MKTG") || taxonomy.name().startsWith("PROMO");
        String category = "BANKING"; // In a real app, derive this from taxonomy.getCategory()

        // 1. WhatsApp Explicit Consent Check
        if (channel == ChannelType.WHATSAPP) {
            boolean hasWaConsent = consentManagementService.hasWhatsAppConsent(userId);
            if (!hasWaConsent) {
                logAudit(userId, phoneNumber, channel, taxonomy.name(), ComplianceAction.BLOCKED_NO_WHATSAPP_CONSENT, "Missing explicit WHATSAPP_EXPLICIT consent");
                return false; // Blocked
            }
        }

        // 2. Transactional Bypass
        // Transactional and Regulatory messages bypass DND checks entirely
        if (!isPromotional || taxonomy.isRegulatoryFlag()) {
            logAudit(userId, phoneNumber, channel, taxonomy.name(), ComplianceAction.ALLOWED_TRANSACTIONAL, "Transactional/Regulatory message bypasses DND");
            return true; // Allowed
        }

        // 3. Promotional DND Check for Telecom Channels (SMS, CALL)
        if (channel == ChannelType.SMS || channel == ChannelType.CALL) {
            boolean isBlocked = dndRegistryService.isNumberBlockedForCategory(phoneNumber, category);
            if (isBlocked) {
                logAudit(userId, phoneNumber, channel, taxonomy.name(), ComplianceAction.BLOCKED_DND, "Number is registered in TRAI DND for category " + category);
                return false; // Blocked
            }
        }

        // 4. General Promotional Consent Check (for Email, Push, In-App, etc.)
        boolean hasMarketingConsent = consentManagementService.hasPromotionalConsent(userId);
        if (!hasMarketingConsent && (channel == ChannelType.EMAIL || channel == ChannelType.PUSH)) {
            // For now, if no marketing consent, block emails/push promos
            logAudit(userId, phoneNumber, channel, taxonomy.name(), ComplianceAction.BLOCKED_DND, "Missing MARKETING consent for promotional message");
            return false;
        }

        // Passed all checks
        logAudit(userId, phoneNumber, channel, taxonomy.name(), ComplianceAction.ALLOWED_PROMOTIONAL, "Passed DND and Consent checks");
        return true;
    }

    private void logAudit(String userId, String phone, ChannelType channel, String eventType, ComplianceAction action, String reason) {
        log.info("[COMPLIANCE-ENGINE] Action={} | User={} | Recipient={} | Channel={} | Event={} | Reason='{}'",
                action, userId, com.notificationengine.util.NotificationUtil.maskRecipient(phone, channel), channel, eventType, reason);

        DndAuditLog audit = DndAuditLog.builder()
                .userId(userId)
                .phoneNumber(phone)
                .channelType(channel)
                .eventType(eventType)
                .actionTaken(action)
                .reason(reason)
                .build();
        auditLogRepository.save(audit);
    }
}
