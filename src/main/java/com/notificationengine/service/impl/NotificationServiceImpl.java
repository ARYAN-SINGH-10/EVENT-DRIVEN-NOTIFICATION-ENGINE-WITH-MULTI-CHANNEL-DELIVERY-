package com.notificationengine.service.impl;

import com.notificationengine.analytics.AnalyticsAggregator;
import com.notificationengine.channel.ChannelProcessorFactory;
import com.notificationengine.channel.NotificationChannelProcessor;
import com.notificationengine.dlq.DeadLetterQueueHandler;
import com.notificationengine.dto.NotificationRequest;
import com.notificationengine.dto.NotificationResponse;
import com.notificationengine.dto.PagedResponse;
import com.notificationengine.entity.NotificationEntity;
import com.notificationengine.entity.NotificationEntity.ChannelType;
import com.notificationengine.entity.NotificationEntity.NotificationStatus;
import com.notificationengine.entity.NotificationTemplate;
import com.notificationengine.exception.ErrorCode;
import com.notificationengine.exception.NotificationException;
import com.notificationengine.preference.PreferenceEvaluator;
import com.notificationengine.repository.NotificationRepository;
import com.notificationengine.repository.NotificationTemplateRepository;
import com.notificationengine.service.NotificationService;
import com.notificationengine.template.TemplateEngine;
import com.notificationengine.template.TemplateVariableResolver;
import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Core notification service implementation handling the full send/retry/delivery lifecycle.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final ChannelProcessorFactory channelProcessorFactory;
    private final PreferenceEvaluator preferenceEvaluator;
    private final TemplateEngine templateEngine;
    private final TemplateVariableResolver variableResolver;
    private final AnalyticsAggregator analyticsAggregator;
    private final DeadLetterQueueHandler dlqHandler;
    private final com.notificationengine.service.NotificationStateMachineService stateMachineService;

    @org.springframework.beans.factory.annotation.Value("${app.dlq.initial-retry-delay-ms:30000}")
    private long initialRetryDelayMs;

    @org.springframework.beans.factory.annotation.Value("${app.dlq.retry-backoff-multiplier:2.0}")
    private double retryBackoffMultiplier;

    // -----------------------------------------------------------------------
    // Send
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public NotificationResponse send(NotificationRequest request) {
        // 1. Check user preference
        if (!preferenceEvaluator.isDeliveryAllowed(
                request.getUserId(), request.getChannelType(), request.getEventType())) {
            log.info("Notification blocked by user preference: userId={} channel={}",
                    request.getUserId(), request.getChannelType());
            throw new NotificationException(ErrorCode.USER_OPT_OUT,
                    "User has opted out of this notification", 409);
        }

        // 2. Resolve template body if template is specified
        String subject = request.getSubject();
        String body    = request.getBody();

        if (request.getTemplateId() != null || request.getTemplateName() != null) {
            NotificationTemplate template = resolveTemplate(
                    request.getTemplateId(), request.getTemplateName(), request.getChannelType());

            Map<String, String> variables = variableResolver.resolve(
                    request.getTemplateVariables(), null);

            subject = templateEngine.render(template.getSubjectTemplate(), variables);
            body    = templateEngine.render(template.getBodyTemplate(), variables);
        }

        // 3. Obtain NotificationEntity (fetch if exists, otherwise create)
        NotificationEntity entity;
        if (request.getNotificationId() != null) {
            entity = notificationRepository.findById(request.getNotificationId())
                    .orElseThrow(() -> new NotificationException(ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + request.getNotificationId(), 404));
            entity.setSubject(subject);
            entity.setBody(body);
            entity.setTemplateId(request.getTemplateId());
            entity.setTemplateVariables(JsonUtil.toJson(request.getTemplateVariables()));
            entity.setMaxRetries(request.getMaxRetries() > 0 ? request.getMaxRetries() : 3);
            entity.setScheduledAt(request.getScheduledAt());
            entity.setExpiresAt(request.getExpiresAt());
            entity.setMetadata(request.getMetadata());
            entity = notificationRepository.save(entity);
        } else {
            entity = NotificationEntity.builder()
                    .userId(request.getUserId())
                    .recipientAddress(request.getRecipientAddress())
                    .channelType(request.getChannelType())
                    .status(NotificationStatus.CREATED)
                    .subject(subject)
                    .body(body)
                    .templateId(request.getTemplateId())
                    .templateVariables(JsonUtil.toJson(request.getTemplateVariables()))
                    .eventType(request.getEventType())
                    .priority(request.getPriority() != null ? request.getPriority() : "NORMAL")
                    .maxRetries(request.getMaxRetries() > 0 ? request.getMaxRetries() : 3)
                    .scheduledAt(request.getScheduledAt())
                    .expiresAt(request.getExpiresAt())
                    .metadata(request.getMetadata())
                    .build();
            entity = notificationRepository.save(entity);
        }

        NotificationStatus nextState = request.getScheduledAt() != null ? NotificationStatus.QUEUED : NotificationStatus.PENDING;
        entity = stateMachineService.transitionState(entity, nextState, "Prepared for dispatch");

        // 4. If immediate (no schedule), dispatch now
        if (request.getScheduledAt() == null) {
            dispatch(entity);
        } else {
            log.info("Notification scheduled: id={} at={}", entity.getId(), request.getScheduledAt());
        }

        return toResponse(entity);
    }

    // -----------------------------------------------------------------------
    // Query
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getById(UUID id) {
        return notificationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + id, 404));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByUserId(String userId, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByUserId(userId, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByStatus(NotificationStatus status, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByStatus(status, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<NotificationResponse> getByChannelType(ChannelType channelType, Pageable pageable) {
        Page<NotificationResponse> page = notificationRepository
                .findByChannelType(channelType, pageable)
                .map(this::toResponse);
        return PagedResponse.from(page);
    }

    @Override
    @Transactional
    public NotificationResponse cancel(UUID id) {
        NotificationEntity entity = notificationRepository.findById(id)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found: " + id, 404));

        if (entity.getStatus() == NotificationStatus.DELIVERED
                || entity.getStatus() == NotificationStatus.CANCELLED) {
            throw new NotificationException(ErrorCode.NOTIFICATION_ALREADY_SENT,
                    "Cannot cancel a notification in status: " + entity.getStatus(), 409);
        }

        entity = stateMachineService.transitionState(entity, NotificationStatus.CANCELLED, "Cancelled by user");
        return toResponse(entity);
    }

    // -----------------------------------------------------------------------
    // Scheduler-invoked operations
    // -----------------------------------------------------------------------

    @Override
    @Transactional
    public int processPendingNotifications() {
        List<NotificationEntity> pending =
                notificationRepository.findPendingNotificationsReadyToSend(LocalDateTime.now());
        int count = 0;
        for (NotificationEntity notification : pending) {
            try {
                dispatch(notification);
                count++;
            } catch (Exception e) {
                log.error("Failed to process notification id={}: {}", notification.getId(), e.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional
    public int processRetryingNotifications() {
        List<NotificationEntity> retrying =
                notificationRepository.findRetryingNotificationsReadyToSend(LocalDateTime.now());
        int count = 0;
        for (NotificationEntity notification : retrying) {
            try {
                log.info("[RETRY] Re-dispatching notification id={} attempt={}/{}",
                        notification.getId(), notification.getRetryCount() + 1, notification.getMaxRetries());
                dispatch(notification);
                count++;
            } catch (Exception e) {
                log.error("[RETRY] Failed re-dispatch for notification id={}: {}", notification.getId(), e.getMessage());
            }
        }
        return count;
    }

    @Override
    @Transactional
    public void reDispatch(UUID notificationId) {
        NotificationEntity entity = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, "Notification not found for reDispatch: " + notificationId, 404));
        log.info("[RE-DISPATCH] Triggering re-dispatch for notificationId={} currentStatus={}",
                notificationId, entity.getStatus());
        dispatch(entity);
    }

    @Override
    @Transactional
    public int expireStaleNotifications() {
        return notificationRepository.expireStaleNotifications(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public long countByUserIdAndStatus(String userId, NotificationStatus status) {
        return notificationRepository.countByUserIdAndStatus(userId, status);
    }

    // -----------------------------------------------------------------------
    // Internal delivery
    // -----------------------------------------------------------------------

    @Async("notificationTaskExecutor")
    protected void dispatch(NotificationEntity notification) {
        try {
            notification = stateMachineService.transitionState(notification, NotificationStatus.PROCESSING, "Dispatching");

            NotificationChannelProcessor processor =
                    channelProcessorFactory.getProcessor(notification.getChannelType());

            long startMs = System.currentTimeMillis();
            String externalId = processor.send(notification);
            long deliveryMs = System.currentTimeMillis() - startMs;

            notification.setSentAt(LocalDateTime.now());
            notification.setExternalMessageId(externalId);
            notification = stateMachineService.transitionState(notification, NotificationStatus.SENT, "Dispatched to provider");

            analyticsAggregator.recordDeliveryEvent(
                    notification.getChannelType(), notification.getEventType(),
                    NotificationStatus.SENT, deliveryMs);

            log.info("Notification sent: id={} channel={} externalId={}",
                    notification.getId(), notification.getChannelType(), externalId);

        } catch (Exception e) {
            log.error("Notification delivery failed: id={} error={}", notification.getId(), e.getMessage());
            notification.setErrorMessage(e.getMessage());
            notification.setRetryCount(notification.getRetryCount() + 1);
            notification = stateMachineService.transitionState(notification, NotificationStatus.FAILED, "Dispatch failed: " + e.getMessage());

            // Enqueue in DLQ if max retries exceeded
            if (notification.getRetryCount() >= notification.getMaxRetries()) {
                notification = stateMachineService.transitionState(notification, NotificationStatus.DLQ, "Max retries exceeded");
                dlqHandler.enqueue(notification, e.getMessage());
            } else {
                // Calculate next retry time with exponential backoff
                LocalDateTime nextRetryAt = com.notificationengine.util.DateTimeUtil.calculateNextRetry(
                        initialRetryDelayMs, notification.getRetryCount(), retryBackoffMultiplier);
                notification.setScheduledAt(nextRetryAt);
                notification = stateMachineService.transitionState(notification, NotificationStatus.RETRYING,
                        String.format("Will retry at %s (attempt %d/%d)",
                                nextRetryAt, notification.getRetryCount(), notification.getMaxRetries()));
                log.warn("[RETRY] Notification id={} scheduled for retry at {} (attempt {}/{})",
                        notification.getId(), nextRetryAt, notification.getRetryCount(), notification.getMaxRetries());
            }

            analyticsAggregator.recordDeliveryEvent(
                    notification.getChannelType(), notification.getEventType(),
                    NotificationStatus.FAILED, null);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private NotificationTemplate resolveTemplate(UUID templateId, String templateName, ChannelType channel) {
        if (templateId != null) {
            return templateRepository.findById(templateId)
                    .orElseThrow(() -> new com.notificationengine.exception.TemplateNotFoundException(templateId.toString()));
        }
        return templateRepository.findByChannelTypeAndEventTypeAndActive(channel, templateName, true)
                .or(() -> templateRepository.findByNameAndActive(templateName, true))
                .orElseThrow(() -> new com.notificationengine.exception.TemplateNotFoundException(templateName));
    }

    private NotificationResponse toResponse(NotificationEntity e) {
        return NotificationResponse.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .recipientAddress(e.getRecipientAddress())
                .channelType(e.getChannelType())
                .status(e.getStatus())
                .subject(e.getSubject())
                .body(e.getBody())
                .templateId(e.getTemplateId())
                .eventType(e.getEventType())
                .priority(e.getPriority())
                .retryCount(e.getRetryCount())
                .externalMessageId(e.getExternalMessageId())
                .scheduledAt(e.getScheduledAt())
                .sentAt(e.getSentAt())
                .deliveredAt(e.getDeliveredAt())
                .expiresAt(e.getExpiresAt())
                .errorMessage(e.getErrorMessage())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
