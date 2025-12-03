package com.viktor.device_service.aspect;

import com.viktor.device_service.annotations.CacheMetric;
import com.viktor.device_service.annotations.CountMetric;
import com.viktor.device_service.service.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsAspect {

    private final MetricsService metricsService;

    @Around("@annotation(annotation)")
    public Object cacheMetrics(ProceedingJoinPoint joinPoint, CacheMetric annotation) throws Throwable {
        Object result = joinPoint.proceed();

        if (Objects.nonNull(result)) {
            metricsService.increment(annotation.hit());
        } else {
            metricsService.increment(annotation.miss());
        }

        return result;
    }

    @Around("@annotation(annotation)")
    public Object countMetrics(ProceedingJoinPoint joinPoint, CountMetric annotation) throws Throwable {
        try {
            Object result = joinPoint.proceed();
            metricsService.increment(annotation.success());
            return result;
        } catch (Exception e) {
            metricsService.increment(annotation.error());
            throw e;
        }
    }
}
