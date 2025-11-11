package com.viktor.service.device;

import com.viktor.avro.Device;
import reactor.core.publisher.Mono;

public interface DeviceService {

    Mono<Void> saveDevice(Device device);
}
