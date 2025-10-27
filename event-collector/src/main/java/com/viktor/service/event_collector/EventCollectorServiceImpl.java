package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEvent;
import com.viktor.entity.DeviceEventByDevice;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_id_publication.DeviceIdPublicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCollectorServiceImpl implements EventCollectorService {

    private final EventCollectorRepository eventCollectorRepository;
    private final DeviceIdPublicationService deviceIdPublicationService;

    @Override
    public Mono<Void> processEvents(List<DeviceEvent> events) {
        return Flux.fromIterable(events)
                .flatMap(this::saveEvent)
                .thenMany(Flux.fromIterable(events)
                        .map(DeviceEvent::getDeviceId)
                        .distinct()
                        .flatMap(deviceIdPublicationService::processDeviceId))
                .then();
    }

    private Mono<DeviceEventByDevice> saveEvent(DeviceEvent deviceEvent) {
        DeviceEventByDevice event = DeviceEventByDevice.builder()
                .deviceId(deviceEvent.getDeviceId())
                .eventId(deviceEvent.getEventId())
                .timestamp(deviceEvent.getTimestamp())
                .type(deviceEvent.getType())
                .payload(deviceEvent.getPayload())
                .build();
        return eventCollectorRepository.save(event);
    }
}
