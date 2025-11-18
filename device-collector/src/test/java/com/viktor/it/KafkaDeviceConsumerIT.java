package com.viktor.it;

import com.viktor.avro.DeviceAvro;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.utils.DeviceUtilData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class KafkaDeviceConsumerIT extends AbstractIntegrationTest {

    @MockitoSpyBean
    private DeviceProcessor processor;

    @Autowired
    private KafkaTemplate<Object, Object> kafkaTemplate;

    @Test
    @DisplayName("Consume message from 'devices' topic — should process save/update")
    void givenDeviceMessage_whenConsumedFromDevicesTopic_thenProcessSaveOrUpdate() {

        DeviceAvro device = DeviceUtilData.getDevice1();

        kafkaTemplate.send("devices", device.getDeviceId(), device);

        verify(processor, timeout(5000).times(1)).processDevices(anyList());
    }

    @Test
    @DisplayName("Consume message from 'devices-delete' topic — should process delete")
    void givenDeviceMessage_whenConsumedFromDevicesDeleteTopic_thenProcessDelete() {

        DeviceAvro device = DeviceUtilData.getDevice1();

        kafkaTemplate.send("devices-delete", device.getDeviceId(), device);

        verify(processor, timeout(5000).times(1)).deleteDevices(anyList());
    }
}
