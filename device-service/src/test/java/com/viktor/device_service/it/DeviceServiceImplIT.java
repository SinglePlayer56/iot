package com.viktor.device_service.it;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.entity.Device;
import com.viktor.device_service.kafka.KafkaDeviceProducer;
import com.viktor.device_service.repository.DeviceRepository;
import com.viktor.device_service.service.device.DeviceService;
import com.viktor.device_service.service.redis.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class DeviceServiceImplIT extends AbstractIntegrationTest {

    @Autowired
    private DeviceService deviceService;

    @Autowired
    private RedisCacheService redisCache;

    @Autowired
    private DeviceRepository deviceRepository;

    @MockitoBean
    private KafkaDeviceProducer producer;

    private DeviceDto newDevice(String id) {
        return new DeviceDto(id, "CAMERA", Instant.now().toEpochMilli(), "{}");
    }

    @Test
    @DisplayName("given new device when createDevice called then publish event and do not use cache or DB")
    void givenNewDevice_whenCreateDevice_thenPublishToKafkaAndNotHitCache() {
        DeviceDto dto = newDevice("dev-1");

        deviceService.createDevice(dto);

        verify(producer, times(1)).sendToCreateUpdateTopic(dto);
        assertThat(redisCache.get("dev-1")).isNull();
        assertThat(deviceRepository.findById("dev-1")).isEmpty();
    }

    @Test
    @DisplayName("given device in DB when findById called then return device and cache result")
    void givenDeviceInDb_whenFindById_thenLoadFromDbAndPutToCache() {
        DeviceDto dto = newDevice("dev-2");

        deviceRepository.save(Device.builder()
                .deviceId(dto.getDeviceId())
                .deviceType(dto.getDeviceType())
                .createdAt(dto.getCreatedAt())
                .meta(dto.getMeta())
                .build()
        );

        assertThat(redisCache.get("dev-2")).isNull();

        DeviceDto result = deviceService.findById("dev-2");

        assertThat(result).isNotNull();
        assertThat(result.getDeviceId()).isEqualTo("dev-2");
        assertThat(redisCache.get("dev-2")).isNotNull();
    }

    @Test
    @DisplayName("given cached device when updateDevice called then publish event and evict cache")
    void givenCachedDevice_whenUpdateDevice_thenPublishEventAndEvictCache() {
        DeviceDto dto = newDevice("dev-3");
        redisCache.set("dev-3", dto);

        deviceService.updateDevice("dev-3", dto);

        verify(producer, times(1)).sendToCreateUpdateTopic(dto);
        assertThat(redisCache.get("dev-3")).isNull();
    }

    @Test
    @DisplayName("given device in cache when deleteDevice called then publish delete event and evict cache")
    void givenCachedDevice_whenDeleteDevice_thenPublishDeleteEventAndEvictCache() {
        DeviceDto dto = newDevice("dev-4");
        redisCache.set("dev-4", dto);

        deviceService.deleteById("dev-4");

        verify(producer, times(1)).sendToDeleteTopic(dto);
        assertThat(redisCache.get("dev-4")).isNull();
    }

    @Test
    @DisplayName("given device only in DB when deleteDevice called then device is loaded and delete event published")
    void givenDbDevice_whenDeleteDevice_thenLoadFromDbAndPublish() {
        DeviceDto dto = newDevice("dev-5");

        deviceRepository.save(Device.builder()
                .deviceId(dto.getDeviceId())
                .deviceType(dto.getDeviceType())
                .createdAt(dto.getCreatedAt())
                .meta(dto.getMeta())
                .build()
        );

        deviceService.deleteById("dev-5");

        verify(producer, times(1)).sendToDeleteTopic(any());
    }
}