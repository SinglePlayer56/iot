package com.viktor.mapper.device;

import com.viktor.avro.DeviceAvro;
import com.viktor.entity.DeviceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    @Mapping(target = "deviceId", source = "deviceId")
    @Mapping(target = "deviceType", source = "deviceType")
    @Mapping(target = "createdAt", source = "createdAt")
    @Mapping(target = "meta", source = "meta")
    DeviceEntity toDeviceEntity(DeviceAvro device);
}
