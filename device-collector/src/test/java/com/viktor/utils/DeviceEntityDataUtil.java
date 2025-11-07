package com.viktor.utils;


import com.viktor.entity.DeviceEntity;

public class DeviceEntityDataUtil {

    public static DeviceEntity getDeviceEntity() {
        return DeviceEntity.builder()
                .deviceId("dev-1")
                .deviceType("sensor")
                .createdAt(System.currentTimeMillis())
                .meta("{}")
                .build();
    }
}
