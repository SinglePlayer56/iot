package com.viktor.dao;

import com.viktor.entity.DeviceEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class DeviceDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<DeviceEntity> rowMapper;

    private static final String FIND_ALL = "SELECT * FROM devices";
    private static final String FIND_BY_DEVICE_ID = "SELECT * FROM devices WHERE device_id = :device_id";
    private static final String DELETE_BY_DEVICE_ID = "DELETE FROM devices WHERE device_id = :device_id";
    private static final String SAVE = """
            INSERT INTO devices (device_id, device_type, created_at, meta)
            VALUES (:device_id, :device_type, :created_at, :meta)
            ON CONFLICT (device_id)
            DO UPDATE SET
                  device_type = EXCLUDED.device_type,
                  created_at = EXCLUDED.created_at,
                  meta = EXCLUDED.meta;
            """;

    public List<DeviceEntity> findAll() {
        return jdbcTemplate.query(FIND_ALL, rowMapper);
    }

    public Optional<DeviceEntity> findById(String deviceId) {
        return jdbcTemplate.query(FIND_BY_DEVICE_ID, Map.of("device_id", deviceId), rowMapper).stream()
                .findFirst();
    }

    public int deleteById(String deviceId) {
        return jdbcTemplate.update(DELETE_BY_DEVICE_ID, Map.of("device_id", deviceId));
    }

    public void save(DeviceEntity deviceEntity) {
        jdbcTemplate.update(SAVE,
                Map.of("device_id", deviceEntity.getDeviceId(),
                        "device_type", deviceEntity.getDeviceType(),
                        "created_at", deviceEntity.getCreatedAt(),
                        "meta", deviceEntity.getMeta()
                )
        );
    }
}
