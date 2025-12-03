package com.viktor.device_service.it;

import com.viktor.avro.DeviceAvro;
import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.kafka.KafkaDeviceProducer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class KafkaDeviceProducerIT extends AbstractIntegrationTest {

    @Autowired
    KafkaDeviceProducer producer;

    private KafkaConsumer<String, DeviceAvro> consumer;

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getHost() + ":" + kafka.getMappedPort(9094));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        props.put("schema.registry.url", "http://" + HOST + ":" + schemaRegistry.getMappedPort(8081));
        props.put("specific.avro.reader", true);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        consumer = new KafkaConsumer<>(props);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    @DisplayName("given valid device when sendToCreateUpdateTopic then message is published to 'devices'")
    void givenValidDevice_whenSendToCreateUpdateTopic_thenMessagePublished() {
        DeviceDto dto = new DeviceDto("dev123", "TYPE", System.currentTimeMillis(), "meta");

        producer.sendToCreateUpdateTopic(dto);
        consumer.subscribe(List.of("devices"));

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var records = consumer.poll(Duration.ofMillis(500));
                    assertFalse(records.isEmpty(), "Kafka did not receive messages!");

                    DeviceAvro avro = records.iterator().next().value();
                    assertEquals(dto.getDeviceId(), avro.getDeviceId());
                    assertEquals(dto.getDeviceType(), avro.getDeviceType());
                });
    }

    @Test
    @DisplayName("given valid device when sendToDeleteTopic then message is published to 'devices-delete'")
    void givenValidDevice_whenSendToDeleteTopic_thenMessagePublished() {
        DeviceDto dto = new DeviceDto("dev123", "TYPE", System.currentTimeMillis(), "meta");

        producer.sendToDeleteTopic(dto);
        consumer.subscribe(List.of("devices-delete"));

        await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    var records = consumer.poll(Duration.ofMillis(500));
                    assertFalse(records.isEmpty(), "Kafka did not receive messages!");

                    DeviceAvro avro = records.iterator().next().value();
                    assertEquals(dto.getDeviceId(), avro.getDeviceId());
                    assertEquals(dto.getDeviceType(), avro.getDeviceType());
                });
    }
}