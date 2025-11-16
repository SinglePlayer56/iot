package com.viktor.device_service.rest;

import com.viktor.device.api.DeviceApiClient;
import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.service.device.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeviceRestControllerV1 implements DeviceApiClient {

    private final DeviceService deviceService;

    @Override
    public ResponseEntity<Void> createDevice(DeviceDto deviceDto) {
        deviceService.createDevice(deviceDto);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<Void> deleteDeviceById(String deviceId) {
        deviceService.deleteById(deviceId);
        return ResponseEntity.accepted().build();
    }

    @Override
    public ResponseEntity<DeviceDto> findById(String deviceId) {
        var deviceDto = deviceService.findById(deviceId);
        return ResponseEntity.ok(deviceDto);
    }

    @Override
    public ResponseEntity<Void> updateDevice(String deviceId, DeviceDto deviceDto) {
        deviceService.updateDevice(deviceId, deviceDto);
        return ResponseEntity.accepted().build();
    }
}
