package com.viktor.it;

import com.viktor.avro.DeviceEventAvro;
import com.viktor.avro.DltError;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.util.EventDataUtil;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.Duration;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class EventProcessorDltIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @MockitoSpyBean
    private EventCollectorRepository eventCollectorRepository;

    @MockitoSpyBean
    private DevicePublicationRepository devicePublicationRepository;

    private KafkaConsumer<String, DltError> createDltConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "dlt-test-group");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                "io.confluent.kafka.serializers.KafkaAvroDeserializer");

        props.put("schema.registry.url", schemaRegistryUrl());
        props.put("specific.avro.reader", true);

        return new KafkaConsumer<>(props);
    }

    private String kafkaBootstrapServers() {
        return "PLAINTEXT_HOST://" + kafka.getHost() + ":" + kafka.getMappedPort(9094);
    }

    private String schemaRegistryUrl() {
        return "http://" + HOST + ":" + schemaRegistry.getMappedPort(8081);
    }

    @Test
    @DisplayName("Given publication save failure When processing device event Then message is routed to DLT")
    void givenPublicationSaveFailure_whenProcessingDeviceEvent_thenMessageIsRoutedToDlt() throws Exception {
        DeviceEventAvro deviceEventAvro = EventDataUtil.getDeviceEventAvro();

        RuntimeException ex = new RuntimeException("DB failure");
        Mockito.doThrow(ex)
                .when(devicePublicationRepository)
                .save(any());

        kafkaTemplate.send("events", deviceEventAvro).get();

        Thread.sleep(5000);

        try (KafkaConsumer<String, DltError> dltConsumer = createDltConsumer()) {

            dltConsumer.subscribe(List.of("dlt-topic"));

            ConsumerRecords<String, DltError> records =
                    dltConsumer.poll(Duration.ofSeconds(10));

            assertThat(records.count())
                    .as("DLT topic must contain 1 failed message")
                    .isEqualTo(1);

            DltError dltMsg = records.iterator().next().value();

            assertThat(dltMsg.getReason())
                    .isEqualTo(ex.getMessage());
        }
    }


    @Test
    @DisplayName("Given save failure When processing device event Then message is routed to DLT")
    void givenSaveFailure_whenProcessingDeviceEvent_thenMessageRoutedToDlt() throws Exception {
        DeviceEventAvro deviceEventAvro = EventDataUtil.getDeviceEventAvro();

        RuntimeException ex = new RuntimeException("DB failure");
        Mockito.doThrow(ex)
                .when(eventCollectorRepository)
                .save(any());

        kafkaTemplate.send("events", deviceEventAvro).get();

        Thread.sleep(5000);

        try (KafkaConsumer<String, DltError> dltConsumer = createDltConsumer()) {

            dltConsumer.subscribe(List.of("dlt-topic"));

            ConsumerRecords<String, DltError> records =
                    dltConsumer.poll(Duration.ofSeconds(10));

            assertThat(records.count())
                    .as("DLT topic must contain 1 failed message")
                    .isEqualTo(1);

            DltError dltMsg = records.iterator().next().value();

            assertThat(dltMsg.getReason())
                    .isEqualTo(ex.getMessage());
        }
    }
}
