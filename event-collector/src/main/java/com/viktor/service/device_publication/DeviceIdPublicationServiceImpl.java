package com.viktor.service.device_publication;

import com.viktor.avro.Device;
import com.viktor.entity.DevicePublication;
import com.viktor.kafka.KafkaDeviceProducer;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeviceIdPublicationServiceImpl implements DeviceIdPublicationService {

    private final DevicePublicationRepository devicePublicationRepository;
    private final DeduplicateCache deduplicateCache;
    private final KafkaDeviceProducer kafkaDeviceProducer;

    @Override
    public Mono<Void> processDevice(Device device) {
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
