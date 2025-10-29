# Avro Shared Library

**Avro Shared Library** — это общая библиотека для сервисов платформы IoT Monitoring Platform, содержащая Avro-схемы и
автоматически сгенерированные Java-классы для сериализации и десериализации сообщений Kafka.

### Содержание

Библиотека включает в себя Avro-схемы и соответствующие Java-классы:
- DeviceEvent — модель события устройства, публикуемая в Kafka-топик events.
- DeviceId — модель идентификатора устройства, публикуемая в Kafka-топик device-id-topic.

Файлы схем находятся в src/main/avro:

```
DeviceEvent.avsc
DeviceId.avsc
```

###  Использование в других сервисах
**1. Подключение репозитория Nexus**

Добавь Nexus в build.gradle.kts:

```kotlin
repositories {
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
```

**2. Подключение зависимости**
```kotlin
dependencies {
    implementation("com.viktor:avro-shared-library:1.0.0")
}
```

**3. Использование в коде**
```java
import com.viktor.avro.DeviceEvent;
import com.viktor.avro.DeviceId;

DeviceEvent event = DeviceEvent.newBuilder()
    .setEventId("abc-123")
    .setDeviceId("dev-001")
    .setTimestamp(System.currentTimeMillis())
    .setType("TEMPERATURE")
    .setPayload("{\"value\": 23.5}")
    .build();
```

### Сборка и публикация

Для публикации библиотеки необходимо запустить контейнер с Nexus и выполнить команды сборки:
```
make nexus-start
```

Затем выполнить сборку и публикацию артефактов:
```
make build-artifacts
```
После успешной публикации библиотека станет доступна для скачивания из репозитория