CREATE TABLE fraud_decisions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    source_event_id UUID NOT NULL UNIQUE,
    decision VARCHAR(16) NOT NULL,
    payload JSONB NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fraud_decisions_value_valid CHECK (decision IN ('APPROVED', 'REVIEW', 'BLOCKED'))
);

CREATE TABLE fraud_outbox_events (
    id UUID PRIMARY KEY,
    aggregate_id UUID NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    published_at TIMESTAMPTZ,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    last_error VARCHAR(1000)
);

CREATE INDEX fraud_outbox_pending_idx
    ON fraud_outbox_events (occurred_at, id)
    WHERE published_at IS NULL;
