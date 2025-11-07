package com.viktor.configs;

import org.apache.shardingsphere.driver.api.yaml.YamlShardingSphereDataSourceFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.io.Resource;

import javax.sql.DataSource;

@Configuration
@Profile("!test")
public class ShardingSphereConfig {

    @Value("classpath:sharding.yaml")
    private Resource resource;

    @Primary
    @Bean
    public DataSource dataSource(ConfigurableEnvironment env) throws Exception {
        String yaml = new String(resource.getInputStream().readAllBytes());
        PropertySourcesPropertyResolver resolver = new PropertySourcesPropertyResolver(env.getPropertySources());
        yaml = resolver.resolvePlaceholders(yaml);
        return YamlShardingSphereDataSourceFactory.createDataSource(yaml.getBytes());
    }
}
