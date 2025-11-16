package com.viktor.device_service.configs;

import com.viktor.device_service.configs.models.DataSourceConfig;
import com.viktor.device_service.configs.models.ShardConfig;
import com.viktor.device_service.configs.models.ShardingProperties;
import com.viktor.device_service.configs.models.TableProperties;
import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.infra.config.mode.ModeConfiguration;
import org.apache.shardingsphere.mode.repository.standalone.StandalonePersistRepositoryConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.ReadwriteSplittingRuleConfiguration;
import org.apache.shardingsphere.readwritesplitting.config.rule.ReadwriteSplittingDataSourceGroupRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.NoneShardingStrategyConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Configuration
@EnableConfigurationProperties(ShardingProperties.class)
@RequiredArgsConstructor
public class ShardingSphereConfig {

    private final ShardingProperties props;

    private Map<String, DataSource> createDatasources() {
        Map<String, DataSource> map = new LinkedHashMap<>();

        for (ShardConfig shard : props.shards()) {
            map.put(shard.name() + "_master", createHikari(shard.master()));

            if (shard.replicas() != null) {
                int idx = 0;
                for (DataSourceConfig replica : shard.replicas()) {
                    map.put(shard.name() + "_replica_" + idx, createHikari(replica));
                    idx++;
                }
            }
        }
        return map;
    }

    private HikariDataSource createHikari(DataSourceConfig dataSourceConfig) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dataSourceConfig.url());
        ds.setUsername(dataSourceConfig.username());
        ds.setPassword(dataSourceConfig.password());
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setMaximumPoolSize(5);
        return ds;
    }

    private ReadwriteSplittingRuleConfiguration createReadWriteConfig() {

        List<ReadwriteSplittingDataSourceGroupRuleConfiguration> groups = new ArrayList<>();

        for (ShardConfig shard : props.shards()) {
            String masterName = shard.name() + "_master";

            List<String> readNames = new ArrayList<>();
            if (shard.replicas() != null) {
                for (int i = 0; i < shard.replicas().size(); i++) {
                    readNames.add(shard.name() + "_replica_" + i);
                }
            }

            groups.add(
                    new ReadwriteSplittingDataSourceGroupRuleConfiguration(
                            shard.name(),
                            masterName,
                            readNames,
                            "round_robin"
                    )
            );
        }

        return new ReadwriteSplittingRuleConfiguration(
                groups,
                Map.of("round_robin",
                        new AlgorithmConfiguration("ROUND_ROBIN", new Properties()))
        );
    }

    private ShardingRuleConfiguration createShardingRule() {

        ShardingRuleConfiguration shardingRule = new ShardingRuleConfiguration();

        TableProperties table = props.table();
        String nodes = generateActualDataNodes(table.name());
        var tableRule = getShardingTableRuleConfiguration(table, nodes);
        shardingRule.getTables().add(tableRule);

        Properties inlineProps = new Properties();
        inlineProps.setProperty("algorithm-expression", props.shardingAlgorithm());

        shardingRule.getShardingAlgorithms().put(
                "inline_algo",
                new AlgorithmConfiguration("INLINE", inlineProps)
        );

        return shardingRule;
    }

    private ShardingTableRuleConfiguration getShardingTableRuleConfiguration(TableProperties table, String nodes) {
        ShardingTableRuleConfiguration tableRule =
                new ShardingTableRuleConfiguration(
                        table.name(),
                        nodes
                );

        tableRule.setTableShardingStrategy(
                new NoneShardingStrategyConfiguration()
        );

        tableRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration(
                        table.shardingColumn(),
                        "inline_algo"
                )
        );
        return tableRule;
    }

    private String generateActualDataNodes(String tableName) {
        StringBuilder sb = new StringBuilder();
        int size = props.shards().size();

        for (int i = 0; i < size; i++) {
            if (i > 0) sb.append(",");
            sb.append(props.shards().get(i).name()).append(".").append(tableName);
        }

        return sb.toString();
    }

    private Properties createGlobalProps() {
        Properties p = new Properties();
        p.setProperty("sql-show", "true");
        return p;
    }

    private ModeConfiguration createModeConfig() {
        return new ModeConfiguration("Standalone",
                new StandalonePersistRepositoryConfiguration("JDBC", new Properties()));
    }

    @Bean
    public DataSource shardingSphereDataSource() throws SQLException {
        return ShardingSphereDataSourceFactory.createDataSource(
                props.dbName(),
                createModeConfig(),
                createDatasources(),
                List.of(createReadWriteConfig(), createShardingRule()),
                createGlobalProps()
        );
    }
}
