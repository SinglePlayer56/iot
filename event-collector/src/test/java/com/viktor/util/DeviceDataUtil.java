package com.viktor.util;

import com.viktor.avro.DeviceAvro;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DeviceDataUtil {

    public static DeviceAvro getDevice1() {
        return new DeviceAvro(
                "dev-1",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static DeviceAvro getDevice2() {
        return new DeviceAvro(
                "dev-2",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static DeviceAvro getDevice3() {
        return new DeviceAvro(
                "dev-3",
                "type",
                System.currentTimeMillis(),
                "meta"
        );
    }

    public static DeviceAvro getUpdatedDevice1() {
        return new DeviceAvro(
                "dev-1",
                "typeUpdated",
                System.currentTimeMillis(),
                "meta"
        );
    }
}
