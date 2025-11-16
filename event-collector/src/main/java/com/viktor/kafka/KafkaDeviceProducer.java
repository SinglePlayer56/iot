package com.viktor.kafka;

import com.viktor.avro.DeviceAvro;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${spring.kafka.producer.write-topic}")
    private final String writeTopic;

    @WithSpan("publishing-in-devices-topic")
    public Mono<SendResult<String, Object>> send(DeviceAvro device) {
        return Mono.fromFuture(() -> {
            ProducerRecord<String, Object> record = new ProducerRecord<>(writeTopic, device);
            var traceId = Span.current().getSpanContext().getTraceId();
            log.info("Publishing device {} to devices topic", device);
            if (Objects.nonNull(traceId)) {
                record.headers().add("trace_id", traceId.getBytes(StandardCharsets.UTF_8));
            }
            return kafkaTemplate.send(record);
        });
    }
}
