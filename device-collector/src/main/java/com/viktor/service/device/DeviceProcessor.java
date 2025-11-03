package com.viktor.service.device;

import com.viktor.avro.Device;
import com.viktor.service.kafka.KafkaDeviceDltProducer;
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

    public Flux<Void> processDevices(List<Device> devices) {
        return Flux.fromIterable(devices)
                .groupBy(Device::getDeviceId)
                .flatMap(group -> group.concatMap(this::processDevice), 8);
    }

    private Mono<Void> processDevice(Device device) {
        return deviceService.saveDevice(device)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(ex -> ex instanceof Exception)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure())
                )
                .onErrorResume(ex -> kafkaDeviceDltProducer.publishToDlt(device, ex));
    }
}
