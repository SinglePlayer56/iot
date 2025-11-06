package com.viktor.aspects;

import com.viktor.exception.ShardErrorException;
import com.viktor.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MetricsAspect {

    private final MetricsService metricsService;

    @Around("execution(* com.viktor.kafka.KafkaDeviceDltProducer.publishToDlt(..))")
    public Mono<Void> trackDltPublishMetrics(ProceedingJoinPoint pjp) throws Throwable {
        var result = (Mono<Void>) pjp.proceed();
        return result
                .doOnSubscribe(_ -> metricsService.incrementError());
    }

    @Around("execution(* com.viktor.dao.DeviceDao.save(..))")
    public Object trackDatabaseSaveMetrics(ProceedingJoinPoint pjp) throws Throwable {
        try {
            Object result = pjp.proceed();
            metricsService.incrementSuccess();
            return result;
        } catch (ShardErrorException ex) {
            metricsService.incrementErrorByShard(ex.getShardName());
            log.warn("🔴 Ошибка сохранения на шарде {}", ex.getShardName());
            throw ex;
        }
    }
}
