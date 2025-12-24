import org.gradle.kotlin.dsl.register
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

val versions = mapOf(
    "avroSerializer" to "7.7.5",
    "testContainer" to "1.21.4",
    "logstashVersion" to "7.4",
    "avroSharedLibrary" to "1.0.3",
    "shardingJdbc" to "5.5.2",
    "mapStruct" to "1.6.3",
    "mapStructBinding" to "0.2.0",
    "springdocOpenapiStarterWebmvcUiVersion" to "2.5.0",
    "feignMicrometerVersion" to "13.6",
    "jakartaValidation" to "3.1.1",
    "jacksonBind" to "0.2.6",
    "springCloudStarterOpenfeign" to "4.1.1",
    "testContainerKeycloak" to "4.0.0",
    "swaggerAnnotations" to "2.2.21",
    "testContainerCore" to "2.0.3"
)

plugins {
    java
    idea
    id("org.springframework.boot") version "3.5.6"
    id("io.spring.dependency-management") version "1.1.7"
    id("maven-publish")
    id("org.openapi.generator") version "7.13.0"
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
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2025.0.0")
        mavenBom("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom:2.15.0")
    }
}

configurations.all { resolutionStrategy.cacheChangingModulesFor(0, "seconds") }

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.apache.shardingsphere:shardingsphere-jdbc:${versions["shardingJdbc"]}")

    implementation("org.springframework.kafka:spring-kafka")
    implementation("io.confluent:kafka-avro-serializer:${versions["avroSerializer"]}")
    implementation("com.viktor:avro-shared-library:${versions["avroSharedLibrary"]}")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:${versions["springCloudStarterOpenfeign"]}")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:${versions["springdocOpenapiStarterWebmvcUiVersion"]}")
    implementation("io.swagger.core.v3:swagger-annotations:${versions["swaggerAnnotations"]}")

    implementation("io.micrometer:micrometer-registry-prometheus")
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-observation")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.github.openfeign:feign-micrometer:${versions["feignMicrometerVersion"]}")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter")
    implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstashVersion"]}")

    implementation("jakarta.validation:jakarta.validation-api:${versions["jakartaValidation"]}")
    implementation("org.openapitools:jackson-databind-nullable:${versions["jacksonBind"]}")

    implementation("org.mapstruct:mapstruct:${versions["mapStruct"]}")
    compileOnly("org.projectlombok:lombok")

    annotationProcessor("org.mapstruct:mapstruct-processor:${versions["mapStruct"]}")
    annotationProcessor("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:${versions["mapStructBinding"]}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core")

    testImplementation("org.testcontainers:testcontainers:${versions["testContainerCore"]}")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql:${versions["testContainer"]}")
    testImplementation("org.testcontainers:kafka:${versions["testContainer"]}")
    testImplementation("com.github.dasniko:testcontainers-keycloak:${versions["testContainerKeycloak"]}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

/*
──────────────────────────────────────────────────────
============== Api generation ==============
──────────────────────────────────────────────────────
*/

val openApiDir = file("${rootDir}/openapi")

val foundSpecifications = openApiDir.listFiles { f -> f.extension in listOf("yaml", "yml") } ?: emptyArray()
logger.lifecycle("Found ${foundSpecifications.size} specifications: " + foundSpecifications.joinToString { it.name })

foundSpecifications.forEach { specFile ->
    val ourDir = getAbsolutePath(specFile.nameWithoutExtension)
    val packageName = defineJavaPackageName(specFile.nameWithoutExtension)

    val taskName = buildGenerateApiTaskName(specFile.nameWithoutExtension)
    logger.lifecycle("Register task ${taskName} from ${ourDir.get()}")
    val basePackage = "com.viktor.${packageName}"

    tasks.register(taskName, GenerateTask::class) {
        generatorName.set("spring")
        inputSpec.set(specFile.absolutePath)
        outputDir.set(ourDir)

        configOptions.set(
            mapOf(
                "library" to "spring-cloud",
                "skipDefaultInterface" to "true",
                "useBeanValidation" to "true",
                "openApiNullable" to "false",
                "useFeignClientUrl" to "true",
                "useTags" to "true",
                "useJakartaEe" to "true",
                "apiPackage" to "${basePackage}.api",
                "modelPackage" to "${basePackage}.dto",
                "configPackage" to "${basePackage}.config"
            )
        )

        doFirst {
            logger.lifecycle("$taskName: starting generation from ${specFile.name}")
        }
    }
}


fun getAbsolutePath(nameWithoutExtension: String): Provider<String> {
    return layout.buildDirectory
        .dir("generated-sources/openapi/${nameWithoutExtension}")
        .map { it.asFile.absolutePath }
}

fun defineJavaPackageName(name: String): String {
    val beforeDash = name.substringBefore('-')
    val match = Regex("^[a-z]+]").find(beforeDash)
    return match?.value ?: beforeDash.lowercase()
}

fun buildGenerateApiTaskName(name: String): String {
    return buildTaskName("generate", name)
}

fun buildJarTaskName(name: String): String {
    return buildTaskName("jar", name)
}

fun buildTaskName(taskPrefix: String, name: String): String {
    val prepareName = name
        .split(Regex("[^A-Za-z0-9]"))
        .filter { it.isNotBlank() }
        .joinToString("") { it.replaceFirstChar(Char::uppercase) }

    return "${taskPrefix}-${prepareName}"
}

val withoutExtensionNames = foundSpecifications.map { it.nameWithoutExtension }

sourceSets.named("main") {
    withoutExtensionNames.forEach { name ->
        java.srcDir(layout.buildDirectory.dir("generated-sources/openapi/$name/src/main/java"))
    }
}

tasks.register("generateAllOpenApi") {
    foundSpecifications.forEach { specFile ->
        dependsOn(buildGenerateApiTaskName(specFile.nameWithoutExtension))
    }
    doLast {
        logger.lifecycle("generateAllOpenApi: all specifications has been generated")
    }
}

tasks.named("compileJava") {
    dependsOn("generateAllOpenApi")
}

/*
──────────────────────────────────────────────────────
============== Building jars ==============
──────────────────────────────────────────────────────
*/

tasks.named("build") {
    dependsOn(generatedJars)
}

val generatedJars = foundSpecifications.map { specFile ->
    val name = specFile.nameWithoutExtension
    val generateTaskName = buildGenerateApiTaskName(name)
    val jarTaskName = buildJarTaskName(name)
    val outDirProvider = getAbsolutePath(name)
    val generateSrcDir = outDirProvider.map { File(it).resolve("src/main/java") }

    val sourcesSetName = name

    val sourceSet = sourceSets.create(sourcesSetName) {
        java.srcDir(generateSrcDir)
        compileClasspath += sourceSets["main"].compileClasspath
    }

    val compileTaskName = "compile${sourcesSetName.replaceFirstChar(Char::uppercase)}Java"
    tasks.register<JavaCompile>(compileTaskName) {
        source = sourceSet.java
        classpath = sourceSet.compileClasspath
        destinationDirectory.set(layout.buildDirectory.dir("classes/${sourcesSetName}"))
        dependsOn(generateTaskName)
    }

    tasks.register<Jar>(jarTaskName) {
        group = "build"
        archiveBaseName.set(name)
        destinationDirectory.set(layout.buildDirectory.dir("libs"))

        val classOutput = layout.buildDirectory.dir("classes/${sourcesSetName}")
        from(classOutput)
        dependsOn(compileTaskName)

        doFirst {
            println("Building JAR for $name from compiled classes in ${classOutput.get().asFile}")
        }
    }
}

/*
──────────────────────────────────────────────────────
============== Resolve NEXUS credentials ==============
──────────────────────────────────────────────────────
*/

val nexusUrl = System.getenv("NEXUS_URL") ?: "http://localhost:8085/repository/maven-releases/"
val nexusUser = System.getenv("NEXUS_USERNAME") ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: "admin"

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
        foundSpecifications.forEach { specFile ->
            val name = specFile.nameWithoutExtension
            val jarBaseName = name
            var jarFile = file("build/libs")
                .listFiles()
                ?.firstOrNull { it.name.contains(name) && (it.extension == "jar" || it.extension == "zip") }

            if (jarFile != null) {
                logger.lifecycle("publishing: ${jarFile.name}")

                create<MavenPublication>("publish${name.replaceFirstChar(Char::uppercase)}Jar") {
                    artifact(jarFile)
                    groupId = "com.viktor"
                    artifactId = jarBaseName
                    version = "1.0.0"

                    pom {
                        this.name.set("Generated API $jarBaseName")
                        this.description.set("OpenAPI generated code for $jarBaseName")
                    }
                }
            }
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

afterEvaluate {
    val jarTask = tasks.named("jar-DeviceServiceApi")

    val publishTask = tasks.named(
        "publishPublishDevice-service-apiJarPublicationToNexusRepository"
    )

    publishTask.configure {
        dependsOn(jarTask)
        mustRunAfter(jarTask)
        inputs.files(jarTask.get().outputs.files)
    }
}