package com.viktor.failed_events_processor.service;

import com.viktor.avro.DltError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FileNamingServiceTest {

    private FileNamingService fileNamingService = new FileNamingService();

    @Test
    @DisplayName("Given DltError when generateFileName then returns valid path")
    void givenDltError_whenGenerateFileName_thenValid() {
        DltError error = new DltError();
        error.setErrorSource("device-service");
        error.setTraceId("abcd");
        error.setReceivedAt(1700000000L);

        String name = fileNamingService.generateFileName(error);

        assertTrue(name.contains("device-service/abcd/"));
        assertTrue(name.endsWith(".json"));
    }
}
