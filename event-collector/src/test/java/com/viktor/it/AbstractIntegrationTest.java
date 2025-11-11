package com.viktor.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

public abstract class AbstractIntegrationTest {

    private static final Network network;

    private static final CassandraContainer<?> cassandra;
    private static final GenericContainer<?> kafka;
    private static final GenericContainer<?> schemaRegistry;

    public static final String HOST = Objects.nonNull(System.getenv("TESTCONTAINERS_HOST_OVERRIDE")) ?
            System.getenv("TESTCONTAINERS_HOST_OVERRIDE") : "localhost";

    static {
        network = Network.newNetwork();

        cassandra = new CassandraContainer<>("cassandra:5.0")
                .withNetwork(network)
                .withInitScript("schema.cql")
                .withExposedPorts(9042)
                .waitingFor(Wait.forLogMessage(".*Startup complete.*", 1))
                .withStartupTimeout(Duration.ofMinutes(3));

        cassandra.start();

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
    }


    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        String host = cassandra.getHost();
        int port = cassandra.getMappedPort(9042);
        System.out.printf("🔧 Registering Cassandra props: %s:%d%n", host, port);
        registry.add("spring.cassandra.contact-points", cassandra::getHost);
        registry.add("spring.cassandra.port", () -> cassandra.getMappedPort(9042));
        registry.add("spring.cassandra.local-datacenter", () -> "datacenter1");
        registry.add("spring.cassandra.keyspace-name", () -> "iot_events");
        registry.add("spring.cassandra.schema-action", () -> "none");

        String kafkaBootstrap = String.format("PLAINTEXT_HOST://%s:%d", kafka.getHost(), kafka.getMappedPort(9094));
        System.out.println("Registering Kafka bootstrap.servers = " + kafkaBootstrap);
        registry.add("spring.kafka.bootstrap-servers", () -> kafkaBootstrap);

        String schemaUrl = "http://" + HOST + ":" + schemaRegistry.getMappedPort(8081);
        System.out.println("Registering Schema Registry URL = " + schemaUrl);
        registry.add("spring.kafka.properties.schema.registry.url", () -> schemaUrl);

        registry.add("spring.kafka.properties.specific.avro.reader", () -> true);
    }

    @BeforeAll
    static void beforeAll() throws Exception {
        /* ждём, чтобы Cassandra действительно начала принимать запросы */
        Thread.sleep(10_000); // 10 секунд
        System.out.println("Cassandra: " + cassandra.getContactPoint());
        System.out.println("Kafka (external): " + kafka.getContainerName());
        System.out.println("Schema Registry: http://" + HOST + ":" + schemaRegistry.getMappedPort(8081));

        registerSchema("events-value", """
                {
                   "type": "record",
                   "name": "DeviceEvent",
                   "namespace": "com.viktor.avro",
                   "fields": [
                     { "name": "eventId", "type": { "type": "string", "avro.java.string": "String" } },
                     { "name": "device", "type": {
                         "type": "record",
                         "name": "Device",
                         "namespace": "com.viktor.avro",
                         "fields": [
                           {"name": "deviceId", "type": { "type": "string", "avro.java.string": "String" }},
                           {"name": "deviceType", "type": { "type": "string", "avro.java.string": "String" }},
                           {"name": "createdAt", "type": "long"},
                           {"name": "meta", "type": { "type": "string", "avro.java.string": "String" }}
                         ]
                       }
                     },
                     { "name": "timestamp", "type": "long" },
                     { "name": "type", "type":{ "type": "string", "avro.java.string": "String" } },
                     { "name": "payload", "type": { "type": "string", "avro.java.string": "String" } }
                   ]
                 }
                """);

        registerSchema("devices-value", """
                {
                   "type": "record",
                   "name": "Device",
                   "namespace": "com.viktor.avro",
                   "fields": [
                     {"name": "deviceId", "type": { "type": "string", "avro.java.string": "String" }},
                     {"name": "deviceType", "type": { "type": "string", "avro.java.string": "String" }},
                     {"name": "createdAt", "type": "long"},
                     {"name": "meta", "type": { "type": "string", "avro.java.string": "String" }}
                   ]
                 }
                """);
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
}
