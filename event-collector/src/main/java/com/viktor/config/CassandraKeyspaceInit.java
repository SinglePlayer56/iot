package com.viktor.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification;
import org.springframework.data.cassandra.core.cql.keyspace.DropKeyspaceSpecification;

import java.util.Collections;
import java.util.List;

@Configuration
public class CassandraKeyspaceInit extends AbstractCassandraConfiguration {

    @Value("${spring.cassandra.contact-points}")
    private String host;

    @Value("${spring.cassandra.keyspace-name}")
    private String keyspace;

    @Value("${spring.cassandra.port}")
    private Integer port;

    @Value("${spring.cassandra.local-datacenter}")
    private String datacenter;

    @Value("${spring.cassandra.schema-action}")
    private String action;

    @Override
    protected String getKeyspaceName() {
        return keyspace;
    }

    @Override
    protected String getContactPoints() {
        return host;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected String getLocalDataCenter() {
        return datacenter;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.valueOf(action.toUpperCase());
    }

    @Override
    protected List<CreateKeyspaceSpecification> getKeyspaceCreations() {
        CreateKeyspaceSpecification specification =
                CreateKeyspaceSpecification.createKeyspace(getKeyspaceName())
                        .ifNotExists()
                        .withSimpleReplication(1);

        return Collections.singletonList(specification);
    }

    @Override
    protected List<DropKeyspaceSpecification> getKeyspaceDrops() {
        return Collections.emptyList();
    }
}