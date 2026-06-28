package com.notificationengine.service.impl;

import com.notificationengine.dlq.DeadLetterQueueHandler;
import com.notificationengine.dto.DlqResponse;
import com.notificationengine.dto.DlqStatsResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.DeadLetterQueueEntry;
import com.notificationengine.entity.DeadLetterQueueEntry.DlqStatus;
import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.exception.DlqException;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.repository.DeadLetterQueueRepository;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.service.DlqService;
import com.notificationengine.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DlqServiceImpl implements DlqService {

    private final DeadLetterQueueRepository dlqRepository;
    private final DeadLetterQueueHandler dlqHandler;
    private final NotificationRepository notificationRepository;

    /**
     * @Lazy to break circular dependency: NotificationService -> DlqHandler -> DlqService -> NotificationService
     */
    private final NotificationService notificationService;

    public DlqServiceImpl(
            DeadLetterQueueRepository dlqRepository,
            DeadLetterQueueHandler dlqHandler,
            NotificationRepository notificationRepository,
            @Lazy NotificationService notificationService) {
        this.dlqRepository = dlqRepository;
        this.dlqHandler = dlqHandler;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<DlqResponse> getByStatus(DlqStatus status, Pageable pageable) {
        Page<DlqResponse> page = dlqRepository.findByStatus(status, pageable).map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public DlqResponse getById(UUID id) {
        return dlqRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new DlqException("DLQ entry not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqResponse> getByUserId(String userId) {
        return dlqRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }


    // -------------------------------------------------------------------------
    // State Mutations
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public DlqResponse resolve(UUID id, String resolvedBy, String notes) {
        DeadLetterQueueEntry entry = fetchEntry(id);

        if (entry.getStatus() == DlqStatus.RESOLVED) {
            throw new DlqException(ErrorCode.DLQ_ALREADY_RESOLVED, "Entry already resolved: " + id);
        }

        dlqHandler.resolve(id, resolvedBy, notes);
        return toResponse(dlqRepository.findById(id).orElseThrow());
    }

    @Override
    @Transactional
    public DlqResponse discard(UUID id, String reason) {
        DeadLetterQueueEntry entry = fetchEntry(id);

        if (entry.getStatus() == DlqStatus.RESOLVED || entry.getStatus() == DlqStatus.DISCARDED) {
            throw new DlqException(ErrorCode.DLQ_ALREADY_RESOLVED,
                    "Entry already in terminal state " + entry.getStatus() + ": " + id);
        }

        entry.setStatus(DlqStatus.DISCARDED);
        entry.setResolutionNotes(reason);
        entry.setResolvedAt(LocalDateTime.now());
        return toResponse(dlqRepository.save(entry));
    }

    /**
     * Replays a specific DLQ entry by re-dispatching its original notification.
     * Resets the DLQ entry to PENDING_RETRY so it won't duplicate in automated scheduler.
     */
    @Override
    @Transactional
    public DlqResponse replay(UUID id) {
        DeadLetterQueueEntry entry = fetchEntry(id);

        if (entry.getStatus() == DlqStatus.DISCARDED) {
            throw new DlqException(ErrorCode.DLQ_ALREADY_RESOLVED,
                    "Cannot replay a discarded DLQ entry: " + id);
        }
        if (entry.getStatus() == DlqStatus.RESOLVED) {
            throw new DlqException(ErrorCode.DLQ_ALREADY_RESOLVED,
                    "Cannot replay an already resolved DLQ entry: " + id);
        }

        UUID notificationId = entry.getNotificationId();
        log.info("[DLQ-REPLAY] Manual replay triggered for dlqId={} notificationId={}", id, notificationId);

        // Reset the notification state to PENDING so the state machine allows PROCESSING
        notificationRepository.findById(notificationId).ifPresentOrElse(notification -> {
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }, () -> {
            throw new DlqException("Original notification not found for DLQ entry: " + id);
        });

        // Mark DLQ entry as retrying
        entry.setStatus(DlqStatus.RETRYING);
        entry.setLastAttemptAt(LocalDateTime.now());
        dlqRepository.save(entry);

        // Re-dispatch (async)
        notificationService.reDispatch(notificationId);

        return toResponse(dlqRepository.findById(id).orElseThrow());
    }

    // -------------------------------------------------------------------------
    // Bulk Retry (called by DlqRetryScheduler)
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public int retryAll() {
        List<DeadLetterQueueEntry> entries = dlqHandler.getEntriesReadyForRetry();
        if (entries.isEmpty()) {
            return 0;
        }
        log.info("[DLQ-RETRY] Processing {} entries ready for retry", entries.size());

        int dispatched = 0;
        for (DeadLetterQueueEntry entry : entries) {
            try {
                UUID notificationId = entry.getNotificationId();

                // Reset notification to PENDING so state machine allows re-transition
                notificationRepository.findById(notificationId).ifPresent(notification -> {
                    notification.setStatus(NotificationStatus.PENDING);
                    notificationRepository.save(notification);
                });

                entry.setStatus(DlqStatus.RETRYING);
                entry.setLastAttemptAt(LocalDateTime.now());
                dlqRepository.save(entry);

                notificationService.reDispatch(notificationId);
                dispatched++;

            } catch (Exception e) {
                log.error("[DLQ-RETRY] Failed to re-dispatch dlqEntryId={}: {}", entry.getId(), e.getMessage());
                // Increment failure via handler so it moves to EXHAUSTED if too many failures
                dlqHandler.enqueue(
                        notificationRepository.findById(entry.getNotificationId()).orElseThrow(),
                        e.getMessage());
            }
        }
        return dispatched;
    }

    // -------------------------------------------------------------------------
    // Monitoring
    // -------------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public DlqStatsResponse getStats() {
        Map<String, Long> countsByStatus = new LinkedHashMap<>();
        Arrays.stream(DlqStatus.values())
                .forEach(s -> countsByStatus.put(s.name(), dlqRepository.countByStatus(s)));

        long totalEntries = countsByStatus.values().stream().mapToLong(Long::longValue).sum();

        Map<String, Long> pendingRetryByChannel = new LinkedHashMap<>();
        for (NotificationEntity.ChannelType ch : NotificationEntity.ChannelType.values()) {
            long count = dlqRepository.countPendingByChannel(ch);
            if (count > 0) {
                pendingRetryByChannel.put(ch.name(), count);
            }
        }

        long dueForRetry = dlqRepository.findEntriesReadyForRetry(LocalDateTime.now()).size();

        return DlqStatsResponse.builder()
                .totalEntries(totalEntries)
                .countsByStatus(countsByStatus)
                .pendingRetryByChannel(pendingRetryByChannel)
                .dueForRetry(dueForRetry)
                .snapshotAt(LocalDateTime.now())
                .build();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private DeadLetterQueueEntry fetchEntry(UUID id) {
        return dlqRepository.findById(id)
                .orElseThrow(() -> new DlqException("DLQ entry not found: " + id));
    }

    private DlqResponse toResponse(DeadLetterQueueEntry e) {
        return DlqResponse.builder()
                .id(e.getId()).notificationId(e.getNotificationId())
                .userId(e.getUserId()).channelType(e.getChannelType())
                .failureReason(e.getFailureReason()).failureCount(e.getFailureCount())
                .maxRetryAttempts(e.getMaxRetryAttempts()).status(e.getStatus())
                .nextRetryAt(e.getNextRetryAt()).lastAttemptAt(e.getLastAttemptAt())
                .resolvedAt(e.getResolvedAt()).resolvedBy(e.getResolvedBy())
                .resolutionNotes(e.getResolutionNotes()).createdAt(e.getCreatedAt())
                .build();
    }
}
