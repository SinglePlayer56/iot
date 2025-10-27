package com.viktor.kafka;

import com.viktor.avro.DeviceId;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KafkaDeviceIdProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.producer.write-topic}")
    private final String writeTopic;

    public Mono<SendResult<String, Object>> send(String deviceId) {
        DeviceId message = new DeviceId(deviceId);
        return Mono.fromFuture(() -> kafkaTemplate.send(writeTopic, message));
    }
}
