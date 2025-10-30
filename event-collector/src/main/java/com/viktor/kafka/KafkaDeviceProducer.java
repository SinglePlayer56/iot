package com.viktor.kafka;

import com.viktor.avro.Device;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class KafkaDeviceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.producer.write-topic}")
    private final String writeTopic;

    public Mono<SendResult<String, Object>> send(Device device) {
        return Mono.fromFuture(() -> kafkaTemplate.send(writeTopic, device));
    }
}
