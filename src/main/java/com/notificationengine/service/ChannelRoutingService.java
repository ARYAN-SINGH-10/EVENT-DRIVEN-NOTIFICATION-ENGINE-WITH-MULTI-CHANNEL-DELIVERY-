package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity.ChannelType;

import java.util.List;
import java.util.Map;

/**
 * Service interface for the Intelligent Channel Routing Engine.
 */
public interface ChannelRoutingService {

    /**
     * Determines the optimal list of channels for routing a notification.
     *
     * @param userId the target user ID
     * @param eventType the type of event triggering the notification (e.g. TXNX-001)
     * @param priority the priority of the notification (e.g. LOW, NORMAL, HIGH, CRITICAL)
     * @return the list of optimal delivery channels
     */
    List<ChannelType> determineOptimalChannels(String userId, String eventType, String priority);

    /**
     * Calculates the individual routing scores for each available channel.
     * Useful for debugging and testing scores.
     *
     * @param userId the target user ID
     * @param eventType the event type
     * @param priority the event priority
     * @return a map of channel types and their computed routing scores (0.0 to 100.0)
     */
    Map<ChannelType, Double> getRoutingScores(String userId, String eventType, String priority);
}
