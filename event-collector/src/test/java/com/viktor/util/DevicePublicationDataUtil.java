package com.viktor.util;

import com.viktor.entity.DevicePublication;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DevicePublicationDataUtil {

    public static DevicePublication getDevicePublication1() {
        return new DevicePublication(
                "dev-1",
                "type",
                "meta",
                System.currentTimeMillis()
        );
    }

    public static DevicePublication getDevicePublication2() {
        return new DevicePublication(
                "dev-2",
                "type",
                "meta",
                System.currentTimeMillis()
        );
    }

    public static DevicePublication getDevicePublication3() {
        return new DevicePublication(
                "dev-3",
                "type",
                "meta",
                System.currentTimeMillis()
        );
    }

    public static DevicePublication getDevicePublication1Updated() {
        return new DevicePublication(
                "dev-1",
                "typeUpdated",
                "meta",
                System.currentTimeMillis()
        );
    }
}
