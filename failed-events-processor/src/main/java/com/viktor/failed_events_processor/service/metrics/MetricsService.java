package com.viktor.failed_events_processor.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;

    @Value("${spring.application.name}")
    private String appName;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private static final String SUCCESS_SAVE_MINIO_METRIC = "_success_save_minio";
    private static final String ERROR_SAVE_MINIO_METRIC = "_error_save_minio";
    private static final String SUCCESS_JSON_SERIALIZE_METRIC = "_success_json_serialize";
    private static final String ERROR_JSON_SERIALIZE_METRIC = "_error_json_serialize";
    private static final String SUCCESS_PROCESS_MESSAGE_METRIC = "_success_process_message";

    @PostConstruct
    public void init() {
        registerCounter(SUCCESS_SAVE_MINIO_METRIC);
        registerCounter(ERROR_SAVE_MINIO_METRIC);
        registerCounter(SUCCESS_JSON_SERIALIZE_METRIC);
        registerCounter(ERROR_JSON_SERIALIZE_METRIC);
        registerCounter(SUCCESS_PROCESS_MESSAGE_METRIC);
    }

    public void incrementSuccessSaveMinio() {
        var key = appName + SUCCESS_SAVE_MINIO_METRIC;
        counters.get(key).increment();
    }

    public void incrementErrorSaveMinio() {
        var key = appName + ERROR_SAVE_MINIO_METRIC;
        counters.get(key).increment();
    }

    public void incrementSuccessJsonSerialize() {
        var key = appName + SUCCESS_JSON_SERIALIZE_METRIC;
        counters.get(key).increment();
    }

    public void incrementErrorJsonSerialize() {
        var key = appName + ERROR_JSON_SERIALIZE_METRIC;
        counters.get(key).increment();
    }

    public void incrementSuccessProcessMessage(int count) {
        var key = appName + SUCCESS_PROCESS_MESSAGE_METRIC;
        counters.get(key).increment(count);
    }

    private void registerCounter(String metricName) {
        counters.computeIfAbsent(appName + metricName, m ->
                Counter.builder(m)
                        .description("Metric: " + m)
                        .register(meterRegistry)
        );
    }
}
