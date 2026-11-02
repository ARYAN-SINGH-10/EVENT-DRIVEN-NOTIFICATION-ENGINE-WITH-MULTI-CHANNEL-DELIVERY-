package com.notificationengine.service;

import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.NotificationEvent;

import java.util.UUID;

/**
 * Service interface for the Event Ingestion Engine.
 * Handles validation, persistence, and dispatch of incoming domain events.
 */
public interface NotificationEventService {

    /**
     * Accepts and validates an incoming event, persists it to the event store,
     * and triggers asynchronous publishing.
     *
     * @param payload the incoming event payload
     * @return the saved NotificationEvent entity
     */
    NotificationEvent ingestEvent(EventPayload payload);

    /**
     * Asynchronously publishes an event to the messaging bus (Kafka or simulation bus).
     *
     * @param eventId the ID of the event to publish
     */
    void publishEventAsync(UUID eventId);

    /**
     * Processes the event payload, translates it to notification requests,
     * and dispatches them via the notification service.
     *
     * @param payload the event payload to process
     * @param eventId the database ID of the event being processed (optional)
     */
    void processEvent(EventPayload payload, UUID eventId);

    /**
     * Recovers in-flight unprocessed events across application restarts.
     *
     * @return the number of events recovered
     */
    int recoverUnprocessedEvents();
}
