package com.viktor.util;

import com.viktor.avro.Device;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DeviceDataUtil {

    public static Device getDevice1() {
        return new Device(
                "dev-1",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static Device getDevice2() {
        return new Device(
                "dev-2",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static Device getDevice3() {
        return new Device(
                "dev-3",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static Device getUpdatedDevice1() {
        return new Device(
                "dev-1",
                "typeUpdated",
                System.currentTimeMillis(),
                "meta"
        );
    }
}
