package com.viktor.service;

import com.viktor.avro.DeviceEvent;
import com.viktor.repository.EventCollectorRepository;
import com.viktor.service.device_id_publication.DeviceIdPublicationService;
import com.viktor.service.event_collector.EventCollectorServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
        DeviceEvent event1 = DeviceEvent.newBuilder()
                .setDeviceId("dev-1")
                .setEventId("e1")
                .setTimestamp(System.currentTimeMillis())
                .setType("ALERT")
                .setPayload("payload1")
                .build();

        DeviceEvent event2 = DeviceEvent.newBuilder()
                .setDeviceId("dev-2")
                .setEventId("e2")
                .setTimestamp(System.currentTimeMillis())
                .setType("ALERT")
                .setPayload("payload2")
                .build();

        DeviceEvent event3 = DeviceEvent.newBuilder()
                .setDeviceId("dev-1")
                .setEventId("e3")
                .setTimestamp(System.currentTimeMillis())
                .setType("INFO")
                .setPayload("payload3")
                .build();

        BDDMockito.given(repository.save(any())).willReturn(Mono.empty());
        BDDMockito.given(deviceIdPublicationService.processDeviceId(anyString())).willReturn(Mono.empty());

        service.processEvents(List.of(event1, event2, event3)).block();

        BDDMockito.then(repository).should(times(3)).save(any());

        BDDMockito.then(deviceIdPublicationService).should(times(1)).processDeviceId("dev-1");
        BDDMockito.then(deviceIdPublicationService).should(times(1)).processDeviceId("dev-2");

        BDDMockito.then(deviceIdPublicationService).shouldHaveNoMoreInteractions();
    }

    @Test
    @DisplayName("Should perform no actions when event list is empty")
    void givenEmptyEventList_whenProcessEvents_thenNoInteractions() {
        List<DeviceEvent> emptyList = List.of();

        service.processEvents(emptyList).block();

        BDDMockito.then(repository).shouldHaveNoInteractions();
        BDDMockito.then(deviceIdPublicationService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("Should save and publish deviceId successfully for single event")
    void givenSingleEvent_whenProcessEvents_thenCompletesSuccessfully() {
        DeviceEvent event = DeviceEvent.newBuilder()
                .setDeviceId("dev-3")
                .setEventId("e4")
                .setTimestamp(System.currentTimeMillis())
                .setType("WARNING")
                .setPayload("payload4")
                .build();

        BDDMockito.given(repository.save(any())).willReturn(Mono.empty());
        BDDMockito.given(deviceIdPublicationService.processDeviceId("dev-3")).willReturn(Mono.empty());

        var result = service.processEvents(List.of(event));

        assertThat(result.block()).isNull();
        BDDMockito.then(repository).should().save(any());
        BDDMockito.then(deviceIdPublicationService).should().processDeviceId("dev-3");
    }
}
