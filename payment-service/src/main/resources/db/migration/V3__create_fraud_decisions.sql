CREATE TABLE fraud_decisions (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    source_event_id UUID NOT NULL UNIQUE,
    decision VARCHAR(16) NOT NULL,
    payload JSONB NOT NULL,
    decided_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payment_fraud_decisions_value_valid CHECK (decision IN ('APPROVED', 'REVIEW', 'BLOCKED'))
);

CREATE INDEX payment_fraud_decisions_payment_id_idx ON fraud_decisions (payment_id, decided_at DESC);
