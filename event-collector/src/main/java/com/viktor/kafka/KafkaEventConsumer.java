package com.viktor.kafka;

import com.viktor.avro.DeviceEventAvro;
import com.viktor.service.event_collector.EventCollectorService;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final EventCollectorService eventCollectorService;

    @WithSpan("consume-events")
    @KafkaListener(topics = "${spring.kafka.consumer.read-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<DeviceEventAvro> deviceEvents, Acknowledgment ack) {
        log.info("Received events: {}", deviceEvents.size());
        eventCollectorService.processEvents(deviceEvents)
                .doOnSuccess(_ -> ack.acknowledge())
                .subscribe();

    }
}
