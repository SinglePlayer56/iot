include $(wildcard .env)

KEYCLOAK_CONTAINER := keycloak
DOCKER_COMPOSE = docker-compose
ENV_FILE ?= .env
COMPOSE_FILE ?= infrastructure/docker-compose.yaml

MONITORING_SERVICES ?= grafana prometheus alloy tempo loki

INFRA_SERVICES ?= $(MONITORING_SERVICES) postgres_commands postgres_devices postgres_keycloak postgres_camunda \
            redis keycloak kafka schema-registry cassandra camunda minio \
            postgres-exporter-commands postgres-exporter-devices \
            postgres-exporter-keycloak postgres-exporter-camunda kafka-exporter \
            redis-exporter

EVENT_COLLECTOR_SERVICES ?= event-collector cassandra kafka schema-registry kafka-ui

EVENT_COLLECTOR_MONITORING ?= $(MONITORING_SERVICES) kafka-exporter

keycloak-disable-ssl:
	@echo "Авторизация в Keycloak CLI"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh config credentials \
		--server http://127.0.0.1:8080 \
		--realm master \
		--user $(KC_BOOTSTRAP_ADMIN_USERNAME) \
		--password $(KC_BOOTSTRAP_ADMIN_PASSWORD)

	@echo "Отключение sslRequired=NONE в master realm"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh update realms/master -s sslRequired=NONE

	@echo "SSL requirement отключён в master realm"

start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d

stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down

stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v

infra:
	@echo "Starting infrastructure services: $(INFRA_SERVICES)"
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(INFRA_SERVICES)
	@echo "Infrastructure is ready."

infra-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(INFRA_SERVICES)

infra-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(INFRA_SERVICES)

ps:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d

event-collector-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES)

event-collector-start-build:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) --build

event-collector-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(EVENT_COLLECTOR_SERVICES)

event-collector-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(EVENT_COLLECTOR_SERVICES)

event-collector-monitoring-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)

event-collector-monitoring-start-build:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING) --build

event-collector-monitoring-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)

event-collector-monitoring-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)