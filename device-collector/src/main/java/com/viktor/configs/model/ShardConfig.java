package com.viktor.configs.model;

import java.util.List;

public record ShardConfig(
        String name,
        DataSourceConfig master,
        List<DataSourceConfig> replicas
) {
}
