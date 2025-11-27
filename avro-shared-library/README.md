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
Device.avsc
```

#### DeviceEvent
```avroschema
{
  "type": "record",
  "name": "DeviceEventAvro",
  "namespace": "com.viktor.avro",
  "fields": [
    { "name": "eventId", "type": { "type": "string", "avro.java.string": "String" } },
    { "name": "device", "type": {
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
    },
    { "name": "timestamp", "type": "long" },
    { "name": "type", "type":{ "type": "string", "avro.java.string": "String" } },
    { "name": "payload", "type": { "type": "string", "avro.java.string": "String" } }
  ]
}
```

#### Device
```avroschema
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
    implementation("com.viktor:avro-shared-library:1.0.1")
}
```

**3. Использование в коде**
```
import com.viktor.avro.DeviceEvent;
import com.viktor.avro.Device;

Device device = new Device();
device.setDeviceId("dev-1");
device.setDeviceType("type");
device.setCreatedAt(System.currentTimeMillis());
device.setMeta("meta");

DeviceEvent event = new DeviceEvent();
event.setEventId("abc-123");
event.setDevice(device);
event.setTimestamp(System.currentTimeMillis());
event.setType("TEMPERATURE");
event.setPayload("{\"value\": 23.5}");
```

### Сборка и публикация

Для публикации библиотеки необходимо запустить контейнер с Nexus и выполнить команды сборки:
```
make nexus-start
```

Затем выполнить сборку и публикацию артефактов:
```
make build-shared-artifacts
```
После успешной публикации библиотека станет доступна для скачивания из репозитория