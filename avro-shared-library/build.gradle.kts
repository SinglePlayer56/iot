plugins {
    `java-library`
    `maven-publish`
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
}

group = "com.viktor"
version = "1.0.1"
description = ""

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
    }
    withJavadocJar()
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.avro:avro:1.12.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters = true
    fieldVisibility.set("PRIVATE")
}

/*
──────────────────────────────────────────────────────
============== Resolve NEXUS credentials ==============
──────────────────────────────────────────────────────
*/

file("../.env").takeIf { it.exists() }?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") && it.contains("=") }
    ?.forEach { line ->
        val (k, v) = line.split("=", limit = 2)
        System.setProperty(k.trim(), v.trim())
        logger.lifecycle("${k.trim()}=${v.trim()}")
    }

val nexusUrl = System.getenv("NEXUS_URL") ?: System.getProperty("NEXUS_URL")
val nexusUser = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME")
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD")

if (nexusUrl.isNullOrBlank() || nexusUser.isNullOrBlank() || nexusPassword.isNullOrBlank()) {
    throw GradleException(
        "NEXUS details are not set. Create a .env file with correct properties: " +
                "NEXUS_URL, NEXUS_USERNAME, NEXUS_PASSWORD"
    )
}

/*
──────────────────────────────────────────────────────
============== Nexus Publishing ==============
──────────────────────────────────────────────────────
*/

publishing {
    publications {
        create<MavenPublication>("avro-shared-library") {
            from(components["java"])
            groupId = "com.viktor"
            artifactId = "avro-shared-library"
            version = "1.0.1"
        }
    }

    repositories {
        maven {
            name = "nexus"
            url = uri(nexusUrl)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUser
                password = nexusPassword
            }
        }
    }
}
