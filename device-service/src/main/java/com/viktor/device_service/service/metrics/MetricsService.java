package com.viktor.device_service.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetricsService {

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> counters = new ConcurrentHashMap<>();

    public void increment(String name) {
        try {
            Counter counter = counters.computeIfAbsent(name, m ->
                    Counter.builder(m)
                            .description("Metric: " + m)
                            .register(meterRegistry)
            );
            counter.increment();
        } catch (Exception e) {
            log.error("Failed to increment metric {}", name, e);
        }
    }
}
