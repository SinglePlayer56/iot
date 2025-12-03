package com.viktor.utils;

import com.viktor.avro.DeviceAvro;

public class DeviceUtilData {

    public static DeviceAvro getDevice1() {
        return DeviceAvro.newBuilder()
                .setDeviceId("dev-1")
                .setDeviceType("typeA")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{}")
                .build();
    }

    public static DeviceAvro getDevice2() {
        return DeviceAvro.newBuilder()
                .setDeviceId("dev-2")
                .setDeviceType("typeA")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{}")
                .build();
    }
}
