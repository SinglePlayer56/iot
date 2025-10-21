package com.viktor.entity;

import lombok.Data;
import org.springframework.data.cassandra.core.cql.PrimaryKeyType;
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn;
import org.springframework.data.cassandra.core.mapping.Table;

@Data
@Table("device_events_by_device")
public class DeviceEventByDevice {

    @PrimaryKeyColumn(name = "device_id", type = PrimaryKeyType.PARTITIONED)
    private String deviceId;

    @PrimaryKeyColumn(name = "event_id", type = PrimaryKeyType.CLUSTERED)
    private String eventId;

    private Long timestamp;

    private String type;

    private String payload;
}
