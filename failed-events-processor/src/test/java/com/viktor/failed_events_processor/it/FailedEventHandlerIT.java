package com.viktor.failed_events_processor.it;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.FailedEventHandler;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;

import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class FailedEventHandlerIT extends AbstractIntegrationTest {

    @Autowired
    private FailedEventHandler handler;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Test
    @DisplayName("Given single DltError when process messages then file saved in Minio")
    void givenMessage_whenProcess_thenSaved() throws Exception {

        DltError error = new DltError();
        error.setErrorSource("device");
        error.setTraceId("t123");
        error.setReceivedAt(System.currentTimeMillis());
        error.setReason("something");

        handler.processMessages(List.of(error))
                .block(Duration.ofSeconds(5));

        boolean found = false;

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : results) {
            Item item = result.get();
            if (item.objectName().contains("device/t123")) {
                found = true;
                break;
            }
        }

        assertTrue("Expected file path containing 'device/t123' to be stored in MinIO", found);
    }
}
