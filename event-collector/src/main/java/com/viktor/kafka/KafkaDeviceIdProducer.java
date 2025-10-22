package com.viktor.kafka;

import com.viktor.avro.DeviceId;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KafkaDeviceIdProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public Mono<SendResult<String, Object>> send(String deviceId) {
        DeviceId message = new DeviceId(deviceId);
        return Mono.fromFuture(() -> kafkaTemplate.send("device-id-topic", message));
    }
}
