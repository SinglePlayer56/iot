package com.viktor.util;

import com.viktor.entity.DeviceEventByDevice;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EventDataUtil {

    public static DeviceEventByDevice getDeviceEventByDevice1() {
        return DeviceEventByDevice.builder()
                .deviceId(DeviceDataUtil.getDevice1().getDeviceId())
                .eventId("event-123")
                .timestamp(System.currentTimeMillis())
                .type("ALERT")
                .payload("payload-data")
                .build();
    }
}
