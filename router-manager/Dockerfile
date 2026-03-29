FROM golang:1.24 AS builder

ARG TESTCONTAINERS_HOST_OVERRIDE
ARG DOCKER_HOST

ENV TESTCONTAINERS_HOST_OVERRIDE=${TESTCONTAINERS_HOST_OVERRIDE}
ENV DOCKER_HOST=${DOCKER_HOST}

WORKDIR /app

RUN apt-get update && apt-get install docker-cli socat -y

COPY go.mod go.sum ./
RUN go mod download

COPY . .

RUN socat -v UNIX-LISTEN:/var/run/docker.sock,forkr TCP:$DOCKER_HOST & go test -v ./tests/...
RUN CGO_ENABLED=0 GOOS=linux go build -o /router-manager ./cmd/router-manager

FROM debian:bookworm-slim

WORKDIR /app

COPY --from=builder /router-manager .
COPY --from=builder /app/config ./config
COPY --from=builder /app/migrations ./migrations

EXPOSE 44044

CMD ["./router-manager", "-config", "config/config.yaml"]
