package com.viktor.device_service.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractIntegrationTest {

    private static final Network network;

    public static final GenericContainer<?> kafka;
    public static final GenericContainer<?> schemaRegistry;
    private static final PostgreSQLContainer<?> shard0;
    private static final PostgreSQLContainer<?> shard1;
    public static final KeycloakContainer keycloak;
    private static final GenericContainer<?> redis;

    public static final String HOST = Objects.nonNull(System.getenv("TESTCONTAINERS_HOST_OVERRIDE")) ?
            System.getenv("TESTCONTAINERS_HOST_OVERRIDE") : "localhost";

    static {
        network = Network.newNetwork();

        shard0 = new PostgreSQLContainer<>("postgres:17-alpine")
                .withNetwork(network)
                .withNetworkAliases("shard_master_0")
                .withDatabaseName("devices")
                .withUsername("postgres")
                .withPassword("postgres")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));

        shard0.start();

        shard1 = new PostgreSQLContainer<>("postgres:17-alpine")
                .withNetwork(network)
                .withNetworkAliases("shard_master_1")
                .withDatabaseName("devices")
                .withUsername("postgres")
                .withPassword("postgres")
                .withEnv("POSTGRES_HOST_AUTH_METHOD", "trust")
                .withExposedPorts(5432)
                .waitingFor(Wait.forListeningPort())
                .withStartupTimeout(Duration.ofMinutes(2));

        shard1.start();

        kafka = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-kafka:7.7.5"))
                .withNetwork(network)
                .withNetworkAliases("kafka")
                .withEnv("KAFKA_NODE_ID", "1")
                .withEnv("CLUSTER_ID", "4L6g3nShT-eMCtK--X86sw") // можно сгенерить через kafka-storage random-uuid
                .withEnv("KAFKA_PROCESS_ROLES", "broker,controller")
                .withEnv("KAFKA_CONTROLLER_QUORUM_VOTERS", "1@kafka:9093")
                .withEnv("KAFKA_LISTENERS", "PLAINTEXT://:9092,CONTROLLER://:9093,PLAINTEXT_HOST://:9094")
                .withEnv("KAFKA_ADVERTISED_LISTENERS", "PLAINTEXT://kafka:9092,PLAINTEXT_HOST://" + HOST + ":9094")
                .withEnv("KAFKA_LISTENER_SECURITY_PROTOCOL_MAP", "PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT")
                .withEnv("KAFKA_CONTROLLER_LISTENER_NAMES", "CONTROLLER")
                .withEnv("KAFKA_INTER_BROKER_LISTENER_NAME", "PLAINTEXT")
                .withEnv("KAFKA_AUTO_CREATE_TOPICS_ENABLE", "true")
                .withEnv("KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR", "1")
                .withEnv("KAFKA_TRANSACTION_STATE_LOG_MIN_ISR", "1")
                .withEnv("ALLOW_PLAINTEXT_LISTENER", "yes")
                .withCreateContainerCmdModifier(cmd -> cmd.getHostConfig()
                        .withPortBindings(new PortBinding(Ports.Binding.bindPort(9094), new ExposedPort(9094)))
                )
                .waitingFor(Wait.forLogMessage(".*Kafka Server started.*", 1))
                .withStartupTimeout(Duration.ofMinutes(2));

        kafka.start();

        schemaRegistry = new GenericContainer<>(DockerImageName.parse("confluentinc/cp-schema-registry:7.7.5"))
                .withNetwork(network)
                .withNetworkAliases("schema-registry")
                .dependsOn(kafka)
                .withExposedPorts(8081)
                .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
                .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
                .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://kafka:9092")
                .waitingFor(Wait.forHttp("/subjects").forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(2));

        schemaRegistry.start();

        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.2")
                .withNetwork(network)
                .withRealmImportFile("keycloak/iot-realm.json")
                .withExposedPorts(9000, 8080)
                .withAdminUsername("admin")
                .withAdminPassword("admin");

        keycloak.start();

        redis = new GenericContainer<>(DockerImageName.parse("redis:8.2.2"))
                .withNetwork(network)
                .withNetworkAliases("redis")
                .withEnv("REDIS_PASSWORD", "redis")
                .withExposedPorts(6379);

        redis.start();
    }


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String kafkaBootstrap = String.format("PLAINTEXT_HOST://%s:%d", kafka.getHost(), kafka.getMappedPort(9094));
        System.out.println("Registering Kafka bootstrap.servers = " + kafkaBootstrap);
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaBootstrap);

        String schemaUrl = "http://" + HOST + ":" + schemaRegistry.getMappedPort(8081);
        System.out.println("Registering Schema Registry URL = " + schemaUrl);
        registry.add("spring.kafka.properties.schema.registry.url", () -> schemaUrl);

        registry.add("spring.kafka.properties.specific.avro.reader", () -> true);

        registry.add("sharding.shards[0].name", () -> "shard0");
        registry.add("sharding.shards[0].master.url", () -> "jdbc:postgresql://" + HOST + ":" + shard0.getMappedPort(5432) + "/devices");
        registry.add("sharding.shards[0].master.username", () -> "postgres");
        registry.add("sharding.shards[0].master.password", () -> "postgres");
        registry.add("sharding.shards[0].replicas[0].url", () -> "jdbc:postgresql://" + HOST + ":" + shard0.getMappedPort(5432) + "/devices");
        registry.add("sharding.shards[0].replicas[0].username", () -> "postgres");
        registry.add("sharding.shards[0].replicas[0].password", () -> "postgres");
        registry.add("sharding.shards[1].name", () -> "shard1");
        registry.add("sharding.shards[1].master.url", () -> "jdbc:postgresql://" + HOST + ":" + shard1.getMappedPort(5432) + "/devices");
        registry.add("sharding.shards[1].master.username", () -> "postgres");
        registry.add("sharding.shards[1].master.password", () -> "postgres");
        registry.add("sharding.shards[1].replicas[0].url", () -> "jdbc:postgresql://" + HOST + ":" + shard1.getMappedPort(5432) + "/devices");
        registry.add("sharding.shards[1].replicas[0].username", () -> "postgres");
        registry.add("sharding.shards[1].replicas[0].password", () -> "postgres");

        String keycloakUrl = "http://" + HOST + ":" + keycloak.getMappedPort(8080) + "/realms/iot";
        registry.add("spring.security.oauth2.resourceserver.jwt.issuer-uri", () -> keycloakUrl);
        registry.add("spring.client.registration.keycloak.provider", () -> "keycloak");
        registry.add("spring.client.registration.keycloak.client-id", () -> "device-service");
        registry.add("spring.client.registration.keycloak.client-secret", () -> "test-secret");
        registry.add("spring.client.registration.keycloak.client-name", () -> "device-service");
        registry.add("spring.client.registration.keycloak.authorization-grant-type", () -> "authorization_code");
        registry.add("spring.client.registration.keycloak.scope", () -> "openid, profile, email");

        registry.add("spring.data.redis.host", () -> HOST);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "redis");
        registry.add("spring.data.redis.timeout", () -> 2000);
        registry.add("spring.data.redis.ttl", () -> 120);
        registry.add("spring.data.redis.lettuce.pool.max-active", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.max-idle", () -> 8);
        registry.add("spring.data.redis.lettuce.pool.min-idle", () -> 2);
        registry.add("spring.data.redis.lettuce.pool.time-between-eviction-runs", () -> 2000);
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        /* ждём, чтобы Cassandra действительно начала принимать запросы */
        Thread.sleep(10_000); // 10 секунд
        System.out.println("Shard 0: " + shard0.getContainerName());
        System.out.println("Shard 1: " + shard1.getContainerName());
        System.out.println("Kafka (external): " + kafka.getContainerName());
        System.out.println("Schema Registry: http://" + HOST + ":" + schemaRegistry.getMappedPort(8081));
        System.out.println("Keycloak: http://" + HOST + ":" + keycloak.getMappedPort(8080));
        System.out.println("Redis: http://" + HOST + ":" + redis.getMappedPort(6379));

        var schema = """
                {
                  "type": "record",
                  "name": "DeviceAvro",
                  "namespace": "com.viktor.avro",
                  "fields": [
                    {"name": "deviceId", "type": { "type": "string", "avro.java.string": "String" }},
                    {"name": "deviceType", "type": { "type": "string", "avro.java.string": "String" }},
                    {"name": "createdAt", "type": "long"},
                    {"name": "meta", "type": { "type": "string", "avro.java.string": "String" }}
                  ]
                }
                """;

        registerSchema("devices-value", schema);
        registerSchema("devices-delete-value", schema);
        createTopic("devices");
        createTopic("devices-delete");
    }

    private static void registerSchema(String subject, String schemaJson) throws Exception {
        String url = "http://" + HOST + ":" + schemaRegistry.getMappedPort(8081)
                     + "/subjects/" + subject + "/versions";

        // используем Jackson для корректной сериализации
        ObjectMapper mapper = new ObjectMapper();
        String payload = mapper.writeValueAsString(Map.of("schema", schemaJson));

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/vnd.schemaregistry.v1+json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int code = connection.getResponseCode();
        if (code != 200 && code != 201) {
            try (var err = connection.getErrorStream()) {
                if (err != null) {
                    System.err.println(new String(err.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
            throw new IllegalStateException("Schema registration failed for " + subject + ", code=" + code);
        }
        System.out.println("Schema registered: " + subject);
    }

    private static void createTopic(String name) {
        try (AdminClient admin = AdminClient.create(
                java.util.Map.of("bootstrap.servers", kafka.getHost() + ":" + kafka.getMappedPort(9094))
        )) {
            admin.createTopics(List.of(
                    new NewTopic(name, 1, (short) 1)
            )).all().get();
        } catch (Exception ignored) {
        }
    }
}
