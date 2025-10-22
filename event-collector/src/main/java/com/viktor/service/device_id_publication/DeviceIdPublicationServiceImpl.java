package com.viktor.service.device_id_publication;

import com.viktor.entity.DeviceIdPublication;
import com.viktor.kafka.KafkaDeviceIdProducer;
import com.viktor.repository.DeviceIdPublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class DeviceIdPublicationServiceImpl implements DeviceIdPublicationService {

    private final DeviceIdPublicationRepository deviceIdPublicationRepository;
    private final DeduplicateCache deduplicateCache;
    private final KafkaDeviceIdProducer kafkaDeviceIdProducer;

    @Override
    public Mono<Void> processDeviceId(String deviceId) {
        if (deduplicateCache.contains(deviceId)) {
            return Mono.empty();
        }

        return kafkaDeviceIdProducer.send(deviceId)
                .then(deviceIdPublicationRepository.save(new DeviceIdPublication(deviceId, System.currentTimeMillis())))
                .doOnSuccess(_ -> deduplicateCache.add(deviceId))
                .then();
    }
}
