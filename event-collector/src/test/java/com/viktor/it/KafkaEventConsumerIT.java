package com.viktor.it;


import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DeviceEventAvro;
import com.viktor.service.event_collector.EventCollectorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class KafkaEventConsumerIT extends AbstractIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @MockitoSpyBean
    private EventCollectorService eventCollectorService;

    @Test
    @DisplayName("Should consume Kafka event and invoke EventCollectorService")
    void givenKafkaEvent_whenConsumed_thenEventCollectorServiceIsInvoked() throws InterruptedException {
        String deviceId = "dev-chain-1";
        DeviceAvro device = new DeviceAvro();
        device.setDeviceId(deviceId);
        device.setDeviceType("type");
        device.setCreatedAt(System.currentTimeMillis());
        device.setMeta("meta");

        DeviceEventAvro event = new DeviceEventAvro("evt-cons-1", device, System.currentTimeMillis(), "TEST", "payload");
        kafkaTemplate.send("events", event);

        verify(eventCollectorService, timeout(5000))
                .processEvents(Mockito.argThat(list -> list.stream().anyMatch(e -> e.getDevice().getDeviceId().equals(deviceId))));
    }
}
