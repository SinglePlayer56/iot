package com.viktor.service.device_id_publication;

import com.viktor.entity.DeviceIdPublication;
import com.viktor.kafka.KafkaDeviceIdProducer;
import com.viktor.repository.DeviceIdPublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class DeviceIdPublicationServiceImpl implements DeviceIdPublicationService {

    private final DeviceIdPublicationRepository deviceIdPublicationRepository;
    private final DeduplicateCache deduplicateCache;
    private final KafkaDeviceIdProducer kafkaDeviceIdProducer;

    @Override
    @Transactional
    public void processDeviceId(String deviceId) {
        var publishedDeviceId = new DeviceIdPublication(deviceId, System.currentTimeMillis());
        if (!deduplicateCache.contains(publishedDeviceId.getDeviceId())) {
            kafkaDeviceIdProducer.send(deviceId).whenComplete((_, ex) -> {
                if (Objects.isNull(ex)) {
                    deviceIdPublicationRepository.save(publishedDeviceId);
                    deduplicateCache.add(deviceId);
                }
            });
        }
    }
}
