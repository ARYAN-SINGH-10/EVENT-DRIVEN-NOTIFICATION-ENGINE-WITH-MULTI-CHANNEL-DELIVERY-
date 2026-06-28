package com.notificationengine.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory simulated event bus for Kafka simulation mode.
 *
 * <p>When {@code app.kafka.simulation-mode=true}, this bus stores published events
 * in memory (useful for testing and local development without a real Kafka broker).</p>
 */
@Slf4j
@Component
public class SimulatedEventBus {

    private final Map<String, ConcurrentLinkedQueue<String>> topicQueues = new ConcurrentHashMap<>();

    /**
     * Publishes a message to a simulated topic.
     *
     * @param topic   the topic name
     * @param message the message payload (JSON string)
     */
    public void publish(String topic, String message) {
        topicQueues.computeIfAbsent(topic, k -> new ConcurrentLinkedQueue<>()).offer(message);
        log.info("[SIMULATED-BUS] Published to topic='{}' — queue size={}",
                topic, topicQueues.get(topic).size());
    }

    /**
     * Polls (retrieves and removes) the next message from a simulated topic.
     *
     * @param topic the topic name
     * @return message payload or null if empty
     */
    public String poll(String topic) {
        ConcurrentLinkedQueue<String> queue = topicQueues.get(topic);
        return queue != null ? queue.poll() : null;
    }

    /**
     * Returns the current queue depth for a topic.
     */
    public int queueDepth(String topic) {
        ConcurrentLinkedQueue<String> queue = topicQueues.get(topic);
        return queue != null ? queue.size() : 0;
    }

    /**
     * Returns all topic names with pending messages.
     */
    public java.util.Set<String> getTopics() {
        return topicQueues.keySet();
    }
}
