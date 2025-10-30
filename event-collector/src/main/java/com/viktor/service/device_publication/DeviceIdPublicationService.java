package com.viktor.service.device_publication;

import com.viktor.avro.Device;
import reactor.core.publisher.Mono;

public interface DeviceIdPublicationService {

    Mono<Void> processDevice(Device device);
}
