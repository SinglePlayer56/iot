package com.viktor.repository;

import com.viktor.entity.DeviceEventByDevice;
import com.viktor.util.EventDataUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class EventCollectorRepositoryTest {

    @Mock
    private EventCollectorRepository repository;

    @Test
    @DisplayName("Should save DeviceEventByDevice entity successfully")
    void givenDeviceEvent_whenSave_thenEventIsPersisted() {
        DeviceEventByDevice event = EventDataUtil.getDeviceEventByDevice1();

        BDDMockito.given(repository.save(event)).willReturn(Mono.just(event));

        DeviceEventByDevice savedEvent = repository.save(event).block();

        assertThat(savedEvent).isNotNull();
        assertThat(savedEvent.getDeviceId()).isEqualTo("dev-1");
        assertThat(savedEvent.getType()).isEqualTo("ALERT");
        BDDMockito.then(repository).should().save(event);
    }
}
