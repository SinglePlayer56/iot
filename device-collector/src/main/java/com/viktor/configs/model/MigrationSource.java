package com.viktor.configs.model;

public record MigrationSource(
        String url,
        String username,
        String password
) {
}