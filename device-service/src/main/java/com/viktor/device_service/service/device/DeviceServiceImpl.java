package com.viktor.device_service.service.device;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.exception.AlreadyExistsException;
import com.viktor.device_service.exception.NotFoundException;
import com.viktor.device_service.kafka.KafkaDeviceProducer;
import com.viktor.device_service.mapper.DeviceMapper;
import com.viktor.device_service.repository.DeviceRepository;
import com.viktor.device_service.service.redis.RedisCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final KafkaDeviceProducer kafkaDeviceProducer;
    private final RedisCacheService redisCacheService;
    private final DeviceMapper deviceMapper;

    @Override
    public void createDevice(DeviceDto deviceDto) {
        var cachedDeviceDto = redisCacheService.get(deviceDto.getDeviceId());
        if (Objects.nonNull(cachedDeviceDto)) {
            throw new AlreadyExistsException("Device with id %s already exists".formatted(cachedDeviceDto.getDeviceId()));
        }
        deviceRepository.findById(deviceDto.getDeviceId())
                .ifPresent(device -> {
                    throw new AlreadyExistsException("Device with id %s already exists".formatted(device.getDeviceId()));
                });

        kafkaDeviceProducer.sendToCreateUpdateTopic(deviceDto);
    }

    @Override
    public void updateDevice(String deviceId, DeviceDto deviceDto) {
        var cachedDeviceDto = redisCacheService.get(deviceId);
        if (Objects.isNull(cachedDeviceDto)) {
            deviceRepository.findById(deviceDto.getDeviceId())
                    .orElseThrow(() -> new NotFoundException("Device with id %s not found".formatted(deviceDto.getDeviceId())));
        }

        kafkaDeviceProducer.sendToCreateUpdateTopic(deviceDto);
        redisCacheService.evict(deviceId);
    }

    @Override
    public DeviceDto findById(String deviceId) {
        var deviceDto = redisCacheService.get(deviceId);
        if (Objects.isNull(deviceDto)) {
            var device = deviceRepository.findByDeviceId(deviceId)
                    .orElseThrow(() -> new NotFoundException("Device not found"));

            deviceDto = deviceMapper.toDeviceDto(device);
            redisCacheService.set(deviceId, deviceDto);
        }

        return deviceDto;
    }

    @Override
    public void deleteById(String deviceId) {
        var deviceDto = redisCacheService.get(deviceId);
        if (Objects.isNull(deviceDto)) {
            var deviceEntity = deviceRepository.findById(deviceId)
                    .orElseThrow(() -> new NotFoundException("Device with id %s not found".formatted(deviceId)));
            deviceDto = deviceMapper.toDeviceDto(deviceEntity);
        } else {
            redisCacheService.evict(deviceId);
        }

        kafkaDeviceProducer.sendToDeleteTopic(deviceDto);
    }
}
