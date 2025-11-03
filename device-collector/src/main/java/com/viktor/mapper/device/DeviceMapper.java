package com.viktor.mapper.device;

import com.viktor.avro.Device;
import com.viktor.entity.DeviceEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DeviceMapper {

    DeviceEntity toDeviceEntity(Device device);

    Device toDevice(DeviceEntity deviceEntity);
}
