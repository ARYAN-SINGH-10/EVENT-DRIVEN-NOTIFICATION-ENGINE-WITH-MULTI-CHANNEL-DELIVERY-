package com.notificationengine.event;

import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.service.NotificationEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publishes domain events by delegating to the NotificationEventService.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationEventPublisher {

    private final NotificationEventService eventService;

    /**
     * Publishes an event payload and persists it to the event store.
     *
     * @param payload the event payload to publish
     * @return the persisted NotificationEvent
     */
    public NotificationEvent publish(EventPayload payload) {
        log.info("Publishing event through NotificationEventPublisher: type={}", payload.getEventType());
        return eventService.ingestEvent(payload);
    }
}
