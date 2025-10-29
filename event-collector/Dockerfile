FROM gradle:8.14.0-jdk24 AS builder

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /app
COPY --chown=gradle:gradle . .

RUN gradle clean build -x test --no-daemon

FROM eclipse-temurin:24-jdk-alpine

WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]