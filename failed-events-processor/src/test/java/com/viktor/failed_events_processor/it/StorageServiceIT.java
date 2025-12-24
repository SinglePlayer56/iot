package com.viktor.failed_events_processor.it;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.serialize.ErrorSerializer;
import com.viktor.failed_events_processor.service.naming.FileNamingService;
import com.viktor.failed_events_processor.service.minio.StorageService;
import com.viktor.failed_events_processor.utils.DltErrorData;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class StorageServiceIT extends AbstractIntegrationTest {

    @Autowired
    private StorageService storageService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private FileNamingService fileNamingService;

    @Autowired
    private ErrorSerializer errorSerializer;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Test
    @DisplayName("Given DltError data when saved to Minio then object exists")
    void givenDltError_whenSaved_thenObjectExists() throws Exception {
        DltError dltError = DltErrorData.getDltError();
        String fileName = fileNamingService.generateFileName(dltError);
        String json = errorSerializer.toJson(dltError);

        storageService.save(json, fileName)
                .block(Duration.ofSeconds(3));

        InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(fileName)
                        .build());
        assertNotNull(is);
        String data = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(json, data);
    }
}
