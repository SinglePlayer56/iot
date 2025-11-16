package com.viktor.device_service.service.device;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.repository.DeviceRepository;
import com.viktor.device_service.service.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;

    @Override
    public void createDevice(DeviceDto deviceDto) {

    }

    @Override
    public void updateDevice(String deviceId, DeviceDto deviceDto) {

    }

    @Override
    public DeviceDto findById(String deviceId) {
        return null;
    }

    @Override
    public void deleteById(String deviceId) {

    }
}
