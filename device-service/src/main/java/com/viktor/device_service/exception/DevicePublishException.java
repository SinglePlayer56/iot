package com.viktor.device_service.exception;

public class DevicePublishException extends RuntimeException {
    public DevicePublishException(String... args) {
        super(String.join(" ", args));
    }
}
