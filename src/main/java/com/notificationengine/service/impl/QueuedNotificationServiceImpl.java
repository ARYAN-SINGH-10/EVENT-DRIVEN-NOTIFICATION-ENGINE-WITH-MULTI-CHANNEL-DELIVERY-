package com.notificationengine.service.impl;

import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.ScheduledNotification;
import com.notificationengine.entity.ScheduledNotification.ScheduleStatus;
import com.notificationengine.repository.ScheduledNotificationRepository;
import com.notificationengine.service.NotificationService;
import com.notificationengine.service.QueuedNotificationService;
import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QueuedNotificationServiceImpl implements QueuedNotificationService {

    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void queueForQuietHours(NotificationRequest request, LocalDateTime nextRunAt) {
        log.info("[QUIET-HOURS-QUEUE] Queueing notification for user={}, channel={}, nextRunAt={}", 
                request.getUserId(), request.getChannelType(), nextRunAt);

        ScheduledNotification scheduled = ScheduledNotification.builder()
                .userId(request.getUserId())
                .recipientAddress(request.getRecipientAddress())
                .channelType(request.getChannelType())
                .status(ScheduleStatus.SCHEDULED)
                .scheduledAt(LocalDateTime.now())
                .nextRunAt(nextRunAt)
                .metadata(JsonUtil.toJson(request)) // Store the full request in metadata so we can reconstruct it
                .build();

        scheduledNotificationRepository.save(scheduled);
    }

    /**
     * Runs every minute to process notifications that have exited quiet hours.
     */
    @Scheduled(fixedRate = 60000)
    @Override
    @Transactional
    public void processDueNotifications() {
        LocalDateTime now = LocalDateTime.now();
        List<ScheduledNotification> dueNotifications = scheduledNotificationRepository.findDueScheduledNotifications(now);

        if (dueNotifications.isEmpty()) {
            return; // Nothing to process
        }

        log.info("[QUIET-HOURS-DIGEST] Found {} due notifications exiting quiet hours", dueNotifications.size());

        // Group by user and channel
        Map<String, Map<ChannelType, List<ScheduledNotification>>> groupedByUserAndChannel = dueNotifications.stream()
                .collect(Collectors.groupingBy(
                        ScheduledNotification::getUserId,
                        Collectors.groupingBy(ScheduledNotification::getChannelType)
                ));

        for (Map.Entry<String, Map<ChannelType, List<ScheduledNotification>>> userEntry : groupedByUserAndChannel.entrySet()) {
            String userId = userEntry.getKey();
            
            for (Map.Entry<ChannelType, List<ScheduledNotification>> channelEntry : userEntry.getValue().entrySet()) {
                ChannelType channel = channelEntry.getKey();
                List<ScheduledNotification> queuedList = channelEntry.getValue();

                if (queuedList.size() == 1) {
                    // Send as-is
                    sendSingle(queuedList.get(0));
                } else {
                    // Morning Digest Generation
                    sendDigest(userId, channel, queuedList);
                }
            }
        }
    }

    private void sendSingle(ScheduledNotification scheduled) {
        try {
            JsonUtil.fromJson(scheduled.getMetadata(), NotificationRequest.class).ifPresentOrElse(
                request -> {
                    notificationService.send(request);
                    scheduled.setStatus(ScheduleStatus.COMPLETED);
                },
                () -> {
                    log.error("[QUIET-HOURS-DIGEST] Failed to parse NotificationRequest from metadata");
                    scheduled.setStatus(ScheduleStatus.FAILED);
                }
            );
        } catch (Exception e) {
            log.error("[QUIET-HOURS-DIGEST] Failed to send single queued notification: {}", scheduled.getId(), e);
            scheduled.setStatus(ScheduleStatus.FAILED);
        }
        scheduled.setLastRunAt(LocalDateTime.now());
        scheduledNotificationRepository.save(scheduled);
    }

    private void sendDigest(String userId, ChannelType channel, List<ScheduledNotification> queuedList) {
        log.info("[QUIET-HOURS-DIGEST] Generating morning digest for user={}, channel={}, msgCount={}", userId, channel, queuedList.size());
        
        try {
            // We use the first one to get the recipient address and standard fields
            NotificationRequest firstRequest = JsonUtil.fromJson(queuedList.get(0).getMetadata(), NotificationRequest.class)
                    .orElseThrow(() -> new IllegalStateException("Failed to parse NotificationRequest"));

            // Create a synthetic digest request
            NotificationRequest digestRequest = NotificationRequest.builder()
                    .userId(userId)
                    .recipientAddress(firstRequest.getRecipientAddress())
                    .channelType(channel)
                    .eventType("MORNING_DIGEST")
                    .templateName("MORNING_DIGEST")
                    .priority("NORMAL")
                    .templateVariables(Map.of("missedCount", String.valueOf(queuedList.size())))
                    .build();

            notificationService.send(digestRequest);

            // Mark all as completed
            LocalDateTime now = LocalDateTime.now();
            for (ScheduledNotification s : queuedList) {
                s.setStatus(ScheduleStatus.COMPLETED);
                s.setLastRunAt(now);
                scheduledNotificationRepository.save(s);
            }
        } catch (Exception e) {
            log.error("[QUIET-HOURS-DIGEST] Failed to generate digest for user={} channel={}", userId, channel, e);
            for (ScheduledNotification s : queuedList) {
                s.setStatus(ScheduleStatus.FAILED);
                scheduledNotificationRepository.save(s);
            }
        }
    }
}
