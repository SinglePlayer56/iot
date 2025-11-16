package com.viktor.service;

import com.viktor.avro.DeviceAvro;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import com.viktor.util.DeviceDataUtil;
import com.viktor.util.DevicePublicationDataUtil;
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
    private DevicePublicationRepository repository;

    private DeduplicateCache cache;

    @BeforeEach
    void init() {
        cache = new DeduplicateCache(repository);
    }

    @Test
    @DisplayName("Should add deviceId to cache successfully")
    void givenEmptyCache_whenAddDeviceId_thenContainsReturnsTrue() {
        var device = DeviceDataUtil.getDevice1();

        cache.add(device);

        assertThat(cache.contains(device))
                .as("Cache should contain deviceId after adding it")
                .isTrue();
    }

    @Test
    @DisplayName("Should load all deviceIds from repository into cache")
    void givenRepositoryHasIds_whenInit_thenAllIdsLoadedIntoBloomFilter() {
        var devicePub1 = DevicePublicationDataUtil.getDevicePublication1();
        var devicePub2 = DevicePublicationDataUtil.getDevicePublication2();
        BDDMockito.given(repository.findAll())
                .willReturn(Flux.just(
                        devicePub1,
                        devicePub2
                ));

        cache.initializeCacheFromDatabase();
        var device1 = DeviceDataUtil.getDevice1();
        var device2 = DeviceDataUtil.getDevice2();
        var device3 = DeviceDataUtil.getDevice3();
        cache.add(device3);

        assertThat(cache.contains(device1)).isTrue();
        assertThat(cache.contains(device2)).isTrue();
        assertThat(cache.contains(device3)).isTrue();

        BDDMockito.then(repository).should(atLeastOnce()).findAll();
    }

    @Test
    @DisplayName("Should return false for unknown deviceId")
    void givenDeviceIdNotAdded_whenContains_thenReturnsFalse() {
        DeviceAvro device = DeviceDataUtil.getDevice1();

        boolean exists = cache.contains(device);

        assertThat(exists)
                .as("DeviceId not added yet should not be present in cache")
                .isFalse();
        BDDMockito.then(repository).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should return true for added device and false after its data changes")
    void givenAddedDevice_whenMetaUpdated_thenContainsReturnsFalse() {
        DeviceAvro device = DeviceDataUtil.getDevice1();

        cache.add(device);
        boolean exists = cache.contains(device);

        assertThat(exists)
                .as("Device just added should be present in cache")
                .isTrue();

        device.setMeta("updated");
        exists = cache.contains(device);

        assertThat(exists)
                .as("Device with updated data should not be present in cache")
                .isFalse();
    }
}
