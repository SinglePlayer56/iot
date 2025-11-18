package com.viktor.device_service.service.device;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.exception.AlreadyExistsException;
import com.viktor.device_service.exception.NotFoundException;
import com.viktor.device_service.kafka.KafkaDeviceProducer;
import com.viktor.device_service.mapper.DeviceMapper;
import com.viktor.device_service.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceRepository deviceRepository;
    private final KafkaDeviceProducer kafkaDeviceProducer;
    private final DeviceMapper deviceMapper;

    @Override
    public void createDevice(DeviceDto deviceDto) {
        deviceRepository.findById(deviceDto.getDeviceId())
                .ifPresent(device -> {
                    throw new AlreadyExistsException("Device with id %s already exists".formatted(device.getDeviceId()));
                });

        kafkaDeviceProducer.sendToCreateUpdateTopic(deviceDto);
    }

    @Override
    public void updateDevice(String deviceId, DeviceDto deviceDto) {
        deviceRepository.findById(deviceDto.getDeviceId())
                .orElseThrow(() -> new NotFoundException("Device with id %s not found".formatted(deviceDto.getDeviceId())));

        kafkaDeviceProducer.sendToCreateUpdateTopic(deviceDto);
    }

    @Override
    public DeviceDto findById(String deviceId) {
        var device = deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new NotFoundException("Device not found"));
        return deviceMapper.toDeviceDto(device);
    }

    @Override
    public void deleteById(String deviceId) {
        var deviceEntity = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new NotFoundException("Device with id %s not found".formatted(deviceId)));

        var deviceDto = deviceMapper.toDeviceDto(deviceEntity);
        kafkaDeviceProducer.sendToDeleteTopic(deviceDto);
    }
}
