FROM gradle:8.14.2-jdk24 AS build

ARG NEXUS_URL
ARG NEXUS_USERNAME
ARG NEXUS_PASSWORD

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY src ./src

RUN gradle --no-daemon clean build publish