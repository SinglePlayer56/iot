package com.viktor.failed_events_processor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.dto.ErrorDto;
import com.viktor.failed_events_processor.mapper.ErrorMapper;
import com.viktor.failed_events_processor.service.serialize.ErrorSerializer;
import com.viktor.failed_events_processor.utils.DltErrorData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class ErrorSerializerTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ErrorMapper errorMapper;

    private ErrorSerializer serializer;

    @BeforeEach
    void setup() {
        serializer = new ErrorSerializer(objectMapper, errorMapper);
    }

    @Test
    @DisplayName("Given valid DltError when serialize then returns JSON")
    void givenError_whenSerialize_thenJson() throws Exception {
        DltError error = DltErrorData.getDltError();
        ErrorDto dto = errorMapper.toErrorDto(error);
        given(errorMapper.toErrorDto(error)).willReturn(dto);
        given(objectMapper.writeValueAsString(dto)).willReturn("{\"ok\":true}");

        String json = serializer.toJson(error);

        assertEquals("{\"ok\":true}", json);
    }

    @Test
    @DisplayName("Given ObjectMapper throws when serialize then IllegalArgumentException")
    void givenMapperThrows_whenSerialize_thenIllegalArgument() throws Exception {
        DltError error = DltErrorData.getDltError();
        ErrorDto dto = errorMapper.toErrorDto(error);
        given(errorMapper.toErrorDto(error)).willReturn(dto);
        given(objectMapper.writeValueAsString(dto))
                .willThrow(new JsonProcessingException("fail") {});

        assertThrows(IllegalArgumentException.class, () -> serializer.toJson(error));
    }
}
