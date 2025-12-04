package com.viktor.failed_events_processor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.dto.ErrorDto;
import com.viktor.failed_events_processor.mapper.ErrorMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorSerializer {

    private final ObjectMapper objectMapper;
    private final ErrorMapper errorMapper;

    public String toJson(DltError error) {
        try {
            ErrorDto errorDto = errorMapper.toErrorDto(error);
            return objectMapper.writeValueAsString(errorDto);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize DltError to JSON: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Unable to serialize DltError", e);
        }
    }
}
