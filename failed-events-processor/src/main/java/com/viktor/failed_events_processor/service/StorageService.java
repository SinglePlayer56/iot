package com.viktor.failed_events_processor.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name}")
    private String bucketName;

    public Mono<Void> save(String json, String fileName) {
        return Mono.<Void>fromCallable(() -> {
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);

                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(fileName)
                                    .stream(new ByteArrayInputStream(bytes), bytes.length, -1)
                                    .contentType("application/json; charset=UTF-8")
                                    .build()
                    );
                    log.info("Saved to minio: {}", fileName);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic());
    }
}
