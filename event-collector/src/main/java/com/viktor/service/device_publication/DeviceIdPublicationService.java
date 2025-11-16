package com.viktor.service.device_publication;

import com.viktor.avro.DeviceAvro;
import reactor.core.publisher.Mono;

public interface DeviceIdPublicationService {

    Mono<Void> processDevice(DeviceAvro device);
}
