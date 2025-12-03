package com.viktor.device_service.exception;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String... args) {
        super(String.join(" ", args));
    }
}
