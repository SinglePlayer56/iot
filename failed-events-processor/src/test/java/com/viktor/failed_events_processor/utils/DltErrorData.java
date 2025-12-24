package com.viktor.failed_events_processor.utils;

import com.viktor.avro.DltError;

import java.time.Instant;

public class DltErrorData {

    public static DltError getDltError() {
        var error = new DltError();
        error.setReason("NullPointerException occurred while processing event");
        error.setErrorSource("device-collector");
        error.setStackTrace("java.lang.NullPointerException: Cannot invoke \\\"Object.toString()\\\" because \\\"obj\\\" is null\\n\\tat com.viktor.device.Service.process(Service.java:42)\\n\\tat com.viktor.device.Controller.handle(Controller.java:21)");
        error.setTraceId("b87ff8f7-6b93-4eea-9e68-ced75f252467");
        error.setReceivedAt(Instant.now().toEpochMilli());
        return error;
    }
}
