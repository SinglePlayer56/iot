#  IoT Infrastructure

Удобный набор команд для управления инфраструктурой проекта с помощью `Makefile` и `docker-compose`.  
С его помощью можно быстро поднимать и останавливать все необходимые сервисы для локальной разработки.

---

##  Перед запуском убедитесь, что установлены:

-  [Docker](https://docs.docker.com/get-docker/)
-  [Docker Compose](https://docs.docker.com/compose/install/)
-  [GNU Make](https://www.gnu.org/software/make/)

Также в корне проекта должен быть файл `.env` с переменными окружения.  
📄 Пример можно взять из: **`.env.example`**

---

## Основные команды Makefile

| Команда                     | Описание                                                                 |
|-----------------------------|--------------------------------------------------------------------------|
| `make start`                | Поднять **все сервисы** из `docker-compose.yaml`                         |
| `make stop`                 | Остановить все сервисы                                                  |
| `make stop-remove`          | Остановить все сервисы и удалить данные (тома)                           |
| `make infra`                | Поднять **только инфраструктурные** сервисы (БД, Kafka, Grafana и др.)    |
| `make infra-stop`           | Остановить инфраструктурные сервисы                                     |
| `make infra-stop-remove`    | Остановить инфраструктуру и удалить данные                               |
| `make keycloak-disable-ssl` | Отключить требование HTTPS в Keycloak                                   |

---

## Быстрый старт
1.  **Клонируй репозиторий**:
```
git clone git@gitlab.proselyte.net:ourcode-iot-oscar/viktor.git
cd viktor
```
2. **Подготовь переменные окружения**:
```
cp .env.example .env
```
3. **Подними инфраструктуру**:
```
make infra
```
4. **(Опционально) Отключи SSL в Keycloak**:
```
make keycloak-disable-ssl
```