package com.viktor.service;

import com.viktor.avro.Device;
import com.viktor.entity.DevicePublication;
import com.viktor.kafka.KafkaDeviceProducer;
import com.viktor.repository.DevicePublicationRepository;
import com.viktor.service.cache.DeduplicateCache;
import com.viktor.service.device_publication.DeviceIdPublicationServiceImpl;
import com.viktor.util.DeviceDataUtil;
import com.viktor.util.DevicePublicationDataUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
public class DevicePublicationServiceTest {

    @Mock
    private DevicePublicationRepository repository;

    @Mock
    private DeduplicateCache cache;

    @Mock
    private KafkaDeviceProducer producer;

    private DeviceIdPublicationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new DeviceIdPublicationServiceImpl(repository, cache, producer);
    }

    @Test
    @DisplayName("Should send, save, and cache deviceId when not already cached")
    void givenDeviceIdNotInCache_whenProcessDevice_thenSendSaveAndAddToCache() {
        Device device = DeviceDataUtil.getDevice1();
        DevicePublication devicePublication = DevicePublicationDataUtil.getDevicePublication1();

        BDDMockito.given(cache.contains(device)).willReturn(false);
        BDDMockito.given(producer.send(device)).willReturn(Mono.empty());
        BDDMockito.given(repository.save(any(DevicePublication.class)))
                .willReturn(Mono.just(devicePublication));

        service.processDevice(device).block();

        BDDMockito.then(cache).should().contains(device);
        BDDMockito.then(producer).should().send(device);
        BDDMockito.then(repository).should().save(any(DevicePublication.class));
        BDDMockito.then(cache).should().add(device);
    }

    @Test
    @DisplayName("Should do nothing when deviceId already exists in cache")
    void givenDeviceIdAlreadyInCache_whenProcessDevice_thenNoActionPerformed() {
        Device device = DeviceDataUtil.getDevice1();

        BDDMockito.given(cache.contains(device)).willReturn(true);

        service.processDevice(device).block();

        BDDMockito.then(producer).shouldHaveNoInteractions();
        BDDMockito.then(repository).shouldHaveNoInteractions();
        BDDMockito.then(cache).should(never()).add(ArgumentMatchers.any(Device.class));
    }

    @Test
    @DisplayName("Should complete successfully when processing uncached deviceId")
    void givenDeviceIdNotInCache_whenProcessDevice_thenCompletesSuccessfully() {
        Device device = DeviceDataUtil.getDevice1();
        DevicePublication devicePublication = DevicePublicationDataUtil.getDevicePublication1();

        BDDMockito.given(cache.contains(device)).willReturn(false);
        BDDMockito.given(producer.send(device)).willReturn(Mono.empty());
        BDDMockito.given(repository.save(any(DevicePublication.class)))
                .willReturn(Mono.just(devicePublication));

        var result = service.processDevice(device);

        assertThat(result.block()).isNull();
        BDDMockito.then(producer).should().send(device);
        BDDMockito.then(repository).should().save(any(DevicePublication.class));
        BDDMockito.then(cache).should().add(device);
    }
}
