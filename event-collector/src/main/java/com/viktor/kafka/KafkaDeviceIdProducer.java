package com.viktor.kafka;

import com.viktor.avro.DeviceId;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
public class KafkaDeviceIdProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public CompletableFuture<?> send(String deviceId) {
        DeviceId message = new DeviceId(deviceId);
        return kafkaTemplate.send("device-id-topic", message);
    }
}
