include $(wildcard .env)

KEYCLOAK_CONTAINER := keycloak
DOCKER_COMPOSE = docker-compose
ENV_FILE ?= .env
COMPOSE_FILE ?= infrastructure/docker-compose.yaml

INFRA_SERVICES ?= postgres_commands postgres_devices postgres_keycloak postgres_camunda \
            redis keycloak kafka schema-registry cassandra camunda minio prometheus \
            loki alloy tempo grafana \
            postgres-exporter-commands postgres-exporter-devices \
            postgres-exporter-keycloak postgres-exporter-camunda kafka-exporter \
            redis-exporter

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