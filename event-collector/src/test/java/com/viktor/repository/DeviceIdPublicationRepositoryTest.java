package com.viktor.repository;

import com.viktor.entity.DeviceIdPublication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class DeviceIdPublicationRepositoryTest {

    @Mock
    private DeviceIdPublicationRepository repository;

    @Test
    @DisplayName("Should save DeviceIdPublication entity successfully")
    void givenDeviceIdPublication_whenSave_thenEntityIsPersisted() {
        DeviceIdPublication entity = new DeviceIdPublication("dev-1", System.currentTimeMillis());
        BDDMockito.given(repository.save(entity)).willReturn(Mono.just(entity));

        DeviceIdPublication saved = repository.save(entity).block();

        assertThat(saved).isNotNull();
        assertThat(saved.getDeviceId()).isEqualTo("dev-1");
        BDDMockito.then(repository).should().save(entity);
    }
}
