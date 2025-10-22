package com.viktor.service.device_id_publication;

import reactor.core.publisher.Mono;

public interface DeviceIdPublicationService {

    Mono<Void> processDeviceId(String deviceId);
}
