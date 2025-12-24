package com.viktor.failed_events_processor.service.naming;

import com.viktor.avro.DltError;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileNamingService {

    private static final String DATE_PATTERN = "yyyy/MM/dd";

    @Value("${minio.bucket-name}")
    private String bucketName;

    @WithSpan("generate-file-name")
    public String generateFileName(DltError dltError) {
        String sourcePart = dltError.getErrorSource();
        String traceIdPart = dltError.getTraceId();
        long timestamp = dltError.getReceivedAt();

        String uuid = UUID.randomUUID().toString();

        String datePath = new SimpleDateFormat(DATE_PATTERN)
                .format(new Date(timestamp));

        return String.format(
                "%s/%s/%s/%s/%d_%s.json",
                bucketName,
                sourcePart,
                traceIdPart,
                datePath,
                timestamp,
                uuid
        );
    }
}
