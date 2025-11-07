package com.viktor.kafka;

import com.viktor.avro.Device;
import com.viktor.service.device.DeviceProcessor;
import com.viktor.utils.DeviceUtilData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
    void givenAvroRecord_whenConsumed_thenProcessorInvoked_andAcked() {
        var device = DeviceUtilData.getDevice1();

        ConsumerRecord<String, Device> record =
                new ConsumerRecord<>("devices", 0, 0L, "key", device);

        List<ConsumerRecord<String, Device>> list = List.of(record);

        BDDMockito.given(processor.processDevices(anyList()))
                .willReturn(Flux.empty());

        consumer.listen(list, ack);

        BDDMockito.then(processor).should(times(1)).processDevices(List.of(device));
        BDDMockito.then(ack).should(times(1)).acknowledge();
    }
}
