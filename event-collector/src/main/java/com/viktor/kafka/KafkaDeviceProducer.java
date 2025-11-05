package com.viktor.kafka;

import com.viktor.avro.Device;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.producer.write-topic}")
    private final String writeTopic;

    @WithSpan("publishing-in-devices-topic")
    public Mono<SendResult<String, Object>> send(Device device) {
        log.info("Publishing device {} to devices topic", device);
        return Mono.fromFuture(() -> kafkaTemplate.send(writeTopic, device));
    }
}
