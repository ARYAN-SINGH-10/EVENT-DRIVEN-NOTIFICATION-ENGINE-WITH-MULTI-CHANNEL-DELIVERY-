package com.notificationengine.provider;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;

/**
 * Strategy interface for individual notification delivery providers.
 */
public interface DeliveryProvider {

    /**
     * Gets the notification channel supported by this provider.
     */
    NotificationChannel getChannel();

    /**
     * Gets the unique provider name.
     */
    String getName();

    /**
     * Sends the notification message.
     *
     * @param recipient the target address or identifier
     * @param subject the subject (optional)
     * @param body the message body
     * @return the external/simulation message ID
     */
    String send(String recipient, String subject, String body);

    /**
     * Validates if the recipient address format matches what is expected.
     */
    boolean validateRecipient(String recipient);

    /**
     * Queries the status of a sent message using its external message ID.
     */
    NotificationStatus getDeliveryStatus(String externalMessageId);

    /**
     * Checks if the provider is currently healthy and operational.
     */
    boolean isHealthy();
}
