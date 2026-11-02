package com.notificationengine.integration;

import com.notificationengine.config.KafkaConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext
@EmbeddedKafka(
        partitions = 1,
        topics = {"notification-events", "notification-dlq", "notification-analytics"}
)
@TestPropertySource(properties = {
        "app.kafka.simulation-mode=false",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "app.kafka.topics.notification-events=notification-events",
        "app.kafka.topics.notification-dlq=notification-dlq",
        "spring.datasource.url=jdbc:h2:mem:kafka_test_db;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=false"
})
@Import(KafkaConfig.class)
public class RealKafkaIntegrationTest {

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Test
    @DisplayName("Real Kafka Broker: Publish event to notification-events and consume")
    void testRealKafkaProduceAndConsume() {
        assertNotNull(embeddedKafkaBroker, "Embedded Kafka Broker should be initialized and running");
        String brokers = embeddedKafkaBroker.getBrokersAsString();
        assertTrue(brokers != null && !brokers.isEmpty(), "Kafka broker address must be non-empty");

        // 1. Setup Producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        DefaultKafkaProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer());
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

        // 2. Setup Consumer
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group-" + System.currentTimeMillis(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> consumer = consumerFactory.createConsumer();
        consumer.subscribe(Collections.singletonList("notification-events"));

        // 3. Send test event over the wire
        String payload = "{\"eventType\":\"PAYMENT_SUCCESS\",\"userId\":\"usr_kafka_real\",\"amount\":100.0}";
        template.send("notification-events", "KEY-001", payload);
        template.flush();

        // 4. Read record from real Kafka broker
        ConsumerRecord<String, String> singleRecord = KafkaTestUtils.getSingleRecord(consumer, "notification-events", Duration.ofSeconds(10));
        assertNotNull(singleRecord, "Real Kafka broker must receive and serve published record");
        assertEquals("KEY-001", singleRecord.key());
        assertEquals(payload, singleRecord.value());
        consumer.close();
    }

    @Test
    @DisplayName("Real Kafka Broker: Dead-letter routing on notification-dlq")
    void testRealKafkaDeadLetterRouting() {
        // Setup Producer
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        DefaultKafkaProducerFactory<String, String> producerFactory =
                new DefaultKafkaProducerFactory<>(producerProps, new StringSerializer(), new StringSerializer());
        KafkaTemplate<String, String> template = new KafkaTemplate<>(producerFactory);

        // Setup Consumer for DLQ
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("dlq-test-group-" + System.currentTimeMillis(), "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        DefaultKafkaConsumerFactory<String, String> consumerFactory =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer());
        Consumer<String, String> dlqConsumer = consumerFactory.createConsumer();
        dlqConsumer.subscribe(Collections.singletonList("notification-dlq"));

        // Send a message directly simulating dead letter publish
        String dlqPayload = "{\"error\":\"POISON_PILL\",\"originalTopic\":\"notification-events\"}";
        template.send("notification-dlq", "POISON-KEY", dlqPayload);
        template.flush();

        ConsumerRecord<String, String> dlqRecord = KafkaTestUtils.getSingleRecord(dlqConsumer, "notification-dlq", Duration.ofSeconds(10));
        assertNotNull(dlqRecord, "DLQ topic must receive the dead letter record from real broker");
        assertEquals("POISON-KEY", dlqRecord.key());
        assertTrue(dlqRecord.value().contains("POISON_PILL"));
        dlqConsumer.close();
    }
}
