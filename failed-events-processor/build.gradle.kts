val versions = mapOf(
    "avroSerializer" to "7.7.5",
    "testContainer" to "1.21.3",
    "logstashVersion" to "7.4",
    "avroSharedLibrary" to "1.0.3",
    "mapStruct" to "1.6.3",
    "mapStructBinding" to "0.2.0",
    "minio" to "8.6.0"
)

plugins {
	java
    idea
	id("org.springframework.boot") version "3.5.6"
	id("io.spring.dependency-management") version "1.1.7"
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
    maven {
        val nexusUrl = System.getenv("NEXUS_URL") ?: "http://localhost:8085/repository/maven-releases/"
        val nexusUser = System.getenv("NEXUS_USERNAME") ?: "admin"
        val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: "admin"

        url = uri(nexusUrl)
        isAllowInsecureProtocol = true

        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
}

dependencyManagement {
    imports {
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:${versions["avroSerializer"]}")
    implementation("com.viktor:avro-shared-library:${versions["avroSharedLibrary"]}")

    implementation("io.minio:minio:${versions["minio"]}")

    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstashVersion"]}")

    implementation("org.mapstruct:mapstruct:${versions["mapStruct"]}")
    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapStruct"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${versions["mapStructBinding"]}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.projectreactor:reactor-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql:${versions["testContainer"]}")
    testImplementation("org.testcontainers:kafka:${versions["testContainer"]}")
}

tasks.withType<Test> {
	useJUnitPlatform()
}
