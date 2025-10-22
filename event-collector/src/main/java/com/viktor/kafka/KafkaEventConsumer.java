package com.viktor.kafka;

import com.viktor.avro.DeviceEvent;
import com.viktor.service.event_collector.EventCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final EventCollectorService eventCollectorService;

    @KafkaListener(topics = "events", groupId = "device-events-consumer")
    public void listen(List<DeviceEvent> deviceEvent, Acknowledgment ack) {
        eventCollectorService.processEvents(deviceEvent)
                .doOnSuccess(_ -> ack.acknowledge())
                .subscribe();

    }
}
