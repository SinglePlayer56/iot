package com.viktor.device_service.configs.models;

public record TableProperties(
        String name,
        String shardingColumn
) {
}
