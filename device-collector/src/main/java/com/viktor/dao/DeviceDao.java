package com.viktor.dao;

import com.viktor.entity.DeviceEntity;
import com.viktor.exception.ShardErrorException;
import com.viktor.util.ShardingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DeviceDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private static final String SAVE = """
            INSERT INTO devices (device_id, device_type, created_at, meta)
            VALUES (:device_id, :device_type, :created_at, :meta)
            ON CONFLICT (device_id)
            DO UPDATE SET
                  device_type = EXCLUDED.device_type,
                  created_at = EXCLUDED.created_at,
                  meta = EXCLUDED.meta;
            """;

    public void save(DeviceEntity deviceEntity) {
        try {
            jdbcTemplate.update(SAVE,
                    Map.of("device_id", deviceEntity.getDeviceId(),
                            "device_type", deviceEntity.getDeviceType(),
                            "created_at", deviceEntity.getCreatedAt(),
                            "meta", deviceEntity.getMeta()
                    )
            );
        } catch (Exception e) {
            var shardName = ShardingUtil.determineShard(deviceEntity.getDeviceId());
            throw new ShardErrorException(e.getMessage(), shardName);
        }
    }
}
