FROM gradle:8.14.0-jdk24 AS builder

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD
ARG TESTCONTAINERS_HOST_OVERRIDE
ARG DOCKER_HOST

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}
ENV TESTCONTAINERS_HOST_OVERRIDE=${TESTCONTAINERS_HOST_OVERRIDE}
ENV DOCKER_HOST=${DOCKER_HOST}
ENV TESTCONTAINERS_RYUK_DISABLED=false

WORKDIR /app
COPY --chown=gradle:gradle . .

RUN apt-get update && apt-get install docker-cli socat -y
RUN socat -v UNIX-LISTEN:/var/run/docker.sock,fork TCP:$DOCKER_HOST & gradle clean build --no-daemon

FROM eclipse-temurin:24-jdk-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]