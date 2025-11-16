package com.viktor.kafka;

import com.viktor.avro.DeviceAvro;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceDltProducer {

    private final KafkaTemplate<Object, Object> kafkaTemplate;

    @Value("${spring.kafka.consumer.read-topic}")
    private String topicName;

    @WithSpan("publish-dlt")
    public Mono<Void> publishToDlt(DeviceAvro device, Throwable ex) {
        return Mono.fromFuture(() -> {
                    log.warn("Sent device {} to DLT due to {}", device.getDeviceId(), ex.getClass().getSimpleName());
                    return kafkaTemplate.send(topicName + ".DLT", device);
                })
                .then();
    }
}
