package com.viktor.device_service.kafka;

import com.viktor.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceProducer {

    private final KafkaTemplate<String, Device> kafkaTemplate;

}
