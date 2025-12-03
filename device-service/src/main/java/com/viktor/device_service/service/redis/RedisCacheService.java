package com.viktor.device_service.service.redis;

import com.viktor.device.dto.DeviceDto;

public interface RedisCacheService {

    DeviceDto get(String deviceId);

    void set(String deviceId, DeviceDto deviceDto);

    void evict(String deviceId);
}
