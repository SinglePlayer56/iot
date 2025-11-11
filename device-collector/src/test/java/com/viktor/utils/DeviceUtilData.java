package com.viktor.utils;

import com.viktor.avro.Device;

public class DeviceUtilData {

    public static Device getDevice1() {
        return Device.newBuilder()
                .setDeviceId("dev-1")
                .setDeviceType("typeA")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{}")
                .build();
    }

    public static Device getDevice2() {
        return Device.newBuilder()
                .setDeviceId("dev-2")
                .setDeviceType("typeA")
                .setCreatedAt(System.currentTimeMillis())
                .setMeta("{}")
                .build();
    }
}
