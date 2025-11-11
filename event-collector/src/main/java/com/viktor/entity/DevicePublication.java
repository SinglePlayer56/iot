package com.viktor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("device_publication")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DevicePublication {

    @PrimaryKey
    @Column("device_id")
    private String deviceId;

    @Column("device_type")
    private String deviceType;

    @Column("meta")
    private String meta;

    private Long timestamp;
}
