package com.viktor.service.event_collector;

import com.viktor.avro.DeviceEventAvro;
import reactor.core.publisher.Mono;

import java.util.List;

public interface EventCollectorService {

    Mono<Void> processEvents(List<DeviceEventAvro> events);
}
