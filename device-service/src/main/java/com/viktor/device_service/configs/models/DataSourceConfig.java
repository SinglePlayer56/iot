package com.viktor.device_service.configs.models;

public record DataSourceConfig(
        String url,
        String username,
        String password
) {
}