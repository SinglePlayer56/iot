package com.viktor.kafka;

import com.viktor.avro.Device;
import com.viktor.service.device.DeviceProcessor;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceConsumer {

    private final DeviceProcessor deviceProcessor;
    private final Tracer tracer = GlobalOpenTelemetry.getTracer("device-collector");

    @WithSpan("consume-devices")
    @KafkaListener(topics = "${spring.kafka.consumer.read-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<ConsumerRecord<String, Device>> records, Acknowledgment ack) {
        var devices = records.stream()
                .map(ConsumerRecord::value)
                .toList();

        var traceHeader = records.stream()
                .map(r -> r.headers().lastHeader("trace_id"))
                .filter(Objects::nonNull)
                .findFirst()
                .map(header -> new String(header.value(), StandardCharsets.UTF_8))
                .orElse(null);

        if (traceHeader == null) {
            log.info("Processing {} devices (no trace context)", devices.size());
            process(devices, ack)
                    .subscribe();
            return;
        }

        var spanContext = SpanContext.createFromRemoteParent(
                traceHeader,
                "0000000000000001",
                TraceFlags.getSampled(),
                TraceState.getDefault()
        );

        var parentContext = Context.root().with(Span.wrap(spanContext));

        try (var ignored = parentContext.makeCurrent()) {
            var span = tracer.spanBuilder("consume-devices").startSpan();
            log.info("Consuming {} devices with trace_id={}", devices.size(), traceHeader);

            process(devices, ack)
                    .doFinally(_ -> span.end())
                    .subscribe();
        }
    }


    private Mono<Void> process(List<Device> devices, Acknowledgment ack) {
        return deviceProcessor.processDevices(devices)
                .then(Mono.fromRunnable(() -> {
                    ack.acknowledge();
                    log.info("Successfully processed {} devices", devices.size());
                }));
    }
}
