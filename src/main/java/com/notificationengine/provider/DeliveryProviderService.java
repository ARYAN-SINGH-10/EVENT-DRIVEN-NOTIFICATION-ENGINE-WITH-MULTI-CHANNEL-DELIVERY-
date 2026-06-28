package com.notificationengine.provider;

import com.notificationengine.entity.NotificationEntity.NotificationStatus;

import java.util.List;

/**
 * Service interface acting as the Provider Abstraction Layer.
 * Manages provider lookup, health tracking, validation, and sending.
 */
public interface DeliveryProviderService {

    /**
     * Resolves the configured/active provider for a given notification channel.
     *
     * @param channel the target channel
     * @return the resolved DeliveryProvider
     */
    DeliveryProvider getProvider(NotificationChannel channel);

    /**
     * Sends a message through the provider mapped to the given channel.
     *
     * @param channel the target channel
     * @param recipient the recipient's identifier
     * @param subject optional subject
     * @param body the message body
     * @return the external/simulation message ID
     */
    String send(NotificationChannel channel, String recipient, String subject, String body);

    /**
     * Validates if a recipient address format is valid for the specified channel.
     *
     * @param channel the channel
     * @param recipient the recipient identifier
     * @return true if format is valid, false otherwise
     */
    boolean validateRecipient(NotificationChannel channel, String recipient);

    /**
     * Queries the delivery status of a sent message from the channel provider.
     *
     * @param channel the channel
     * @param externalMessageId the external tracking ID
     * @return the current status of the message
     */
    NotificationStatus getDeliveryStatus(NotificationChannel channel, String externalMessageId);

    /**
     * Checks if the active provider for the channel is healthy.
     */
    boolean isChannelHealthy(NotificationChannel channel);

    /**
     * Returns a list of all registered delivery providers.
     */
    List<DeliveryProvider> getAllProviders();
}
