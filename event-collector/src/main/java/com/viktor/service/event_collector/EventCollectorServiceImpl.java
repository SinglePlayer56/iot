package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEvent;
import com.viktor.entity.DeviceEventByDevice;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_publication.DeviceIdPublicationService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
    @WithSpan("process-events")
    public Mono<Void> processEvents(List<DeviceEvent> events) {
        log.info("Processing events: {}", events.size());
        return Flux.fromIterable(events)
                .flatMap(this::processEvent)
                .then();
    }

    @WithSpan("process-event")
    private Mono<Void> processEvent(DeviceEvent deviceEvent) {
        return saveEvent(deviceEvent)
                .then(deviceIdPublicationService.processDevice(deviceEvent.getDevice()))
                .then();
    }

    @WithSpan("save-event")
    private Mono<DeviceEventByDevice> saveEvent(DeviceEvent deviceEvent) {
        DeviceEventByDevice event = DeviceEventByDevice.builder()
                .deviceId(deviceEvent.getDevice().getDeviceId())
                .eventId(deviceEvent.getEventId())
                .timestamp(deviceEvent.getTimestamp())
                .type(deviceEvent.getType())
                .payload(deviceEvent.getPayload())
                .build();
        log.info("Saving event: {}", event);
        return eventCollectorRepository.save(event);
    }
}
