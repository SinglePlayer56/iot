# Device Service

Микросервис **Device Service**:

- выполняет CRUD операции с устройствами
- хранит актуальные данные в Redis Cache
- публикует сообщения в топик `devices` для добавления и обновления устройств
- публикует сообщения в топик `devices-delete` для удаления устройств

---

## Avro-схемы

### `devices-value`/ `devices-delete-value` (для топиков `devices`, `devices-delete`)

```avroschema
{
  "type": "record",
  "name": "DeviceAvro",
  "namespace": "com.viktor.avro",
  "fields": [
    {
      "name": "deviceId",
      "type": {
        "type": "string",
        "avro.java.string": "String"
      }
    },
    {
      "name": "deviceType",
      "type": {
        "type": "string",
        "avro.java.string": "String"
      }
    },
    {
      "name": "createdAt",
      "type": "long"
    },
    {
      "name": "meta",
      "type": {
        "type": "string",
        "avro.java.string": "String"
      }
    }
  ]
}
```

> ⚠️ **Важно:** Avro-схемы необходимо добавить вручную в Schema Registry через Kafka UI (http://localhost:8070)
> 1. Открой `Schema Registry`
> 2. Добавь схему для `devices-value`
> 3. Добавь схему для `devices-delete-value`
> 4. После этого можно публиковать сообщения в топик `devices`, `devices-delete`.

---

## Локальный запуск

1. Перейди в корень репозитория:

```bash
  cd ../
```

2. Убедись, что установлены:
    - Docker
    - Docker Compose

3. Зависимости:
    - Запущен контейнер nexus `make nexus-start`
    - В Nexus опубликованы артефакты `make build-shared-artifacts`
    - Запущен `make security-start`
    - Запущен `make device-collector-start`

4. Настройка ключей для авторизации (Keycloak)
    
   После запуска `make security-start` необходимо выполнить настройку авторизации в Keycloak.

   1  Авторизуйся в Keycloak с параметрами
       KC_BOOTSTRAP_ADMIN_USERNAME / KC_BOOTSTRAP_ADMIN_PASSWORD

   2  Перейди в realm iot

   3  Открой клиент device-service

   4  Сгенерируй новый client secret

   5  Добавь его в .env в переменную KC_DS_CLIENT_SECRET

   6  Для тестирования запросов создай пользователя в Users и выдай ему роль:
        - device-service.admin или
        - device-service.user

5. Используй команды `make` для управления сервисами:

| Команда                                      | Назначение                                            |
|----------------------------------------------|-------------------------------------------------------|
| `make device-service-start-build`            | Первый запуск — собрать образ и запустить приложение  |
| `make device-service-start`                  | Повторный запуск без пересборки                       |
| `make device-service-stop`                   | Остановить контейнеры                                 |
| `make device-service-stop-remove`            | Остановить контейнеры и удалить volume                |
| `make device-service-monitoring-start-build` | Первый запуск с системой мониторинга                  |
| `make device-service-monitoring-start`       | Запуск с мониторингом без пересборки                  |
| `make device-service-monitoring-stop`        | Остановить приложение с мониторингом                  |
| `make device-service-monitoring-stop-remove` | Остановить приложение с мониторингом и удалить volume |

---

## Пример отправки сообщения в топики `devices` и `devices-delete`

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
- Grafana доступна по адресу [http://localhost:3000](http://localhost:3000) (дашборд Device Service)

---

## Тестирование

Тесты запускается автоматически при сборке контейнера.\
Используйте команды с постфиксом *-build

- `make device-service-start-build`
- `make device-service-monitoring-start-build`

Для запуска тестов в локальной среде.

```bash
  ./gradlew test
```

- Для интеграционных тестов используется **Testcontainers** (Kafka, PostgreSQL, Schema Registry)
- Покрытие тестов проверяется с помощью **JUnit 5** и **Mockito**

---

## Переменные окружения

| Переменная                                  | Описание                     | Пример                                                        |
|---------------------------------------------|------------------------------|---------------------------------------------------------------|
| `OTEL_EXPORTER_OTLP_ENDPOINT`               | Адрес OTLP-экспортера        | http://alloy:4318                                             |
| `OTEL_EXPORTER_OTLP_PROTOCOL`               | Протокол OTLP                | http/protobuf                                                 |
| `OTEL_METRICS_EXPORTER`                     | Экспорт метрик               | none                                                          |
| `OTEL_TRACES_EXPORTER`                      | Экспорт трейсов              | otlp                                                          |
| `OTEL_LOGS_EXPORTER`                        | Экспорт логов                | none                                                          |
| `OTEL_RESOURCE_ATTRIBUTES`                  | Атрибуты ресурса OTEL        | service.version=1.0.0,deployment.environment=docker           |
| `KAFKA_ADDRESS`                             | Адрес брокера Kafka          | kafka:9092                                                    |
| `SCHEMA_REGISTRY_ADDRESS`                   | Адрес Schema Registry        | schema-registry:8081                                          |
| `POSTGRES_DEVICES_DB_NAME`                  | Имя базы данных устройств    | devices                                                       |
| `SHARD_0_MASTER_URL`                        | Адрес мастера шарда 0        | postgres-devices-shard-0-master:5432                          |
| `SHARD_0_REPLICA_URL`                       | Адрес реплики шарда 0        | postgres-devices-shard-0-replica:5432                         |
| `SHARD_1_MASTER_URL`                        | Адрес мастера шарда 1        | postgres-devices-shard-1-master:5432                          |
| `SHARD_1_REPLICA_URL`                       | Адрес реплики шарда 1        | postgres-devices-shard-1-replica:5432                         |
| `POSTGRES_DEVICES_USER_SHARD_0_MASTER`      | Пользователь мастера шарда 0 | postgres                                                      |
| `POSTGRES_DEVICES_PASSWORD_SHARD_0_MASTER`  | Пароль мастера шарда 0       | postgres                                                      |
| `POSTGRES_DEVICES_USER_SHARD_0_REPLICA`     | Пользователь реплики шарда 0 | postgres                                                      |
| `POSTGRES_DEVICES_PASSWORD_SHARD_0_REPLICA` | Пароль реплики шарда 0       | postgres                                                      |
| `POSTGRES_DEVICES_USER_SHARD_1_MASTER`      | Пользователь мастера шарда 1 | postgres                                                      |
| `POSTGRES_DEVICES_PASSWORD_SHARD_1_MASTER`  | Пароль мастера шарда 1       | postgres                                                      |
| `POSTGRES_DEVICES_USER_SHARD_1_REPLICA`     | Пользователь реплики шарда 1 | postgres                                                      |
| `POSTGRES_DEVICES_PASSWORD_SHARD_1_REPLICA` | Пароль реплики шарда 1       | postgres                                                      |
| `KC_DS_CLIENT_ID`                           | Client ID Keycloak           | device-service                                                |
| `KC_DS_CLIENT_SECRET`                       | Client Secret Keycloak       | ******                                                        |
| `KEYCLOAK_ISS_URL`                          | URL Issuer Keycloak          | http://localhost:8080/realms/iot                              |
| `KEYCLOAK_JWK_URL`                          | URL JWK Keycloak             | http://keycloak:8080/realms/iot/protocol/openid-connect/certs |
| `REDIS_HOST`                                | Адрес Redis                  | redis                                                         |
| `REDIS_PORT`                                | Порт Redis                   | 6379                                                          |
| `REDIS_PASSWORD`                            | Пароль Redis                 | redis                                                         |

---

## Дополнительно

- Sequence диаграмма [sequence.puml](./diagrams/sequence.puml)
- Компонентная диаграмма [component.puml](./diagrams/component.puml)
