# Failed Events Processor Service

Микросервис **Failed Events Processor Service**:
- подписывается на Kafka-топик `dlt-topic` для обработки сообщений
- получает события в формате Avro (валидация через Schema Registry)
- сохраняет ошибки в хранилище MinIo

---

## Avro-схемы

### `dlt-topic-value` (для топика `dlt-topic`)
```avroschema
{
  "type": "record",
  "name": "DltError",
  "namespace": "com.viktor.avro",
  "fields": [
    {"name": "reason", "type": {"type": "string", "avro.java.string": "String" }},
    {"name": "errorSource", "type": {"type": "string", "avro.java.string": "String" }},
    {"name": "stackTrace", "type": {"type": "string", "avro.java.string": "String" }},
    {"name": "traceId", "type": {"type": "string", "avro.java.string": "String" }},
    {"name": "receivedAt", "type": "long"}
  ]
}
```

> ⚠️ **Важно:** Avro-схемы необходимо добавить вручную в Schema Registry через Kafka UI (http://localhost:8070)
> 1. Открой `Schema Registry`
> 2. Добавь схему для `dlt-topic-value`
> 3. После этого можно публиковать сообщения в топик `dlt-topic`.

---

## Локальный запуск


1.  Перейди в корень репозитория:
```bash
  cd ../
```

2.  Убедись, что установлены:
    - Docker
    - Docker Compose

3. Зависимости:
    - Запущен контейнер nexus `make nexus-start`
    - В Nexus опубликованы артефакты `make build-shared-artifacts`

3.  Используй команды `make` для управления сервисами:

| Команда                                               | Назначение                                            |
|-------------------------------------------------------|-------------------------------------------------------|
| `make failed-events-processor-start-build`            | Первый запуск — собрать образ и запустить приложение  |
| `make failed-events-processor-start`                  | Повторный запуск без пересборки                       |
| `make failed-events-processor-stop`                   | Остановить контейнеры                                 |
| `make failed-events-processor-stop-remove`            | Остановить контейнеры и удалить volume                |
| `make failed-events-processor-monitoring-start-build` | Первый запуск с системой мониторинга                  |
| `make failed-events-processor-monitoring-start`       | Запуск с мониторингом без пересборки                  |
| `make failed-events-processor-monitoring-stop`        | Остановить приложение с мониторингом                  |
| `make failed-events-processor-monitoring-stop-remove` | Остановить приложение с мониторингом и удалить volume |

---

## Пример отправки сообщения в топик `dlt-topic`

```json
{
  "reason": "NullPointerException occurred while processing event",
  "errorSource": "device-service",
  "stackTrace": "java.lang.NullPointerException: Cannot invoke \"Object.toString()\" because \"obj\" is null\n\tat com.viktor.device.Service.process(Service.java:42)\n\tat com.viktor.device.Controller.handle(Controller.java:21)",
  "traceId": "b87ff8f7-6b93-4eea-9e68-ced75f252467",
  "receivedAt": 1733356800000
}
```

---

## Мониторинг и метрики

- `/actuator/health` — проверка здоровья сервиса
- `/actuator/info` — общая информация о сервисе
- `/actuator/metrics` — базовые метрики Spring Boot
- `/actuator/prometheus` — метрики для Prometheus
- Grafana доступна по адресу [http://localhost:3000](http://localhost:3000) (дашборд Failed Events Processor)

---

## Тестирование
Тесты запускается автоматически при сборке контейнера.\
Используйте команды с постфиксом *-build
- `make failed-events-processor-start-build`
- `make failed-events-processor-monitoring-start-build`

Для запуска тестов в локальной среде.
```bash
  ./gradlew test
```

- Для интеграционных тестов используется **Testcontainers** (Kafka, PostgreSQL, Schema Registry)
- Покрытие тестов проверяется с помощью **JUnit 5** и **Mockito**

---

## Переменные окружения

| Переменная                    | Описание                                                      | Пример                                                |
|-------------------------------|---------------------------------------------------------------|-------------------------------------------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT` | Адрес OTLP-экспортера                                         | `http://alloy:4318`                                   |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | Протокол для OTLP                                             | `http/protobuf`                                       |
| `OTEL_METRICS_EXPORTER`       | Экспорт метрик                                                | `none`                                                |
| `OTEL_TRACES_EXPORTER`        | Экспорт трейсов                                               | `otlp`                                                |
| `OTEL_LOGS_EXPORTER`          | Экспорт логов                                                 | `none`                                                |
| `OTEL_RESOURCE_ATTRIBUTES`    | Атрибуты ресурса OTEL (версия и окружение деплоя)             | `service.version=1.0.0,deployment.environment=docker` |
| `KAFKA_ADDRESS`               | Адрес брокера Kafka                                           | `kafka:9092`                                          |
| `SCHEMA_REGISTRY_ADDRESS`     | Адрес Schema Registry                                         | `schema-registry:8081`                                |
| `MINIO_HOST`                  | Хост подключения к MinIo                                      | 'http://minio:9000'                                   |
| `MINIO_ROOT_USER`             | Имя пользователя MinIo                                        | 'minio1234'                                           |
| `MINIO_ROOT_PASSWORD`         | Пароль MinIo                                                  | 'minio1234'                                           |
| `MINIO_BUCKET_NAME`           | Бакет, создаваемый при старте приложения (если не существует) | 'failed-events'                                       |

---

## Дополнительно
- Sequence диаграмма [sequence.puml](./diagrams/sequence.puml)
- Компонентная диаграмма [component.puml](./diagrams/component.puml)
