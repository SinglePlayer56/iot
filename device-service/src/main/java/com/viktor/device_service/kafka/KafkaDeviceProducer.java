package com.viktor.device_service.kafka;

import com.viktor.avro.DeviceAvro;
import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.exception.DevicePublishException;
import com.viktor.device_service.mapper.DeviceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaDeviceProducer {

    private final KafkaTemplate<String, DeviceAvro> kafkaTemplate;
    private final DeviceMapper deviceMapper;

    @Value("${spring.kafka.producer.write-create-update-topic}")
    private String createUpdateTopic;

    @Value("${spring.kafka.producer.write-delete-topic}")
    private String deleteTopic;

    public void sendToCreateUpdateTopic(DeviceDto deviceDto) {
        DeviceAvro deviceAvro = deviceMapper.toDeviceAvro(deviceDto);
        try {
            kafkaTemplate
                    .send(createUpdateTopic, deviceAvro)
                    .get();
        } catch (Exception e) {
            log.error("Failed to send device to topic {}", createUpdateTopic, e);
            throw new DevicePublishException(e.getMessage());
        }
    }

    public void sendToDeleteTopic(DeviceDto deviceDto) {
        DeviceAvro deviceAvro = deviceMapper.toDeviceAvro(deviceDto);
        try {
            kafkaTemplate
                    .send(deleteTopic, deviceAvro)
                    .get();
        } catch (Exception e) {
            log.error("Failed to send device to topic {}", deleteTopic, e);
            throw new DevicePublishException(e.getMessage());
        }
    }

}
