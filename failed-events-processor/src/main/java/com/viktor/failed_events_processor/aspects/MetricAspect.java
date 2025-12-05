package com.viktor.failed_events_processor.aspects;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
@Aspect
public class MetricAspect {

    private final MetricsService metricsService;

    @Around("execution(* com.viktor.failed_events_processor.service.minio.StorageService.save(..))")
    public Mono<Void> trackSaveMinioMetric(ProceedingJoinPoint pjp) throws Throwable {
        Mono<Void> result = (Mono<Void>) pjp.proceed();

        return result
                .doOnSuccess(_ -> metricsService.incrementSuccessSaveMinio())
                .doOnError(_ -> metricsService.incrementErrorSaveMinio());
    }

    @Around("execution(* com.viktor.failed_events_processor.service.serialize.ErrorSerializer.toJson(..))")
    public String trackToJsonSerializeMetric(ProceedingJoinPoint pjp) throws Throwable {
        try {
            String result = (String) pjp.proceed();
            metricsService.incrementSuccessJsonSerialize();
            return result;
        } catch (Exception e) {
            metricsService.incrementErrorJsonSerialize();
            throw e;
        }
    }

    @Around("execution(* com.viktor.failed_events_processor.service.handler.FailedEventHandler.processMessages(..))")
    public Mono<Void> trackProcessMessagesMetric(ProceedingJoinPoint pjp) throws Throwable {
        Object[] args = pjp.getArgs();

        List<DltError> messages = (List<DltError>) args[0];

        int totalMessages = messages.size();

        Mono<Void> result = (Mono<Void>) pjp.proceed();

        return result
                .doOnSuccess(_ -> {
                    metricsService.incrementSuccessProcessMessage(totalMessages);
                });
    }
}
