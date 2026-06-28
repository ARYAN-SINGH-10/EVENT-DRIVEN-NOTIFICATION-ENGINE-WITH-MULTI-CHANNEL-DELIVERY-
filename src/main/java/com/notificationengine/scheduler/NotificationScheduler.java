package com.notificationengine.scheduler;

import com.notificationengine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for Notification lifecycle management.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationScheduler {

    private final NotificationService notificationService;

    /**
     * Polls for PENDING or QUEUED notifications where scheduled_at <= now,
     * and triggers their dispatch.
     * Runs every 10 seconds.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.notification-process-interval:10000}")
    public void processPendingNotifications() {
        log.trace("Running scheduled task: processPendingNotifications");
        try {
            int count = notificationService.processPendingNotifications();
            if (count > 0) {
                log.info("Processed {} pending notifications", count);
            }
        } catch (Exception e) {
            log.error("Error in processPendingNotifications scheduled task: {}", e.getMessage());
        }
    }

    /**
     * Marks notifications as EXPIRED if their expires_at <= now
     * and they haven't been sent.
     * Runs every hour.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.notification-expiry-interval:3600000}")
    public void expireStaleNotifications() {
        log.trace("Running scheduled task: expireStaleNotifications");
        try {
            int count = notificationService.expireStaleNotifications();
            if (count > 0) {
                log.info("Expired {} stale notifications", count);
            }
        } catch (Exception e) {
            log.error("Error in expireStaleNotifications scheduled task: {}", e.getMessage());
        }
    }
}
