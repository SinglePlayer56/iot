package com.viktor.failed_events_processor.service.handler;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.minio.StorageService;
import com.viktor.failed_events_processor.service.naming.FileNamingService;
import com.viktor.failed_events_processor.service.serialize.ErrorSerializer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FailedEventHandler {

    private final FileNamingService fileNamingService;
    private final ErrorSerializer errorSerializer;
    private final StorageService storageService;

    @WithSpan("process-messages")
    public Mono<Void> processMessages(List<DltError> dltMessages) {
        return Flux.fromIterable(dltMessages)
                .flatMap(this::processMessage, 8)
                .then();
    }

    @WithSpan("process-message")
    public Mono<Void> processMessage(DltError dltError) {
        var fileName = fileNamingService.generateFileName(dltError);
        var errorJson = errorSerializer.toJson(dltError);

        return storageService.save(errorJson, fileName)
                .retryWhen(
                        Retry.backoff(3, Duration.ofSeconds(1))
                                .maxBackoff(Duration.ofSeconds(8))
                                .transientErrors(true)
                                .doBeforeRetry(rs -> log.warn(
                                        "Retrying DLT message {} attempt {} due to: ",
                                        dltError.getTraceId(),
                                        rs.totalRetries() + 1,
                                        rs.failure()))
                );
    }
}
