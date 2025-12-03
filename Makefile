include $(wildcard .env)

KEYCLOAK_CONTAINER := keycloak
DOCKER_COMPOSE = docker-compose
NEXUS_URL = http://localhost:8085
ENV_FILE ?= .env
COMPOSE_FILE ?= infrastructure/docker-compose.yaml

MONITORING_SERVICES ?= grafana prometheus alloy tempo loki

INFRA_SERVICES ?= $(MONITORING_SERVICES) postgres_commands postgres_camunda postgres_keycloak  \
			postgres-devices-shard-0-master postgres-devices-shard-1-master \
			postgres-devices-shard-0-replica postgres-devices-shard-1-replica \
            postgres-exporter-devices-shard-0-master postgres-exporter-devices-shard-1-master \
            postgres-exporter-devices-shard-0-replica postgres-exporter-devices-shard-1-replica \
            redis keycloak kafka schema-registry cassandra camunda minio postgres-exporter-commands \
            postgres-exporter-keycloak postgres-exporter-camunda kafka-exporter \
            redis-exporter

KAFKA_SERVICES ?= kafka schema-registry kafka-ui

EVENT_COLLECTOR_SERVICES ?= event-collector cassandra $(KAFKA_SERVICES)

EVENT_COLLECTOR_MONITORING ?= $(MONITORING_SERVICES) kafka-exporter

DEVICES_DB_CLUSTER ?= postgres-devices-shard-0-master postgres-devices-shard-1-master \
					postgres-devices-shard-0-replica postgres-devices-shard-1-replica

DEVICE_COLLECTOR_SERVICES ?= device-collector $(KAFKA_SERVICES) $(DEVICES_DB_CLUSTER)

DEVICE_COLLECTOR_MONITORING ?= device-collector $(MONITORING_SERVICES) kafka-exporter postgres-exporter-devices-shard-0-master \
postgres-exporter-devices-shard-1-master postgres-exporter-devices-shard-0-replica postgres-exporter-devices-shard-1-replica

SECURITY ?= keycloak postgres_keycloak

DEVICE_SERVICE ?= device-service redis $(DEVICES_DB_CLUSTER) $(KAFKA_SERVICES)

DEVICE_SERVICE_MONITORING ?= $(DEVICE_SERVICE) $(MONITORING_SERVICES) redis-exporter kafka-exporter \
							postgres-exporter-devices-shard-0-master postgres-exporter-devices-shard-1-master \
	  						postgres-exporter-devices-shard-0-replica postgres-exporter-devices-shard-1-replica

SHARED_ARTIFACTS ?= avro-shared-library-publisher

SERVICES_ARTIFACTS ?= device-service-publisher

ifeq ($(OS),Windows_NT)
WAIT_CMD = powershell -Command "while ($$true) { \
		try { \
			Invoke-WebRequest -UseBasicParsing -Uri $(NEXUS_URL)/service/rest/v1/status -ErrorAction Stop; \
			break \
		} \
		catch { \
			Write-Host 'Nexus not ready, sleeping...'; \
			Start-Sleep -Seconds 5 \
		} \
	}"
else
WAIT_CMD = until curl -sf $(NEXUS_URL)/service/rest/v1/status; do \
echo 'Nexus not ready, sleeping...'; sleep 5; \
done
endif

docker-proxy-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d docker-proxy

docker-proxy-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down docker-proxy

keycloak-disable-ssl:
	@echo "Авторизация в Keycloak CLI (master realm)"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh config credentials \
		--config /tmp/kc-master.json \
		--server http://127.0.0.1:8080 \
		--realm master \
		--user "$(KC_BOOTSTRAP_ADMIN_USERNAME)" \
		--password "$(KC_BOOTSTRAP_ADMIN_PASSWORD)"

	@echo "Отключение sslRequired=NONE в master realm"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh update \
		--config /tmp/kc-master.json \
		realms/master \
		-s sslRequired=NONE
	@echo "SSL requirement отключён в master realm"
	@echo ""

	@echo "Авторизация в Keycloak CLI (iot realm)"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh config credentials \
		--config /tmp/kc-iot.json \
		--server http://127.0.0.1:8080 \
		--realm master \
		--user "$(KC_BOOTSTRAP_ADMIN_USERNAME)" \
		--password "$(KC_BOOTSTRAP_ADMIN_PASSWORD)"

	@echo "Отключение sslRequired=NONE в iot realm"
	docker exec -it $(KEYCLOAK_CONTAINER) /opt/keycloak/bin/kcadm.sh update \
		--config /tmp/kc-iot.json \
		realms/iot \
		-s sslRequired=NONE
	@echo "SSL requirement отключён в iot realm"

nexus-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d nexus
	@echo "Waiting for Nexus to be healthy..."
	@$(WAIT_CMD)
	@echo "Nexus is healthy!"

nexus-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down nexus

nexus-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v nexus

build-shared-artifacts:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) build $(SHARED_ARTIFACTS) --no-cache

build-all-artifacts:
	@$(MAKE) build-shared-artifacts
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) build $(SERVICES_ARTIFACTS) --no-cache
	@$(MAKE) docker-proxy-stop

build-device-service-artifact:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) build device-service-publisher --no-cache
	@$(MAKE) docker-proxy-stop

security-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(SECURITY)

security-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(SECURITY)

security-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(SECURITY)

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
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) --build
	@$(MAKE) docker-proxy-stop

event-collector-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(EVENT_COLLECTOR_SERVICES)

event-collector-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(EVENT_COLLECTOR_SERVICES)

event-collector-monitoring-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)

event-collector-monitoring-start-build:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING) --build
	@$(MAKE) docker-proxy-stop

event-collector-monitoring-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)

event-collector-monitoring-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(EVENT_COLLECTOR_SERVICES) $(EVENT_COLLECTOR_MONITORING)

device-collector-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_COLLECTOR_SERVICES)

device-collector-start-build:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_COLLECTOR_SERVICES) --build
	@$(MAKE) docker-proxy-stop

device-collector-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(DEVICE_COLLECTOR_SERVICES)

device-collector-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(DEVICE_COLLECTOR_SERVICES)

device-collector-monitoring-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_COLLECTOR_SERVICES) $(DEVICE_COLLECTOR_MONITORING)

device-collector-monitoring-start-build:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_COLLECTOR_SERVICES) $(DEVICE_COLLECTOR_MONITORING) --build
	@$(MAKE) docker-proxy-stop

device-collector-monitoring-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(DEVICE_COLLECTOR_SERVICES) $(DEVICE_COLLECTOR_MONITORING)

device-collector-monitoring-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(DEVICE_COLLECTOR_SERVICES) $(DEVICE_COLLECTOR_MONITORING)

device-service-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_SERVICE)

device-service-start-build:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_SERVICE) --build
	@$(MAKE) docker-proxy-stop

device-service-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(DEVICE_SERVICE)

device-service-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(DEVICE_SERVICE)

device-service-monitoring-start:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_SERVICE) $(DEVICE_SERVICE_MONITORING)

device-service-monitoring-start-build:
	@$(MAKE) docker-proxy-start
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d $(DEVICE_SERVICE) $(DEVICE_SERVICE_MONITORING) --build
	@$(MAKE) docker-proxy-stop

device-service-monitoring-stop:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down $(DEVICE_SERVICE) $(DEVICE_SERVICE_MONITORING)

device-service-monitoring-stop-remove:
	$(DOCKER_COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down -v $(DEVICE_SERVICE) $(DEVICE_SERVICE_MONITORING)