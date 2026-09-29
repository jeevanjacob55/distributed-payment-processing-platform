# Architecture

## Service boundaries and ownership

| Service | Owns | Synchronous responsibilities | Asynchronous responsibilities |
| --- | --- | --- | --- |
| API gateway | Routing configuration only | Authentication boundary, request IDs, routing | None |
| Payment service | Payments, payment attempts, idempotency records and outbox records | Create, retrieve and refund payments; validate commands | Publishes payment lifecycle events |
| Ledger service | Immutable ledger entries and balance projections | Balance and transaction-history reads | Consumes finalized payment/refund events |
| Fraud service | Fraud rules and decisions | Administrative rule configuration (future) | Consumes payment events and publishes decisions |
| Notification service | Notification records and delivery attempts | None | Consumes status events and sends notifications |

Each service owns its schema/database credentials. Services never write another service's tables. The payment service is the command authority for a payment; the ledger is the financial record authority.

## Communication

Client commands enter through the gateway and call the payment service synchronously. The payment service commits its state transition and transactional-outbox record in one PostgreSQL transaction. An outbox publisher subsequently delivers the event to Kafka. Consumers are idempotent and persist their consumption state before acknowledging a message.

## Gateway routing and security

The API gateway uses static local routes for `/api/payments/**`, `/api/accounts/**`, `/api/fraud/**`, and `/api/notifications/**`; service URLs are environment-configured for container and cloud deployments. It is an OAuth 2.0 resource server that validates JWT signatures and issuer claims using an OpenID Connect provider's JWK Set. Role-based route authorization is described in [security.md](security.md).

Kafka topics use versioned payloads and a partition key of `paymentId` to preserve payment event ordering:

| Topic | Producer | Consumers |
| --- | --- | --- |
| `payment.lifecycle.v1` | Payment service | Ledger, fraud, notification |
| `payment.fraud-decision.v1` | Fraud service | Payment service |
| `payment.dlq.v1` | Any failed consumer | Operations/replay worker |

The Fraud Service stores enabled, versionable-by-code rule definitions in its own schema and evaluates configured maximum amounts, transaction velocity, repeated failed references, rolling transaction volume, and blocked account IDs. Rule changes use the Fraud Service rule API and take effect on the next evaluation. An evaluation returns `APPROVED`, `REVIEW`, or `BLOCKED` with matching rule codes. The payment event decision loop is task 38; the task 37 API can also be called directly for rule administration and evaluation.

Redis is not a source of financial truth. It is reserved for short-lived rate limits, idempotency hot-cache entries, and narrowly scoped distributed locks where a documented cross-resource critical section cannot be protected by PostgreSQL row/version locking. PostgreSQL constraints and transactions remain the final guarantee against duplicate charges.

### Distributed locking

Payment creation uses a Redis `SET NX` lock only for the short cross-instance window after a new idempotency key has been observed but before its PostgreSQL idempotency row is committed. The key is SHA-256 hashed before use in Redis, has a 30-second TTL, and is released with an owner-token Lua compare-and-delete script after the database transaction completes. This prevents two application instances from independently treating the same new key as absent. A lock miss returns `409 PAYMENT_IN_PROGRESS`; a Redis outage returns `503 IDEMPOTENCY_UNAVAILABLE` without processing the command.

Redis is deliberately not used to lock account balances or authorize funds. Those operations use deterministic PostgreSQL `PESSIMISTIC_WRITE` account-row locks, optimistic versions, constraints, and one transaction, which remain authoritative if Redis expires, restarts, or is partitioned.

### Outbox delivery and compensation

Each payment and refund state change appends a versioned JSON lifecycle event to `payment.outbox_events` in the same transaction as its account, payment, refund, and idempotency updates. A scheduled publisher locks eligible rows with `FOR UPDATE SKIP LOCKED`, waits for Kafka broker acknowledgement, then records `published_at`. It only claims the earliest unpublished event for each payment aggregate, so multiple publisher instances cannot overtake each other. A crash after Kafka accepts an event but before PostgreSQL commits `published_at` can cause redelivery; consumers deduplicate by stable `eventId` and therefore provide at-least-once delivery without duplicate ledger entries.

The payment command is one PostgreSQL transaction: a failure before commit rolls back both account balance changes and the payment/outbox rows. After commit, the immutable ledger consumes completion and refund events independently. A refund is the compensating financial operation for a completed payment; it creates a new balanced transfer rather than editing the original ledger entries. Consumer failures are retried three times and then copied to `payment.dlq.v1` for operator replay. Ledger, fraud, and notification services persist an event inbox record before acknowledging, making replay idempotent.

## AWS topology

Production deploys independently scalable containers to ECS/Fargate behind an Application Load Balancer. Private subnets contain the services, RDS PostgreSQL, ElastiCache Redis and MSK; only the load balancer is public. Secrets are retrieved from AWS Secrets Manager through task IAM roles. CloudWatch collects platform logs; Prometheus-compatible metrics feed Grafana. Terraform defines all infrastructure, with separate state and parameters per environment.
