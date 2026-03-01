# Router Manager

Микросервис **Router Manager**:

- принимает gRPC-команды для роутеров (`REBOOT`, `PING` и т.д.)
- сохраняет команды в PostgreSQL
- поддерживает отправку команды конкретному роутеру и broadcast всем роутерам
- отдает роутерам команды через `Poll` и меняет статусы `PENDING -> SENT -> ACKED/FAILED`
- запускает фонового воркера, который переводит просроченные `SENT` команды в `FAILED` по TTL

---

## Proto-схемы

### `send-command.proto` (gRPC API)

```protobuf
syntax = "proto3";

package api.v1;
option go_package = "router-manager/internal/gen/proto/api/v1;apiv1";

import "google/protobuf/struct.proto";
import "google/protobuf/empty.proto";

service RouterManager {
  rpc SendCommand (SendCommandRequest) returns (google.protobuf.Empty);
  rpc Poll (PollRequest) returns (PollResponse);
  rpc Ack (AckRequest) returns (google.protobuf.Empty);
}

message SendCommandRequest {
  optional string router_id = 1;
  string command_type = 2;
  google.protobuf.Struct payload = 3;
}

message PollRequest {
  string router_id = 1;
}

message Command {
  string command_id = 1;
  string command_type = 2;
  google.protobuf.Struct payload = 3;
}

message PollResponse {
  repeated Command commands = 1;
}

message AckRequest {
  string router_id = 1;
  string command_id = 2;
}
```

---

## Жизненный цикл команды

```
PENDING -> SENT -> ACKED/FAILED (по TTL)
```

| Статус    | Описание                                                              |
|-----------|-----------------------------------------------------------------------|
| `PENDING` | Команда создана и ожидает выдачи роутеру                              |
| `SENT`    | Команда была выдана роутеру через `Poll`                              |
| `ACKED`   | Роутер подтвердил выполнение через `Ack`                              |
| `FAILED`  | Команда осталась в `SENT` дольше `commands.ttl` и переведена воркером |

> ℹ️ `Poll` возвращает команды со статусами `PENDING` и `SENT`.

---

## Локальный запуск

1. Перейди в корень репозитория:

```bash
  cd ../
```

2. Убедись, что установлены:
    - Docker
    - Docker Compose

3. Используй команды `make` для управления сервисом:

| Команда                           | Назначение                                                       |
|-----------------------------------|------------------------------------------------------------------|
| `make router-manager-start-build` | Первый запуск `router-manager`                                   |
| `make router-manager-start`       | Повторный запуск без пересборки                                  |
| `make router-manager-stop`        | Остановить `router-manager` и его PostgreSQL без удаления volume |
| `make router-manager-stop-remove` | Остановить `router-manager` и его PostgreSQL с удалением volume  |

4. Добавь тестовый роутер (иначе `Poll`/`Ack` вернут ошибку `router not found`):

```bash
docker exec -i postgres-router-manager psql -U postgres -d router_manager -c \
"INSERT INTO routers (id, serial_number) VALUES ('11111111-1111-1111-1111-111111111111', 'SN-000001') ON CONFLICT (id) DO NOTHING;"
```

## Примеры вызова gRPC (grpcurl)

> ℹ️ Используй `-proto proto/api/v1/send-command.proto`.

### Отправить команду конкретному роутеру

```bash
grpcurl -plaintext \
  -proto proto/api/v1/send-command.proto \
  -d '{"router_id":"11111111-1111-1111-1111-111111111111","command_type":"REBOOT","payload":{"reason":"scheduled"}}' \
  localhost:44044 api.v1.RouterManager/SendCommand
```

### Отправить broadcast-команду всем роутерам

```bash
grpcurl -plaintext \
  -proto proto/api/v1/send-command.proto \
  -d '{"command_type":"PING","payload":{}}' \
  localhost:44044 api.v1.RouterManager/SendCommand
```

### Получить команды (`Poll`)

```bash
grpcurl -plaintext \
  -proto proto/api/v1/send-command.proto \
  -d '{"router_id":"11111111-1111-1111-1111-111111111111"}' \
  localhost:44044 api.v1.RouterManager/Poll
```

### Подтвердить выполнение (`Ack`)

```bash
grpcurl -plaintext \
  -proto proto/api/v1/send-command.proto \
  -d '{"router_id":"11111111-1111-1111-1111-111111111111","command_id":"<command_id из Poll>"}' \
  localhost:44044 api.v1.RouterManager/Ack
```

---

## Тестирование

Тесты запускается автоматически при сборке контейнера.\
Используйте команды с постфиксом *-build

- `make router-manager-start-build`

Для запуска тестов в локальной среде.

```bash
go test -v ./tests/...
```

- Для интеграционных тестов используется **Testcontainers** (PostgreSQL)

---

## Переменные окружения

| Переменная     | Описание                     | Пример                                                                       |
|----------------|------------------------------|------------------------------------------------------------------------------|
| `DATABASE_URL` | URL подключения к PostgreSQL | `postgres://postgres:postgres@localhost:5432/router_manager?sslmode=disable` |
| `GRPC_PORT`    | Порт gRPC-сервера            | `44044`                                                                      |
| `GRPC_TIMEOUT` | Таймаут gRPC                 | `5s`                                                                         |
| `COMMANDS_TTL` | TTL для `SENT` команд        | `30s`                                                                        |

---

## Дополнительно

- Sequence диаграмма [sequence.puml](./diagrams/sequence.puml)
- Компонентная диаграмма [component.puml](./diagrams/component.puml)
