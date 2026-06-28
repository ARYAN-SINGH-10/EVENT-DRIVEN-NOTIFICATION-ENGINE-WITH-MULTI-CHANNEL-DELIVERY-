package com.notificationengine.dlq;

import com.notificationengine.entity.DeadLetterQueueEntry;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.repository.DeadLetterQueueRepository;
import com.notificationengine.util.DateTimeUtil;
import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles enqueueing failed notifications into the Dead Letter Queue
 * and updating retry state with exponential backoff.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeadLetterQueueHandler {

    private final DeadLetterQueueRepository dlqRepository;

    @Value("${app.dlq.max-retry-attempts:5}")
    private int maxRetryAttempts;

    @Value("${app.dlq.initial-retry-delay-ms:1000}")
    private long initialRetryDelayMs;

    @Value("${app.dlq.retry-backoff-multiplier:2.0}")
    private double retryBackoffMultiplier;

    /**
     * Enqueues a failed notification in the DLQ.
     *
     * @param notification   the failed notification
     * @param failureReason  human-readable failure reason
     */
    @Transactional
    public DeadLetterQueueEntry enqueue(NotificationEntity notification, String failureReason) {
        // Check if entry already exists
        return dlqRepository.findByNotificationId(notification.getId())
                .map(existing -> incrementFailure(existing, failureReason))
                .orElseGet(() -> createNewEntry(notification, failureReason));
    }

    private DeadLetterQueueEntry createNewEntry(NotificationEntity notification, String failureReason) {
        LocalDateTime nextRetry = DateTimeUtil.calculateNextRetry(initialRetryDelayMs, 1, retryBackoffMultiplier);

        DeadLetterQueueEntry entry = DeadLetterQueueEntry.builder()
                .notificationId(notification.getId())
                .userId(notification.getUserId())
                .channelType(notification.getChannelType())
                .originalPayload(JsonUtil.toJson(notification))
                .failureReason(failureReason)
                .failureCount(1)
                .maxRetryAttempts(maxRetryAttempts)
                .status(DlqStatus.PENDING_RETRY)
                .nextRetryAt(nextRetry)
                .lastAttemptAt(LocalDateTime.now())
                .build();

        DeadLetterQueueEntry saved = dlqRepository.save(entry);
        log.warn("[DLQ] Enqueued notification={} channel={} reason='{}'",
                notification.getId(), notification.getChannelType(), failureReason);
        return saved;
    }

    private DeadLetterQueueEntry incrementFailure(DeadLetterQueueEntry entry, String failureReason) {
        entry.setFailureCount(entry.getFailureCount() + 1);
        entry.setFailureReason(failureReason);
        entry.setLastAttemptAt(LocalDateTime.now());

        if (entry.getFailureCount() >= entry.getMaxRetryAttempts()) {
            entry.setStatus(DlqStatus.EXHAUSTED);
            log.error("[DLQ] Max retries exhausted for notificationId={}", entry.getNotificationId());
        } else {
            entry.setStatus(DlqStatus.PENDING_RETRY);
            entry.setNextRetryAt(DateTimeUtil.calculateNextRetry(
                    initialRetryDelayMs, entry.getFailureCount(), retryBackoffMultiplier));
        }

        return dlqRepository.save(entry);
    }

    /**
     * Marks a DLQ entry as resolved.
     */
    @Transactional
    public void resolve(java.util.UUID dlqId, String resolvedBy, String notes) {
        dlqRepository.resolve(dlqId, DlqStatus.RESOLVED, LocalDateTime.now(), resolvedBy, notes);
        log.info("[DLQ] Resolved entry={} by={}", dlqId, resolvedBy);
    }

    /**
     * Returns entries ready for retry.
     */
    public java.util.List<DeadLetterQueueEntry> getEntriesReadyForRetry() {
        return dlqRepository.findEntriesReadyForRetry(LocalDateTime.now());
    }
}
