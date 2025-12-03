package com.viktor.kafka;

import com.viktor.avro.DeviceAvro;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.utils.DeviceUtilData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class KafkaDeviceConsumerMockTest {

    @Mock
    DeviceProcessor processor;

    @Mock
    Acknowledgment ack;

    @InjectMocks
    KafkaDeviceConsumer consumer;

    @Test
    @DisplayName("consume devices — should invoke processDevices and acknowledge")
    void givenAvroRecord_whenConsumed_thenProcessorInvoked_andAcked() {
        var device = DeviceUtilData.getDevice1();

        ConsumerRecord<String, DeviceAvro> record =
                new ConsumerRecord<>("devices", 0, 0L, "key", device);

        List<ConsumerRecord<String, DeviceAvro>> list = List.of(record);

        BDDMockito.given(processor.processDevices(anyList()))
                .willReturn(Flux.empty());

        consumer.listenDevices(list, ack);

        BDDMockito.then(processor).should(times(1)).processDevices(List.of(device));
        BDDMockito.then(ack).should(times(1)).acknowledge();
    }

    @Test
    @DisplayName("consume delete event — should invoke deleteDevices and acknowledge")
    void givenAvroRecordDeleted_whenConsumed_thenDeleteProcessorInvoked_andAcked() {
        var device = DeviceUtilData.getDevice1();

        ConsumerRecord<String, DeviceAvro> record =
                new ConsumerRecord<>("devices-delete", 0, 0L, "key", device);

        List<ConsumerRecord<String, DeviceAvro>> list = List.of(record);

        BDDMockito.given(processor.deleteDevices(anyList()))
                .willReturn(Flux.empty());

        consumer.listenDevicesDelete(list, ack);

        BDDMockito.then(processor).should(times(1)).deleteDevices(List.of(device));
        BDDMockito.then(ack).should(times(1)).acknowledge();
    }
}
