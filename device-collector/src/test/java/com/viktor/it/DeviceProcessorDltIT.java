package com.viktor.it;

import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DltError;
import com.viktor.dao.DeviceDao;
import com.viktor.exception.ShardErrorException;
import com.viktor.utils.DeviceUtilData;
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
public class DeviceProcessorDltIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @MockitoSpyBean
    private DeviceDao deviceDao;

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
    @DisplayName("Given shard failure When processing device Then message ends up in DLT")
    void shouldSendToDltOnShardFailure() throws Exception {

        DeviceAvro device = DeviceUtilData.getDevice1();

        ShardErrorException ex = new ShardErrorException("DB failure", device.getDeviceId());
        Mockito.doThrow(ex)
                .when(deviceDao)
                .save(any());

        kafkaTemplate.send("devices", device).get();

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
