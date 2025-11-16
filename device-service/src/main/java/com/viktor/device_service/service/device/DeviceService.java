package com.viktor.device_service.service.device;

import com.viktor.device.dto.DeviceDto;

public interface DeviceService {

    void createDevice(DeviceDto deviceDto);

    void updateDevice(String deviceId, DeviceDto deviceDto);

    DeviceDto findById(String deviceId);

    void deleteById(String deviceId);
}
