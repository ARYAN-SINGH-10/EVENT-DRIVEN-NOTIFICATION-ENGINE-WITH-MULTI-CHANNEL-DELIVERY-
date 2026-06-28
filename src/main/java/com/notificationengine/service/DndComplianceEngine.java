package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.taxonomy.FinancialEventType;

public interface DndComplianceEngine {

    /**
     * Validates whether a notification is compliant to be sent based on TRAI DND rules
     * and platform consent rules (e.g., WhatsApp).
     *
     * @param userId       the target user
     * @param phoneNumber  the recipient phone number (relevant for SMS, CALL, WHATSAPP)
     * @param channel      the delivery channel
     * @param taxonomy     the event taxonomy (determines if it's promotional/transactional)
     * @return true if compliant (should deliver), false if blocked
     */
    boolean validateCompliance(String userId, String phoneNumber, ChannelType channel, FinancialEventType taxonomy);
}
