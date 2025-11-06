package com.viktor.mapper.device;

import com.viktor.entity.DeviceEntity;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class DeviceEntityRowMapper implements RowMapper<DeviceEntity> {
    @Override
    public DeviceEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
        return DeviceEntity.builder()
                .deviceId(rs.getObject("device_id", String.class))
                .deviceType(rs.getObject("device_type", String.class))
                .createdAt(rs.getObject("created_at", Long.class))
                .meta(rs.getObject("meta", String.class))
                .build();
    }
}
