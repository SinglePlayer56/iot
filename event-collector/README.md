# Event Collector Service

Микросервис **Event Collector Service**:
- подписывается на Kafka-топик `events`
- получает события в формате Avro (валидация через Schema Registry)
- сохраняет события в Apache Cassandra для аналитики
- публикует уникальные `device_id` в отдельный Kafka-топик `device-id-topic`

---

## Avro-схемы

### `events-value` (для топика `events`)

```json
{
  "type": "record",
  "name": "DeviceEvent",
  "namespace": "com.viktor.avro",
  "fields": [
    { "name": "eventId", "type": { "type": "string", "avro.java.string": "String" } },
    { "name": "deviceId", "type": { "type": "string", "avro.java.string": "String" } },
    { "name": "timestamp", "type": "long" },
    { "name": "type", "type":{ "type": "string", "avro.java.string": "String" } },
    { "name": "payload", "type": { "type": "string", "avro.java.string": "String" } }
  ]
}
```

### `device-id-topic-value` (для топика `device-id-topic`)

```json
{
  "type": "record",
  "name": "DeviceId",
  "namespace": "com.viktor.avro",
  "fields": [
    { "name": "deviceId", "type": { "type": "string", "avro.java.string": "String" } }
  ]
}
```

> ⚠️ **Важно:** Avro-схемы необходимо добавить вручную в Schema Registry через Kafka UI (http://localhost:8070)
> 1. Открой `Schema Registry`
> 2. Добавь схему для `events-value`
> 3. Добавь схему для `device-id-topic-value`
> 4. После этого можно публиковать сообщения в топик `events`.

---

## Локальный запуск


1.  Перейди в корень репозитория:
```bash
  cd ../
```

2.  Убедись, что установлены:
    - Docker
    - Docker Compose

3.  Используй команды `make` для управления сервисами:

| Команда                                       | Назначение                                            |
|-----------------------------------------------|-------------------------------------------------------|
| `make event-collector-start-build`            | Первый запуск — собрать образ и запустить приложение  |
| `make event-collector-start`                  | Повторный запуск без пересборки                       |
| `make event-collector-stop`                   | Остановить контейнеры                                 |
| `make event-collector-stop-remove`            | Остановить контейнеры и удалить volume                |
| `make event-collector-monitoring-start-build` | Первый запуск с системой мониторинга                  |
| `make event-collector-monitoring-start`       | Запуск с мониторингом без пересборки                  |
| `make event-collector-monitoring-stop`        | Остановить приложение с мониторингом                  |
| `make event-collector-monitoring-stop-remove` | Остановить приложение с мониторингом и удалить volume |

---

## Пример отправки события в топик `events`

```json
{
  "eventId": "event-123",
  "deviceId": "device-001",
  "timestamp": 1698146400000,
  "type": "ALERT",
  "payload": "{\"status\":\"OK\"}"
}
```

---

## Мониторинг и метрики

- `/actuator/health` — проверка здоровья сервиса
- `/actuator/info` — общая информация о сервисе
- `/actuator/metrics` — базовые метрики Spring Boot
- `/actuator/prometheus` — метрики для Prometheus
- Grafana доступна по адресу [http://localhost:3000](http://localhost:3000) (дашборд Event Collector)

---

## Тестирование

```bash
  ./gradlew test
```

- Для интеграционных тестов используется **Testcontainers** (Kafka, Cassandra, Schema Registry)
- Покрытие тестов проверяется с помощью **JUnit 5** и **Mockito**

---

## Переменные окружения

| Переменная                          | Описание                                                 | Пример                                            |
|--------------------------------------|-----------------------------------------------------------|---------------------------------------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT`       | Адрес OTLP-экспортера                                    | `http://alloy:4318`                              |
| `OTEL_EXPORTER_OTLP_PROTOCOL`       | Протокол для OTLP                                        | `http/protobuf`                                  |
| `OTEL_METRICS_EXPORTER`            | Экспорт метрик                                          | `none`                                           |
| `OTEL_TRACES_EXPORTER`             | Экспорт трейсов                                         | `otlp`                                           |
| `OTEL_LOGS_EXPORTER`               | Экспорт логов                                           | `none`                                           |
| `OTEL_RESOURCE_ATTRIBUTES`         | Атрибуты ресурса OTEL (версия и окружение деплоя)         | `service.version=1.0.0,deployment.environment=docker` |
| `KAFKA_ADDRESS`                    | Адрес брокера Kafka                                      | `kafka:9092`                                     |
| `SCHEMA_REGISTRY_ADDRESS`          | Адрес Schema Registry                                    | `schema-registry:8081`                           |
| `CASSANDRA_HOST`                   | Адрес хоста Cassandra                                    | `cassandra`                                      |
| `CASSANDRA_PORT`                   | Порт Cassandra                                          | `9042`                                          |

---

## Дополнительно
- Sequence диаграмма [sequence.puml](./diagrams/sequence.puml)
- Компонентная диаграмма [component.puml](./diagrams/component.puml)
