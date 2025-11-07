package com.viktor.dao;

import com.viktor.entity.DeviceEntity;
import com.viktor.utils.DeviceEntityDataUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
public class TestDeviceDao {

    @Mock
    private NamedParameterJdbcTemplate jdbcTemplate;

    @InjectMocks
    private DeviceDao deviceDao;

    @Test
    void givenDeviceExists_whenFindById_thenReturnEntity() {
        var entity = DeviceEntityDataUtil.getDeviceEntity();
        BDDMockito.given(jdbcTemplate.query(anyString(), anyMap(), any(RowMapper.class)))
                .willReturn(List.of(entity));

        Optional<DeviceEntity> result = deviceDao.findById(entity.getDeviceId());

        assertThat(result).isPresent();
        assertThat(result.get().getDeviceId()).isEqualTo(entity.getDeviceId());
    }

    @Test
    void givenDeviceExists_whenDeleteById_thenReturnRowsUpdated() {
        BDDMockito.given(jdbcTemplate.update(anyString(), anyMap())).willReturn(1);

        int rows = deviceDao.deleteById("dev-1");

        assertThat(rows).isEqualTo(1);
        BDDMockito.then(jdbcTemplate).should(times(1)).update(anyString(), anyMap());
    }

    @Test
    void givenValidEntity_whenSave_thenNoExceptions() {
        var entity = DeviceEntityDataUtil.getDeviceEntity();

        BDDMockito.given(jdbcTemplate.update(anyString(), anyMap())).willReturn(1);

        assertThatCode(() -> deviceDao.save(entity))
                .doesNotThrowAnyException();

        BDDMockito.then(jdbcTemplate).should(times(1)).update(anyString(), anyMap());
    }
}
