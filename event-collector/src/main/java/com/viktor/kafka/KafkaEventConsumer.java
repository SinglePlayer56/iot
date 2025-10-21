package com.viktor.kafka;

import com.viktor.avro.DeviceEvent;
import com.viktor.service.event_collector.EventCollectorService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private final EventCollectorService eventCollectorService;

    @KafkaListener(topics = "events", groupId = "device-events-consumer")
    public void listen(DeviceEvent deviceEvent) {
        eventCollectorService.processEvent(deviceEvent);
    }
}
