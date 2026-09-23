CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(200) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT users_status_valid CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users (id),
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT accounts_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT accounts_balance_non_negative CHECK (available_balance >= 0),
    CONSTRAINT accounts_closed_balance_zero CHECK (status <> 'CLOSED' OR available_balance = 0),
    CONSTRAINT accounts_status_valid CHECK (status IN ('ACTIVE', 'FROZEN', 'CLOSED'))
);

CREATE TABLE payments (
    id UUID PRIMARY KEY,
    payer_account_id UUID NOT NULL REFERENCES accounts (id),
    payee_account_id UUID NOT NULL REFERENCES accounts (id),
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    merchant_reference VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    failure_code VARCHAR(64),
    idempotency_key VARCHAR(255) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT payments_amount_positive CHECK (amount > 0),
    CONSTRAINT payments_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT payments_distinct_accounts CHECK (payer_account_id <> payee_account_id),
    CONSTRAINT payments_status_valid CHECK (status IN ('CREATED', 'VALIDATED', 'AUTHORIZED', 'COMPLETED', 'FAILED', 'REVERSED')),
    CONSTRAINT payments_merchant_reference_unique UNIQUE (payer_account_id, merchant_reference)
);

CREATE TABLE payment_attempts (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments (id),
    attempt_number INTEGER NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'STARTED',
    processor_reference VARCHAR(128),
    failure_code VARCHAR(64),
    started_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMPTZ,
    CONSTRAINT payment_attempts_number_positive CHECK (attempt_number > 0),
    CONSTRAINT payment_attempts_completed_after_start CHECK (completed_at IS NULL OR completed_at >= started_at),
    CONSTRAINT payment_attempts_status_valid CHECK (status IN ('STARTED', 'SUCCEEDED', 'FAILED', 'TIMED_OUT')),
    CONSTRAINT payment_attempts_payment_number_unique UNIQUE (payment_id, attempt_number)
);

CREATE TABLE refunds (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL REFERENCES payments (id),
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    reference VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT refunds_amount_positive CHECK (amount > 0),
    CONSTRAINT refunds_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT refunds_status_valid CHECK (status IN ('CREATED', 'COMPLETED', 'FAILED', 'REVERSED'))
);

CREATE TABLE idempotency_records (
    id UUID PRIMARY KEY,
    scope VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(255) NOT NULL,
    request_hash CHAR(64) NOT NULL,
    payment_id UUID REFERENCES payments (id),
    response_status INTEGER,
    response_body JSONB,
    state VARCHAR(16) NOT NULL DEFAULT 'IN_PROGRESS',
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT idempotency_request_hash_format CHECK (request_hash ~ '^[a-f0-9]{64}$'),
    CONSTRAINT idempotency_response_status_valid CHECK (response_status IS NULL OR response_status BETWEEN 100 AND 599),
    CONSTRAINT idempotency_state_valid CHECK (state IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT idempotency_scope_key_unique UNIQUE (scope, idempotency_key)
);

CREATE INDEX accounts_user_id_idx ON accounts (user_id);
CREATE INDEX payments_payer_created_at_idx ON payments (payer_account_id, created_at DESC);
CREATE INDEX payments_payee_created_at_idx ON payments (payee_account_id, created_at DESC);
CREATE INDEX payment_attempts_payment_id_idx ON payment_attempts (payment_id);
CREATE INDEX refunds_payment_id_idx ON refunds (payment_id);
CREATE INDEX idempotency_records_expires_at_idx ON idempotency_records (expires_at);
