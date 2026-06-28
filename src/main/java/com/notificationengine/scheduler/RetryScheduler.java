package com.notificationengine.scheduler;

import com.notificationengine.service.DlqService;
import com.notificationengine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Unified retry scheduler that handles:
 * <ol>
 *   <li>Standard retry queue — picks up {@code RETRYING} notifications whose
 *       {@code scheduledAt} has elapsed and re-dispatches them with exponential backoff.</li>
 *   <li>Dead Letter Queue retry — picks up {@code PENDING_RETRY} DLQ entries whose
 *       {@code nextRetryAt} has elapsed and re-dispatches them.</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

    private final NotificationService notificationService;
    private final DlqService dlqService;

    /**
     * Polls the {@code RETRYING} notification pool every 30 seconds and
     * re-dispatches any notifications whose backoff window has elapsed.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.retry-interval-ms:30000}")
    public void processRetryingNotifications() {
        log.trace("[RETRY-SCHEDULER] Checking RETRYING notifications...");
        try {
            int count = notificationService.processRetryingNotifications();
            if (count > 0) {
                log.info("[RETRY-SCHEDULER] Re-dispatched {} RETRYING notification(s)", count);
            }
        } catch (Exception e) {
            log.error("[RETRY-SCHEDULER] Error in processRetryingNotifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Polls DLQ entries with status {@code PENDING_RETRY} every 60 seconds.
     * Uses the exponential backoff window calculated by {@link com.notificationengine.dlq.DeadLetterQueueHandler}.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.dlq-retry-interval:60000}")
    public void processDlqRetries() {
        log.trace("[DLQ-SCHEDULER] Checking PENDING_RETRY DLQ entries...");
        try {
            int count = dlqService.retryAll();
            if (count > 0) {
                log.info("[DLQ-SCHEDULER] Re-dispatched {} DLQ notification(s)", count);
            }
        } catch (Exception e) {
            log.error("[DLQ-SCHEDULER] Error in processDlqRetries: {}", e.getMessage(), e);
        }
    }
}
