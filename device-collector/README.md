# Device Collector Service

Микросервис **Device Collector Service**:
- подписывается на Kafka-топик `devices`
- получает события в формате Avro (валидация через Schema Registry)
- сохраняет/обновляет устройство в PostgreSQL кластере
- публикует необработанные сообщения в `devices.DLT` топик

---

## Avro-схемы

### `devices-value`/`devices.DLT-value` (для топиков `devices` и `devices.DLT`)

```avroschema
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
```

> ⚠️ **Важно:** Avro-схемы необходимо добавить вручную в Schema Registry через Kafka UI (http://localhost:8070)
> 1. Открой `Schema Registry`
> 2. Добавь схему для `devices-value`
> 3. Добавь схему для `devices.DLT-value`
> 4. После этого можно публиковать сообщения в топик `devices`.

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
    - В Nexus опубликованы артефакты `make build-artifacts`

3.  Используй команды `make` для управления сервисами:

| Команда                                   | Назначение                                            |
|-------------------------------------------|-------------------------------------------------------|
| `make device-collector-start-build`       | Первый запуск — собрать образ и запустить приложение  |
| `make device-collector-start`             | Повторный запуск без пересборки                       |
| `make device-collector-stop`              | Остановить контейнеры                                 |
| `make device-collector-stop-remove`       | Остановить контейнеры и удалить volume                |
| `make device-collector-monitoring-start-build` | Первый запуск с системой мониторинга                  |
| `make device-collector-monitoring-start`  | Запуск с мониторингом без пересборки                  |
| `make device-collector-monitoring-stop`   | Остановить приложение с мониторингом                  |
| `make device-collector-monitoring-stop-remove` | Остановить приложение с мониторингом и удалить volume |

---

## Пример отправки события в топик `devices`

```json
{
  "deviceId": "123e4567-e89b-12d3-a456-426614174000",
  "deviceType": "temperature-sensor",
  "createdAt": 1730613592000,
  "meta": "{\"location\": \"Berlin\", \"firmware\": \"v2.1.4\", \"battery\": 87}"
}
```

---

## Мониторинг и метрики

- `/actuator/health` — проверка здоровья сервиса
- `/actuator/info` — общая информация о сервисе
- `/actuator/metrics` — базовые метрики Spring Boot
- `/actuator/prometheus` — метрики для Prometheus
- Grafana доступна по адресу [http://localhost:3000](http://localhost:3000) (дашборд Device Collector)

---

## Тестирование
Тесты запускается автоматически при сборке контейнера.\
Используйте команды с постфиксом *-build
- `make device-collector-start-build`
- `make device-collector-monitoring-start-build`

Для запуска тестов в локальной среде.
```bash
  ./gradlew test
```

- Для интеграционных тестов используется **Testcontainers** (Kafka, PostgreSQL, Schema Registry)
- Покрытие тестов проверяется с помощью **JUnit 5** и **Mockito**

---

## Переменные окружения

| Переменная                                  | Описание                                          | Пример                                                |
|---------------------------------------------|---------------------------------------------------|-------------------------------------------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT`               | Адрес OTLP-экспортера                             | `http://alloy:4318`                                   |
| `OTEL_EXPORTER_OTLP_PROTOCOL`               | Протокол для OTLP                                 | `http/protobuf`                                       |
| `OTEL_METRICS_EXPORTER`                     | Экспорт метрик                                    | `none`                                                |
| `OTEL_TRACES_EXPORTER`                      | Экспорт трейсов                                   | `otlp`                                                |
| `OTEL_LOGS_EXPORTER`                        | Экспорт логов                                     | `none`                                                |
| `OTEL_RESOURCE_ATTRIBUTES`                  | Атрибуты ресурса OTEL (версия и окружение деплоя) | `service.version=1.0.0,deployment.environment=docker` |
| `KAFKA_ADDRESS`                             | Адрес брокера Kafka                               | `kafka:9092`                                          |
| `SCHEMA_REGISTRY_ADDRESS`                   | Адрес Schema Registry                             | `schema-registry:8081`                                |
| `POSTGRES_DEVICES_DB_NAME`                  | Имя базы данных устройств                         | devices                                               |
| `SHARD_0_MASTER_URL`                        | Адрес мастера шарда 0                             | postgres-devices-shard-0-master:5432                  |
| `SHARD_0_REPLICA_URL`                       | Адрес реплики шарда 0                             | postgres-devices-shard-0-replica:5432                 |
| `SHARD_1_MASTER_URL`                        | Адрес мастера шарда 1                             | postgres-devices-shard-1-master:5432                  |
| `SHARD_0_REPLICA_URL`                       | Адрес реплики шарда 1                             | postgres-devices-shard-1-replica:5432                 |
| `POSTGRES_DEVICES_USER_SHARD_0_MASTER`      | Пользователь мастера шарда 0                      | postgres                                              |
| `POSTGRES_DEVICES_PASSWORD_SHARD_0_MASTER`  | Пароль мастера шарда 0                            | postgres                                              |
| `POSTGRES_DEVICES_USER_SHARD_0_REPLICA`     | Пользователь реплики шарда 0                      | postgres                                              |
| `POSTGRES_DEVICES_PASSWORD_SHARD_0_REPLICA` | Пароль реплики шарда 0                            | postgres                                              |
| `POSTGRES_DEVICES_USER_SHARD_1_MASTER`      | Пользователь мастера шарда 1                      | postgres                                              |
| `POSTGRES_DEVICES_PASSWORD_SHARD_1_MASTER`  | Пароль мастера шарда 1                            | postgres                                              |
| `POSTGRES_DEVICES_USER_SHARD_1_REPLICA`     | Пользователь реплики шарда 01                     | postgres                                              |
| `POSTGRES_DEVICES_PASSWORD_SHARD_1_REPLICA` | Пароль реплики шарда 1                            | postgres                                              |

---

## Дополнительно
- Sequence диаграмма [sequence.puml](./diagrams/sequence.puml)
- Компонентная диаграмма [component.puml](./diagrams/component.puml)
