package com.viktor.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("device_id_publication")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeviceIdPublication {

    @PrimaryKey
    @Column("device_id")
    private String deviceId;

    private Long timestamp;
}
