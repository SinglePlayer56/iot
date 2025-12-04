package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEventAvro;
import com.viktor.entity.DeviceEventByDevice;
import com.viktor.kafka.KafkaDltProducer;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_publication.DeviceIdPublicationService;
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
public class EventCollectorServiceImpl implements EventCollectorService {

    private final EventCollectorRepository eventCollectorRepository;
    private final DeviceIdPublicationService deviceIdPublicationService;
    private final KafkaDltProducer kafkaDltProducer;

    @Override
    @WithSpan("process-events")
    public Mono<Void> processEvents(List<DeviceEventAvro> events) {
        log.info("Processing events: {}", events.size());
        return Flux.fromIterable(events)
                .flatMap(this::processEvent)
                .then();
    }

    @WithSpan("process-event")
    private Mono<Void> processEvent(DeviceEventAvro deviceEvent) {
        return Mono.defer(() ->
                        saveEvent(deviceEvent)
                                .then(deviceIdPublicationService.processDevice(deviceEvent.getDevice()))
                )
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(8))
                        .filter(ex -> ex instanceof Exception)
                        .onRetryExhaustedThrow((_, signal) -> signal.failure())
                )
                .onErrorResume(kafkaDltProducer::publishToDlt);
    }

    @WithSpan("save-event")
    private Mono<DeviceEventByDevice> saveEvent(DeviceEventAvro deviceEvent) {
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
