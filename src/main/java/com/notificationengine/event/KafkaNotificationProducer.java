package com.notificationengine.event;

import com.notificationengine.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka notification producer — active only when simulation mode is disabled.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.kafka.simulation-mode", havingValue = "false")
public class KafkaNotificationProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.topics.notification-events:notification-events}")
    private String notificationEventsTopic;

    public void sendNotificationEvent(String key, Object payload) {
        String json = JsonUtil.toJson(payload);
        log.info("[KAFKA] Sending to topic={} key={}", notificationEventsTopic, key);
        kafkaTemplate.send(notificationEventsTopic, key, json)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[KAFKA] Failed to send message key={}: {}", key, ex.getMessage());
                    } else {
                        log.debug("[KAFKA] Sent: topic={} partition={} offset={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
