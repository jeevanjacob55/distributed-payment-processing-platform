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

Kafka topics use versioned payloads and a partition key of `paymentId` to preserve payment event ordering:

| Topic | Producer | Consumers |
| --- | --- | --- |
| `payment.lifecycle.v1` | Payment service | Ledger, fraud, notification |
| `payment.fraud-decision.v1` | Fraud service | Payment service |
| `payment.dlq.v1` | Any failed consumer | Operations/replay worker |

Redis is not a source of financial truth. It is reserved for short-lived rate limits, idempotency hot-cache entries, and narrowly scoped distributed locks where a documented cross-resource critical section cannot be protected by PostgreSQL row/version locking. PostgreSQL constraints and transactions remain the final guarantee against duplicate charges.

## AWS topology

Production deploys independently scalable containers to ECS/Fargate behind an Application Load Balancer. Private subnets contain the services, RDS PostgreSQL, ElastiCache Redis and MSK; only the load balancer is public. Secrets are retrieved from AWS Secrets Manager through task IAM roles. CloudWatch collects platform logs; Prometheus-compatible metrics feed Grafana. Terraform defines all infrastructure, with separate state and parameters per environment.
