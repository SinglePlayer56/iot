package com.viktor.service.device_publication;

import com.viktor.avro.DeviceAvro;
import com.viktor.entity.DevicePublication;
import com.viktor.kafka.KafkaDeviceProducer;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceIdPublicationServiceImpl implements DeviceIdPublicationService {

    private final DevicePublicationRepository devicePublicationRepository;
    private final DeduplicateCache deduplicateCache;
    private final KafkaDeviceProducer kafkaDeviceProducer;

    @Override
    @WithSpan("process-device")
    public Mono<Void> processDevice(DeviceAvro device) {
        log.info("Processing device {}", device);
        if (deduplicateCache.contains(device)) {
            return Mono.empty();
        }

        var devicePublication = new DevicePublication(
                device.getDeviceId(),
                device.getDeviceType(),
                device.getMeta(),
                System.currentTimeMillis()
        );

        return kafkaDeviceProducer.send(device)
                .then(devicePublicationRepository.save(devicePublication))
                .doOnSuccess(_ -> deduplicateCache.add(device))
                .then();
    }
}
