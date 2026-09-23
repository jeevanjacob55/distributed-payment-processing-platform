# Shared configuration conventions

## Build and dependency policy

The root Maven parent is the single authority for Java and shared plugin versions. The platform requires Java 21, UTF-8 sources, and Maven dependency management from Spring Boot 3.4.5 and Spring Cloud 2024.0.1. Services must not override these versions locally. New shared dependencies and build plugins belong in the root `pom.xml`; service POMs declare only the dependencies they consume.

## Runtime configuration

Configuration is supplied through environment variables, with Spring's relaxed binding used by service configuration. `.env.example` is a non-secret local template; real `.env` files are ignored. The active profile is `SPRING_PROFILES_ACTIVE` (`dev`, `test`, or `prod`) and defaults to `dev` only when it is absent.

| Service | Environment variable | Default port |
| --- | --- | ---: |
| API gateway | `SERVER_PORT` | 8080 |
| Payment service | `SERVER_PORT` | 8081 |
| Ledger service | `SERVER_PORT` | 8082 |
| Fraud service | `SERVER_PORT` | 8083 |
| Notification service | `SERVER_PORT` | 8084 |

Container orchestration sets `SERVER_PORT` independently for each service; the named port variables in `.env.example` are the local convention for that mapping.

All services run with `TZ=UTC` and serialize time as UTC ISO-8601 values. Monetary timestamps are stored as PostgreSQL `timestamptz`, never local timestamps.

## Logging

The common console pattern emits one event per line with UTC timestamp, level, service name, trace ID, request ID, logger, and message. Do not log card data, authentication tokens, passwords, or unredacted personal data. `dev` uses `INFO`, `test` uses `WARN`, and `prod` enables graceful shutdown. JSON logging and automatic correlation-ID propagation will be added with observability work (task 45).
