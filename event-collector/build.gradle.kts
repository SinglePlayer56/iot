val versions = mapOf(
    "avroSerializer" to "7.7.5",
    "schemaRegistryClient" to "7.7.5",
    "flyway" to "11.14.1",
    "jdbcCassandra" to "4.16.0",
    "guava" to "33.5.0"
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

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters = true
    fieldVisibility.set("PRIVATE")
}
