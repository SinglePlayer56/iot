package com.viktor.configs;

import com.viktor.configs.model.DataSourceConfig;
import lombok.Data;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Data
@Configuration
@ConfigurationProperties(prefix = "spring.flyway")
public class FlywayMigrationConfig {

    private List<DataSourceConfig> dataSources;

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer() {
        return new FlywayMigrationInitializer(Flyway.configure().load(), _ -> {
            for (DataSourceConfig dataSource : dataSources) {
                Flyway.configure()
                        .dataSource(dataSource.url(), dataSource.username(), dataSource.password())
                        .locations("classpath:db/migration")
                        .load()
                        .migrate();
            }
        });
    }
}
