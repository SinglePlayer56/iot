package com.viktor.configs.model;

public record TableProperties(
        String name,
        String shardingColumn
) {
}
