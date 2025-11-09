package com.viktor.configs.model;

public record DataSourceConfig(
        String url,
        String username,
        String password
) {
}