package com.viktor.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    private static final String NAME = "device_collector";
    private static final String SUCCESS_PROCESS_MESSAGE = NAME + "_success_process_messages";
    private static final String SUCCESS_FAILED_MESSAGE = NAME + "_failed_process_messages";
    private static final String ERROR_BY_SHARD = NAME + "_error_by_shard_total";

    @PostConstruct
    public void init() {
        getCounter(SUCCESS_PROCESS_MESSAGE, "status", "success");
        getCounter(SUCCESS_FAILED_MESSAGE, "status", "error");
    }

    public void incrementSuccess() {
        getCounter(SUCCESS_PROCESS_MESSAGE, "status", "success").increment();
    }

    public void incrementError() {
        getCounter(SUCCESS_FAILED_MESSAGE, "status", "error").increment();
    }

    public void incrementErrorByShard(String shardName) {
        getCounter(ERROR_BY_SHARD, "shard", shardName).increment();
    }

    private Counter getCounter(String name, String tagKey, String tagValue) {
        String key = name + ":" + tagKey + "=" + tagValue;
        return counters.computeIfAbsent(key,
                _ -> Counter.builder(name)
                        .description("Metric for " + name)
                        .tag(tagKey, tagValue)
                        .register(meterRegistry));
    }
}
