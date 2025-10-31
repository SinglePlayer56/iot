package com.viktor.service.kafka;

import com.viktor.avro.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KafkaDeviceConsumer {

    @KafkaListener(topics = "${spring.kafka.consumer.read-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<Device> devices, Acknowledgment ack) {

    }
}
