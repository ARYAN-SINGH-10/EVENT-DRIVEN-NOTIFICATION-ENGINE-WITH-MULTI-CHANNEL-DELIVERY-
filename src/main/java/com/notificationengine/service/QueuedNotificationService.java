package com.notificationengine.service;

import com.notificationengine.dto.NotificationRequest;
import java.time.LocalDateTime;

public interface QueuedNotificationService {

    /**
     * Queues a notification in the scheduled notifications repository.
     * 
     * @param request   The original notification request
     * @param nextRunAt The time (UTC) when the notification should be processed (e.g. end of quiet hours)
     */
    void queueForQuietHours(NotificationRequest request, LocalDateTime nextRunAt);

    /**
     * Finds notifications that have exited quiet hours, groups them by user,
     * and dispatches them (optionally as a digest).
     */
    void processDueNotifications();
}
