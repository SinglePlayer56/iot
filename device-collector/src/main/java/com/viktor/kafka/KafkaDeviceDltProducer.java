package com.viktor.kafka;

import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DltError;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceDltProducer {

    private final KafkaTemplate<Object, DltError> kafkaTemplate;

    @Value("${spring.kafka.dlt-topic}")
    private String dltTopicName;

    @Value("${spring.application.name}")
    private String appName;

    @WithSpan("publish-dlt")
    public Mono<Void> publishToDlt(DeviceAvro device, Throwable ex) {
        return Mono.fromFuture(() -> {
                    log.warn("Sent device {} to DLT due to {}", device.getDeviceId(), ex.getClass().getSimpleName());
                    var errorMessage = buildErrorMessage(ex);
                    return kafkaTemplate.send(dltTopicName, errorMessage);
                })
                .then();
    }

    private DltError buildErrorMessage(Throwable ex) {
        DltError dltMessage = new DltError();
        var traceId = Span.current().getSpanContext().getTraceId();
        dltMessage.setTraceId(traceId);
        dltMessage.setReceivedAt(System.currentTimeMillis());
        dltMessage.setErrorSource(appName);
        dltMessage.setReason(ex.getMessage());
        dltMessage.setStackTrace(Arrays.toString(ex.getStackTrace()));
        return dltMessage;
    }
}
