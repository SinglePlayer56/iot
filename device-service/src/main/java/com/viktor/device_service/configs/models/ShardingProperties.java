package com.viktor.device_service.configs.models;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix="sharding")
public record ShardingProperties(
        String dbName,
        String shardingAlgorithm,
        TableProperties table,
        List<ShardConfig> shards
) {
}
