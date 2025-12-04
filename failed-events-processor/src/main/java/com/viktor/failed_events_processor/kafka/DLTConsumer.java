package com.viktor.failed_events_processor.kafka;

import com.viktor.avro.DltError;
import com.viktor.failed_events_processor.service.FailedEventHandler;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DLTConsumer {

    private final FailedEventHandler failedEventHandler;

    @WithSpan("consume-dlt-topic")
    @KafkaListener(topics = "${spring.kafka.dlt-topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void listen(List<DltError> dltMessages, Acknowledgment ack) {
        log.info("Received messages: {}", dltMessages.size());
        failedEventHandler.processMessages(dltMessages)
                .doOnSuccess(_ -> ack.acknowledge())
                .doOnError(e -> log.error("Failed to process DLT messages", e))
                .subscribe();

    }
}
