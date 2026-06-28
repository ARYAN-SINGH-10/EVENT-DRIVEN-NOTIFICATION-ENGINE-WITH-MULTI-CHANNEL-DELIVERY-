package com.notificationengine.event;

import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.service.NotificationEventService;
import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link DomainNotificationEvent} and triggers async notification processing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationEventService eventService;

    @Value("${app.kafka.simulation-mode:true}")
    private boolean simulationMode;

    /**
     * Handles a domain notification event asynchronously.
     *
     * @param event the domain event
     */
    @Async("notificationTaskExecutor")
    @EventListener
    public void onNotificationEvent(DomainNotificationEvent event) {
        NotificationEvent notificationEvent = event.getNotificationEvent();
        log.info("[EVENT-LISTENER] Received event: id={} type={} user={} correlationId={}",
                notificationEvent.getId(),
                notificationEvent.getEventType(),
                notificationEvent.getUserId(),
                notificationEvent.getCorrelationId());

        if (simulationMode) {
            log.info("[EVENT-LISTENER] Simulation mode active. Triggering notification dispatch for event id={}",
                    notificationEvent.getId());
            try {
                EventPayload payload = JsonUtil.fromJson(notificationEvent.getPayload(), EventPayload.class)
                        .orElseThrow(() -> new IllegalArgumentException("Failed to parse event payload"));
                eventService.processEvent(payload, notificationEvent.getId());
            } catch (Exception e) {
                log.error("[EVENT-LISTENER] Failed to process event id={} internally: {}",
                        notificationEvent.getId(), e.getMessage(), e);
            }
        } else {
            log.debug("[EVENT-LISTENER] Simulation mode disabled. Event will be processed via Kafka consumer instead.");
        }
    }
}
