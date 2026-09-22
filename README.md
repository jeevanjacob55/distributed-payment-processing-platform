# Distributed Payment Processing Platform

A fault-tolerant, event-driven payment platform designed to process concurrent requests safely and prevent duplicate charges.

## Services

| Service | Local port | Responsibility |
| --- | ---: | --- |
| API gateway | 8080 | Edge routing and cross-cutting request handling |
| Payment service | 8081 | Payment lifecycle, idempotency, authorization and outbox |
| Ledger service | 8082 | Immutable double-entry ledger and balance projections |
| Fraud service | 8083 | Asynchronous payment risk decisions |
| Notification service | 8084 | Payment-status notifications and delivery retries |

See [architecture documentation](docs/architecture.md) for boundaries, ownership, events and deployment topology. Local environment instructions are in [docker/README.md](docker/README.md).

## Development conventions

- Java 21, Maven, Spring Boot and UTC timestamps are used throughout.
- Use `dev`, `test`, and `prod` Spring profiles; no secrets belong in source control.
- Run `mvn spotless:apply` before opening a pull request once Maven is available.
- Follow [CONTRIBUTING.md](CONTRIBUTING.md) for branch and commit conventions.
