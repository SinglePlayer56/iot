package com.viktor.device_service.service.redis;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.annotations.CacheMetric;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisCacheServiceImpl implements RedisCacheService {

    private final RedisTemplate<String, DeviceDto> redisTemplate;

    @Value("${spring.data.redis.ttl}")
    private Integer ttl;

    @CacheMetric(hit = "device.cache.hit", miss = "device.cache.miss")
    public DeviceDto get(String deviceId) {
        return redisTemplate.opsForValue().get(deviceId);
    }

    public void set(String deviceId, DeviceDto deviceDto) {
        redisTemplate.opsForValue().set(deviceId, deviceDto, Duration.ofSeconds(ttl));
    }

    public void evict(String deviceId) {
        redisTemplate.delete(deviceId);
    }
}
