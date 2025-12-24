package com.viktor.failed_events_processor.dto;

public record ErrorDto(
        String reason,
        String errorSource,
        String stackTrace,
        String traceId,
        long receivedAt
) {
}
