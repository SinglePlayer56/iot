package com.viktor.configs.model;

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
