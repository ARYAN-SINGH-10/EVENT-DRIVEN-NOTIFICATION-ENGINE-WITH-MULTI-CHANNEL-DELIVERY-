package com.notificationengine.event;

import com.notificationengine.dto.EventPayload;
import com.notificationengine.entity.NotificationEvent;
import com.notificationengine.repository.NotificationEventRepository;
import com.notificationengine.service.NotificationEventService;
import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Kafka consumer — active only when simulation mode is disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.simulation-mode", havingValue = "false")
public class KafkaNotificationConsumer {

    private final NotificationEventService eventService;
    private final NotificationEventRepository eventRepository;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    @KafkaListener(
            topics = "${app.kafka.topics.notification-events:notification-events}",
            groupId = "${spring.kafka.consumer.group-id:notification-engine-group}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(String message, Acknowledgment ack) {
        try {
            log.info("[KAFKA-CONSUMER] Received message: {}", message);
            
            // Deserialize event payload
            EventPayload payload = JsonUtil.fromJson(message, EventPayload.class)
                    .orElseThrow(() -> new IllegalArgumentException("Failed to parse Kafka message payload"));
            
            // Locate corresponding persisted event in DB
            Optional<NotificationEvent> eventOpt = eventRepository.findByCorrelationId(payload.getCorrelationId());
            java.util.UUID eventId = eventOpt.map(NotificationEvent::getId).orElse(null);

            // Process the event
            eventService.processEvent(payload, eventId);
            
            ack.acknowledge();
            log.debug("[KAFKA-CONSUMER] Message processed and acknowledged successfully");
        } catch (Exception e) {
            log.error("[KAFKA-CONSUMER] Failed to process message: {}", e.getMessage(), e);
            // Re-throw so DefaultErrorHandler and DeadLetterPublishingRecoverer handle retry and DLQ routing
            throw new RuntimeException("Kafka event processing failed: " + e.getMessage(), e);
        }
    }
}
