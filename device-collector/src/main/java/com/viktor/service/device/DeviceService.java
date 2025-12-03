package com.viktor.service.device;

import com.viktor.avro.DeviceAvro;
import reactor.core.publisher.Mono;

public interface DeviceService {

    Mono<Void> saveDevice(DeviceAvro device);

    Mono<Void> deleteDevice(String deviceId);
}
