package com.viktor.service;

import com.viktor.entity.DeviceIdPublication;
import com.viktor.kafka.KafkaDeviceIdProducer;
import com.viktor.repository.DeviceIdPublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import com.viktor.service.device_id_publication.DeviceIdPublicationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class DeviceIdPublicationServiceTest {

    @Mock
    private DeviceIdPublicationRepository repository;

    @Mock
    private DeduplicateCache cache;

    @Mock
    private KafkaDeviceIdProducer producer;

    private DeviceIdPublicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceIdPublicationServiceImpl(repository, cache, producer);
    }

    @Test
    @DisplayName("Should send, save, and cache deviceId when not already cached")
    void givenDeviceIdNotInCache_whenProcessDeviceId_thenSendSaveAndAddToCache() {
        String deviceId = "dev-1";
        BDDMockito.given(cache.contains(deviceId)).willReturn(false);
        BDDMockito.given(producer.send(deviceId)).willReturn(Mono.empty());
        BDDMockito.given(repository.save(any(DeviceIdPublication.class)))
                .willReturn(Mono.just(new DeviceIdPublication(deviceId, System.currentTimeMillis())));

        service.processDeviceId(deviceId).block();

        BDDMockito.then(cache).should().contains(deviceId);
        BDDMockito.then(producer).should().send(deviceId);
        BDDMockito.then(repository).should().save(any(DeviceIdPublication.class));
        BDDMockito.then(cache).should().add(deviceId);
    }

    @Test
    @DisplayName("Should do nothing when deviceId already exists in cache")
    void givenDeviceIdAlreadyInCache_whenProcessDeviceId_thenNoActionPerformed() {
        String deviceId = "dev-1";
        BDDMockito.given(cache.contains(deviceId)).willReturn(true);

        service.processDeviceId(deviceId).block();

        BDDMockito.then(producer).shouldHaveNoInteractions();
        BDDMockito.then(repository).shouldHaveNoInteractions();
        BDDMockito.then(cache).should(never()).add(anyString());
    }

    @Test
    @DisplayName("Should complete successfully when processing uncached deviceId")
    void givenDeviceIdNotInCache_whenProcessDeviceId_thenCompletesSuccessfully() {
        String deviceId = "dev-2";
        BDDMockito.given(cache.contains(deviceId)).willReturn(false);
        BDDMockito.given(producer.send(deviceId)).willReturn(Mono.empty());
        BDDMockito.given(repository.save(any(DeviceIdPublication.class)))
                .willReturn(Mono.just(new DeviceIdPublication(deviceId, System.currentTimeMillis())));

        var result = service.processDeviceId(deviceId);

        assertThat(result.block()).isNull();
        BDDMockito.then(producer).should().send(deviceId);
        BDDMockito.then(repository).should().save(any(DeviceIdPublication.class));
        BDDMockito.then(cache).should().add(deviceId);
    }
}
