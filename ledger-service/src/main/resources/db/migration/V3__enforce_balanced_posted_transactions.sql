CREATE TABLE reconciliation_issues (
    account_id UUID NOT NULL,
    currency CHAR(3) NOT NULL,
    available_balance NUMERIC(19, 4) NOT NULL,
    posted_balance NUMERIC(19, 4) NOT NULL,
    calculated_balance NUMERIC(19, 4) NOT NULL,
    discrepancy NUMERIC(19, 4) NOT NULL,
    balanced BOOLEAN NOT NULL,
    checked_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (account_id, currency)
);

CREATE OR REPLACE FUNCTION ledger.assert_posted_transaction_balanced()
RETURNS TRIGGER AS $$
DECLARE
    target_transaction_id UUID;
    transaction_status VARCHAR(16);
    entry_count BIGINT;
    total_entry_count BIGINT;
    debit_total NUMERIC(19, 4);
    credit_total NUMERIC(19, 4);
    currency_count BIGINT;
BEGIN
    IF TG_TABLE_NAME = 'ledger_transactions' THEN
        target_transaction_id := COALESCE(NEW.id, OLD.id);
    ELSE
        target_transaction_id := COALESCE(NEW.transaction_id, OLD.transaction_id);
    END IF;

    SELECT status INTO transaction_status
    FROM ledger.ledger_transactions
    WHERE id = target_transaction_id;

    IF transaction_status IS DISTINCT FROM 'POSTED' THEN
        RETURN NULL;
    END IF;

    SELECT COUNT(*) FILTER (WHERE status = 'POSTED'),
           COUNT(*),
           COALESCE(SUM(amount) FILTER (WHERE direction = 'DEBIT' AND status = 'POSTED'), 0),
           COALESCE(SUM(amount) FILTER (WHERE direction = 'CREDIT' AND status = 'POSTED'), 0),
           COUNT(DISTINCT currency)
      INTO entry_count, total_entry_count, debit_total, credit_total, currency_count
      FROM ledger.ledger_entries
     WHERE transaction_id = target_transaction_id;

    IF entry_count = 0 OR entry_count <> total_entry_count OR currency_count <> 1 OR debit_total <> credit_total THEN
        RAISE EXCEPTION 'posted ledger transaction % is not balanced', target_transaction_id
            USING ERRCODE = '23514';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER ledger_transaction_must_balance
AFTER INSERT OR UPDATE ON ledger.ledger_transactions
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ledger.assert_posted_transaction_balanced();

CREATE CONSTRAINT TRIGGER ledger_entry_must_balance
AFTER INSERT OR UPDATE OR DELETE ON ledger.ledger_entries
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION ledger.assert_posted_transaction_balanced();

CREATE OR REPLACE FUNCTION ledger.reject_ledger_mutation()
RETURNS TRIGGER AS $$
BEGIN
    RAISE EXCEPTION 'ledger records are append-only; post a compensating transaction instead'
        USING ERRCODE = '23514';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER ledger_entries_append_only
BEFORE UPDATE OR DELETE ON ledger.ledger_entries
FOR EACH ROW EXECUTE FUNCTION ledger.reject_ledger_mutation();

CREATE TRIGGER posted_ledger_transactions_append_only
BEFORE UPDATE OR DELETE ON ledger.ledger_transactions
FOR EACH ROW WHEN (OLD.status <> 'PENDING')
EXECUTE FUNCTION ledger.reject_ledger_mutation();
