package com.viktor.device_service.it;

import com.viktor.device.dto.DeviceDto;
import com.viktor.device_service.service.redis.RedisCacheService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class RedisCacheServiceImplIT extends AbstractIntegrationTest {

    @Autowired
    RedisCacheService redisCacheService;

    @Autowired
    RedisTemplate<String, DeviceDto> redisTemplate;

    @Test
    @DisplayName("given value saved in Redis when get then value must be returned")
    void givenSetValue_whenGet_thenReturned() {
        DeviceDto dto = new DeviceDto("42", "TYPE", 111L, "meta");
        redisCacheService.set("42", dto);

        DeviceDto res = redisCacheService.get("42");

        assertNotNull(res);
        assertEquals("42", res.getDeviceId());
        assertEquals("TYPE", res.getDeviceType());
    }

    @Test
    @DisplayName("given key exists when evict then key is removed from cache")
    void givenKeyExists_whenEvict_thenKeyRemoved() {
        DeviceDto dto = new DeviceDto("99", "TYPE", 111L, "meta");
        redisCacheService.set("99", dto);
        assertNotNull(redisCacheService.get("99"));

        redisCacheService.evict("99");

        assertNull(redisCacheService.get("99"));
    }

    @Test
    @DisplayName("given value saved when checking TTL then TTL must be positive")
    void givenSetValue_whenCheckTTL_thenTTLPositive() {
        DeviceDto dto = new DeviceDto("777", "TYPE", 111L, "meta");
        redisCacheService.set("777", dto);

        Long ttl = redisTemplate.getExpire("777");

        assertNotNull(ttl, "TTL must not be null");
        assertTrue(ttl > 0, "TTL must be > 0");
    }
}