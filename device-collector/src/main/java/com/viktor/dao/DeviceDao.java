package com.viktor.dao;

import com.viktor.entity.DeviceEntity;
import com.viktor.exception.ShardErrorException;
import com.viktor.util.ShardingUtil;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Slf4j
public class DeviceDao {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final RowMapper<DeviceEntity> rowMapper;

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

    @WithSpan("find-by-id-device-dao")
    public Optional<DeviceEntity> findById(String deviceId) {
        try {
            return jdbcTemplate.query(FIND_BY_DEVICE_ID, Map.of("device_id", deviceId), rowMapper).stream()
                    .findFirst();
        } catch (Exception e) {
            var shardName = ShardingUtil.determineShard(deviceId);
            log.error("Error finding entity to shard {}", shardName, e);
            throw new ShardErrorException(e.getMessage(), shardName);
        }
    }

    @WithSpan("delete-device-dao")
    public int deleteById(String deviceId) {
        try {
            int rows = jdbcTemplate.update(DELETE_BY_DEVICE_ID, Map.of("device_id", deviceId));
            log.info("delete device with device_id {}", deviceId);
            if (rows > 0) {
                log.info("delete device with device_id {} success", deviceId);
            }
            return rows;
        } catch (Exception e) {
            log.error("error while delete device with device_id {}", deviceId, e);
            var shardName = ShardingUtil.determineShard(deviceId);
            throw new ShardErrorException(e.getMessage(), shardName);
        }
    }

    @WithSpan("save-device-dao")
    public void save(DeviceEntity deviceEntity) {
        try {
            jdbcTemplate.update(SAVE,
                    Map.of("device_id", deviceEntity.getDeviceId(),
                            "device_type", deviceEntity.getDeviceType(),
                            "created_at", deviceEntity.getCreatedAt(),
                            "meta", deviceEntity.getMeta()
                    )
            );
            log.info("Save device in DAO {}", deviceEntity);
        } catch (Exception e) {
            log.error("error while save device in DAO {}", deviceEntity, e);
            var shardName = ShardingUtil.determineShard(deviceEntity.getDeviceId());
            throw new ShardErrorException(e.getMessage(), shardName);
        }
    }
}
