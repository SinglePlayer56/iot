package com.viktor.service;

import com.viktor.avro.DeviceAvro;
import com.viktor.avro.DeviceEventAvro;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_publication.DeviceIdPublicationService;
import com.viktor.service.event_collector.EventCollectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class EventCollectorServiceTest {

    @Mock
    private EventCollectorRepository repository;

    @Mock
    private DeviceIdPublicationService deviceIdPublicationService;

    private EventCollectorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new EventCollectorServiceImpl(repository, deviceIdPublicationService);
    }

    @Test
    @DisplayName("Should save all events and publish distinct deviceIds")
    void givenListOfEvents_whenProcessEvents_thenSaveEachAndPublishDistinctDeviceIds() {
        DeviceAvro device1 = new DeviceAvro();
        device1.setDeviceId("dev-1");
        DeviceEventAvro event1 = DeviceEventAvro.newBuilder()
                .setDevice(device1)
                .setEventId("e1")
                .setTimestamp(System.currentTimeMillis())
                .setType("ALERT")
                .setPayload("payload1")
                .build();

        DeviceAvro device2 = new DeviceAvro();
        device2.setDeviceId("dev-2");
        DeviceEventAvro event2 = DeviceEventAvro.newBuilder()
                .setDevice(device2)
                .setEventId("e2")
                .setTimestamp(System.currentTimeMillis())
                .setType("ALERT")
                .setPayload("payload2")
                .build();

        DeviceAvro device3 = new DeviceAvro();
        device3.setDeviceId("dev-3");
        DeviceEventAvro event3 = DeviceEventAvro.newBuilder()
                .setDevice(device3)
                .setEventId("e3")
                .setTimestamp(System.currentTimeMillis())
                .setType("INFO")
                .setPayload("payload3")
                .build();

        BDDMockito.given(repository.save(any())).willReturn(Mono.empty());
        BDDMockito.given(deviceIdPublicationService.processDevice(ArgumentMatchers.any(DeviceAvro.class)))
                .willReturn(Mono.empty());

        service.processEvents(List.of(event1, event2, event3)).block();

        BDDMockito.then(repository).should(times(3)).save(any());

        BDDMockito.then(deviceIdPublicationService).should(times(1)).processDevice(device1);
        BDDMockito.then(deviceIdPublicationService).should(times(1)).processDevice(device2);

        BDDMockito.then(deviceIdPublicationService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Should perform no actions when event list is empty")
    void givenEmptyEventList_whenProcessEvents_thenNoInteractions() {
        List<DeviceEventAvro> emptyList = List.of();

        service.processEvents(emptyList).block();

        BDDMockito.then(repository).shouldHaveNoInteractions();
        BDDMockito.then(deviceIdPublicationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should save and publish deviceId successfully for single event")
    void givenSingleEvent_whenProcessEvents_thenCompletesSuccessfully() {
        String deviceId = "dev-3";
        DeviceAvro device = new DeviceAvro();
        device.setDeviceId(deviceId);
        DeviceEventAvro event = DeviceEventAvro.newBuilder()
                .setDevice(device)
                .setEventId("e4")
                .setTimestamp(System.currentTimeMillis())
                .setType("WARNING")
                .setPayload("payload4")
                .build();

        BDDMockito.given(repository.save(any())).willReturn(Mono.empty());
        BDDMockito.given(deviceIdPublicationService.processDevice(device)).willReturn(Mono.empty());

        var result = service.processEvents(List.of(event));

        assertThat(result.block()).isNull();
        BDDMockito.then(repository).should().save(any());
        BDDMockito.then(deviceIdPublicationService).should().processDevice(device);
    }
}
