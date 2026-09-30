CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_id UUID NOT NULL REFERENCES event_inbox(event_id),
    payment_id UUID NOT NULL,
    account_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'IN_APP',
    message TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT notifications_event_account_uq UNIQUE (event_id, account_id)
);

CREATE INDEX notifications_account_created_idx ON notifications (account_id, created_at DESC);
