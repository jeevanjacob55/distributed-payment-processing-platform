CREATE TABLE fraud_rules (
    id UUID PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    rule_type VARCHAR(32) NOT NULL,
    action VARCHAR(16) NOT NULL DEFAULT 'BLOCK',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    parameters JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fraud_rules_type_valid CHECK (rule_type IN (
        'MAX_AMOUNT', 'VELOCITY', 'REPEATED_REFERENCE', 'MAX_VOLUME', 'BLOCKED_ACCOUNT'
    )),
    CONSTRAINT fraud_rules_action_valid CHECK (action IN ('REVIEW', 'BLOCK'))
);

CREATE INDEX fraud_rules_enabled_idx ON fraud_rules (enabled, rule_type);
CREATE INDEX fraud_event_inbox_received_at_idx ON event_inbox (received_at DESC);
