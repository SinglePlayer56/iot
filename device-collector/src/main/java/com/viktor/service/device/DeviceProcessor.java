package com.viktor.service.device;

import com.viktor.avro.DeviceAvro;
import com.viktor.kafka.KafkaDeviceDltProducer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceProcessor {

    private final DeviceService deviceService;
    private final KafkaDeviceDltProducer kafkaDeviceDltProducer;

    @WithSpan("process-devices")
    public Flux<Void> processDevices(List<DeviceAvro> devices) {
        return Flux.fromIterable(devices)
                .groupBy(DeviceAvro::getDeviceId)
                .flatMap(group -> group.concatMap(this::processDevice), 8);
    }

    @WithSpan("process-device")
    private Mono<Void> processDevice(DeviceAvro device) {
        log.info("Processing device {}", device.getDeviceId());
        return deviceService.saveDevice(device)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(ex -> ex instanceof Exception)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure())
                )
                .onErrorResume(ex -> kafkaDeviceDltProducer.publishToDlt(device, ex));
    }
}
