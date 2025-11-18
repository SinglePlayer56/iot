package com.viktor.device_service.mapper;

import com.viktor.avro.DeviceAvro;
import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.entity.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "meta", source = "meta")
    Device toDevice(DeviceDto device);

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "meta", source = "meta")
    DeviceDto toDeviceDto(Device device);

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "meta", source = "meta")
    DeviceAvro toDeviceAvro(DeviceDto device);
}
