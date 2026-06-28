package com.notificationengine.service;

import com.notificationengine.entity.NotificationEntity.ChannelType;

public interface FrequencyCapService {

    /**
     * Checks if the notification is allowed under current frequency rules.
     * Rules evaluated include:
     * - Global daily cap (Max 12)
     * - Per channel cap (SMS: 5, Push: 8, Email: 3)
     * - Per category cap (Max 3/day)
     * - Cooldown period (1 hour since last notification of this type)
     *
     * @param userId    the target user ID
     * @param channel   the delivery channel
     * @param eventType the type of event (acting as the category)
     * @param priority  the priority level of the event (CRITICAL bypasses all caps)
     * @return true if within caps, false if rate limited
     */
    boolean isWithinCaps(String userId, ChannelType channel, String eventType, String priority);
}
