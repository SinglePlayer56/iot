package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEvent;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventCollectorService {

    Mono<Void> processEvents(List<DeviceEvent> events);
}
