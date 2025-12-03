package com.viktor.device_service.configs.models;

import java.util.List;

public record ShardConfig(
        String name,
        DataSourceConfig master,
        List<DataSourceConfig> replicas
) {
}
