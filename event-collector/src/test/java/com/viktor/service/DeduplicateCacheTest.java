package com.viktor.service;

import com.viktor.entity.DeviceIdPublication;
import com.viktor.repository.DeviceIdPublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;

@ExtendWith(MockitoExtension.class)
public class DeduplicateCacheTest {

    @Mock
    private DeviceIdPublicationRepository repository;

    private DeduplicateCache cache;

    @BeforeEach
    void init() {
        cache = new DeduplicateCache(repository);
    }

    @Test
    @DisplayName("Should add deviceId to cache successfully")
    void givenEmptyCache_whenAddDeviceId_thenContainsReturnsTrue() {
        String deviceId = "dev-1";

        cache.add(deviceId);

        assertThat(cache.contains(deviceId))
                .as("Cache should contain deviceId after adding it")
                .isTrue();
    }

    @Test
    @DisplayName("Should load all deviceIds from repository into cache")
    void givenRepositoryHasIds_whenInit_thenAllIdsLoadedIntoBloomFilter() {
        String existingId1 = "dev-1";
        String existingId2 = "dev-2";
        BDDMockito.given(repository.findAll())
                .willReturn(Flux.just(
                        new DeviceIdPublication(existingId1, System.currentTimeMillis()),
                        new DeviceIdPublication(existingId2, System.currentTimeMillis())
                ));

        cache.initializeCacheFromDatabase();
        cache.add("dev-3");

        assertThat(cache.contains(existingId1)).isTrue();
        assertThat(cache.contains(existingId2)).isTrue();
        assertThat(cache.contains("dev-3")).isTrue();

        BDDMockito.then(repository).should(atLeastOnce()).findAll();
    }

    @Test
    @DisplayName("Should return false for unknown deviceId")
    void givenDeviceIdNotAdded_whenContains_thenReturnsFalse() {
        String deviceId = "dev-unknown";

        boolean exists = cache.contains(deviceId);

        assertThat(exists)
                .as("DeviceId not added yet should not be present in cache")
                .isFalse();
        BDDMockito.then(repository).shouldHaveNoInteractions();
    }
}
