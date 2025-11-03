package com.viktor.service.kafka;

import com.viktor.avro.Device;
import com.viktor.service.device.DeviceProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceConsumer {

    private final DeviceProcessor deviceProcessor;

    @KafkaListener(topics = "${spring.kafka.consumer.read-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<Device> devices, Acknowledgment ack) {
        deviceProcessor.processDevices(devices)
                .then(Mono.fromRunnable(() -> {
                    ack.acknowledge();
                    log.info("Successfully processed {} devices", devices.size());
                }))
                .subscribe();
    }
}
