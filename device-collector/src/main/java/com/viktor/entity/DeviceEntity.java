package com.viktor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeviceEntity {
    private String deviceId;
    private String deviceType;
    private Long createdAt;
    private String meta;
}
