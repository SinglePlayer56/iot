val versions = mapOf(
    "avroSerializer" to "7.7.5",
    "flyway" to "11.14.1",
    "jdbcCassandra" to "4.16.0",
    "opentelemetrySpringBootVersion" to "2.6.0",
    "testContainer" to "1.21.3"
)

plugins {
    java
    idea
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.viktor"
version = "1.0.0"
description = ""

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-cassandra-reactive")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.kafka:spring-kafka")

    implementation("io.confluent:kafka-avro-serializer:${versions["avroSerializer"]}")

    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.flywaydb:flyway-core:${versions["flyway"]}")
    implementation("org.flywaydb:flyway-database-cassandra:${versions["flyway"]}")
    implementation("com.ing.data:cassandra-jdbc-wrapper:${versions["jdbcCassandra"]}")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter:${versions["opentelemetrySpringBootVersion"]}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:cassandra:${versions["testContainer"]}")
    testImplementation("org.testcontainers:kafka:${versions["testContainer"]}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters = true
    fieldVisibility.set("PRIVATE")
}
