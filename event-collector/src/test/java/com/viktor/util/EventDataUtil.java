package com.viktor.util;

import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DeviceEventAvro;
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

    public static DeviceEventAvro getDeviceEventAvro() {
        DeviceAvro deviceAvro = new DeviceAvro();
        deviceAvro.setDeviceId("123");
        deviceAvro.setDeviceType("deviceType");
        deviceAvro.setMeta("meta");
        deviceAvro.setCreatedAt(System.currentTimeMillis());

        DeviceEventAvro deviceEventAvro = new DeviceEventAvro();
        deviceEventAvro.setEventId("123");
        deviceEventAvro.setDevice(deviceAvro);
        deviceEventAvro.setPayload("payload");
        deviceEventAvro.setType("type");
        deviceEventAvro.setTimestamp(System.currentTimeMillis());

        return deviceEventAvro;
    }
}
