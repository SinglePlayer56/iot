package com.viktor.device_service.exception;

public class AlreadyExistsException extends RuntimeException {
    public AlreadyExistsException(String... args) {
        super(String.join(" ", args));
    }
}
