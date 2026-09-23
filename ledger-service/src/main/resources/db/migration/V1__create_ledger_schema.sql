CREATE TABLE ledger_transactions (
    id UUID PRIMARY KEY,
    reference_type VARCHAR(32) NOT NULL,
    reference_id UUID NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    description VARCHAR(255) NOT NULL,
    reversal_of_transaction_id UUID REFERENCES ledger_transactions (id),
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ledger_transactions_reference_type_valid CHECK (reference_type IN ('PAYMENT', 'REFUND', 'REVERSAL', 'ADJUSTMENT')),
    CONSTRAINT ledger_transactions_status_valid CHECK (status IN ('PENDING', 'POSTED', 'REVERSED')),
    CONSTRAINT ledger_transactions_reference_unique UNIQUE (reference_type, reference_id),
    CONSTRAINT ledger_transactions_no_self_reversal CHECK (id <> reversal_of_transaction_id)
);

CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    transaction_id UUID NOT NULL REFERENCES ledger_transactions (id),
    account_id UUID NOT NULL,
    direction VARCHAR(6) NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    currency CHAR(3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ledger_entries_amount_positive CHECK (amount > 0),
    CONSTRAINT ledger_entries_currency_format CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ledger_entries_direction_valid CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT ledger_entries_status_valid CHECK (status IN ('PENDING', 'POSTED', 'REVERSED'))
);

CREATE TABLE account_balance_projections (
    account_id UUID NOT NULL,
    currency CHAR(3) NOT NULL,
    available_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    posted_balance NUMERIC(19, 4) NOT NULL DEFAULT 0,
    last_entry_id UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (account_id, currency),
    CONSTRAINT account_balance_projections_currency_format CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE INDEX ledger_entries_account_created_at_idx ON ledger_entries (account_id, created_at DESC, id DESC);
CREATE INDEX ledger_entries_transaction_id_idx ON ledger_entries (transaction_id);
CREATE INDEX ledger_transactions_occurred_at_idx ON ledger_transactions (occurred_at DESC);
