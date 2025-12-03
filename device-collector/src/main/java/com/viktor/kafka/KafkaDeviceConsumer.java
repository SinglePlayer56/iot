package com.viktor.kafka;

import com.viktor.avro.DeviceAvro;
import com.viktor.enums.DeviceOperation;
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

    @WithSpan("consume-devices-save-update")
    @KafkaListener(topics = "${spring.kafka.consumer.save-update-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenDevices(List<ConsumerRecord<String, DeviceAvro>> records, Acknowledgment ack) {
        handleEvent(records, ack, DeviceOperation.SAVE_OR_UPDATE);
    }

    @WithSpan("consume-devices-delete")
    @KafkaListener(topics = "${spring.kafka.consumer.delete-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listenDevicesDelete(List<ConsumerRecord<String, DeviceAvro>> records, Acknowledgment ack) {
        handleEvent(records, ack, DeviceOperation.DELETE);
    }

    private void handleEvent(
            List<ConsumerRecord<String, DeviceAvro>> records,
            Acknowledgment ack,
            DeviceOperation operation
    ) {
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
            processOperation(operation, devices, ack)
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
            var span = tracer
                    .spanBuilder("consume-devices-" + operation)
                    .startSpan();

            log.info("Consuming {} devices ({}), trace_id={}", devices.size(), operation, traceHeader);

            processOperation(operation, devices, ack)
                    .doFinally(_ -> span.end())
                    .subscribe();
        }
    }

    private Mono<Void> processOperation(DeviceOperation operation, List<DeviceAvro> devices, Acknowledgment ack) {
        return switch (operation) {
            case DeviceOperation.SAVE_OR_UPDATE -> processSaveOrUpdate(devices, ack);
            case DeviceOperation.DELETE -> processDelete(devices, ack);
        };
    }

    private Mono<Void> processDelete(List<DeviceAvro> devices, Acknowledgment ack) {
        return deviceProcessor.deleteDevices(devices)
                .then(Mono.fromRunnable(() -> {
                    ack.acknowledge();
                    log.info("Successfully delete {} devices", devices.size());
                }));
    }


    private Mono<Void> processSaveOrUpdate(List<DeviceAvro> devices, Acknowledgment ack) {
        return deviceProcessor.processDevices(devices)
                .then(Mono.fromRunnable(() -> {
                    ack.acknowledge();
                    log.info("Successfully processed {} devices", devices.size());
                }));
    }
}
