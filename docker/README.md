# Local development environment

Copy `.env.example` to `.env`, then start dependencies with:

```shell
docker compose --env-file .env up -d
```

PostgreSQL is exposed at `localhost:5432`, Redis at `localhost:6379`, Kafka at `localhost:9092`, Prometheus at `localhost:9090`, and Grafana at `localhost:3000`. Grafana's initial local credentials are configured in `.env.example` and must be changed outside local development.

Build a service image from the repository root so Maven has the parent project and all module descriptors available:

```shell
docker build -f payment-service/Dockerfile -t payment-service:local .
```
