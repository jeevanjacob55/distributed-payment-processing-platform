package com.paymentplatform.ledger.event;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerEventProcessor {
    private final JdbcTemplate jdbcTemplate;

    public LedgerEventProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void process(PaymentLifecycleEvent event) {
        int inserted = jdbcTemplate.update(
                "insert into ledger.consumed_events (event_id, event_type) values (?, ?) on conflict do nothing",
                event.eventId(),
                event.eventType());
        if (inserted == 0) {
            return;
        }
        if (!"payment.completed.v1".equals(event.eventType())
                && !"refund.completed.v1".equals(event.eventType())) {
            return;
        }

        UUID transactionId = UUID.randomUUID();
        String referenceType = "payment.completed.v1".equals(event.eventType()) ? "PAYMENT" : "REFUND";
        jdbcTemplate.update(
                "insert into ledger.ledger_transactions (id, reference_type, reference_id, status, description, occurred_at) values (?, ?, ?, 'POSTED', ?, ?)",
                transactionId,
                referenceType,
                event.eventId(),
                referenceType + " " + event.paymentId(),
                event.occurredAt());

        boolean refund = "REFUND".equals(referenceType);
        UUID debitAccount = refund ? event.payeeAccountId() : event.payerAccountId();
        UUID creditAccount = refund ? event.payerAccountId() : event.payeeAccountId();
        insertEntry(transactionId, debitAccount, "DEBIT", event);
        insertEntry(transactionId, creditAccount, "CREDIT", event);
    }

    private void insertEntry(UUID transactionId, UUID accountId, String direction, PaymentLifecycleEvent event) {
        jdbcTemplate.update(
                "insert into ledger.ledger_entries (id, transaction_id, account_id, direction, amount, currency, status, created_at) values (?, ?, ?, ?, ?, ?, 'POSTED', ?)",
                UUID.randomUUID(),
                transactionId,
                accountId,
                direction,
                event.amount(),
                event.currency(),
                event.occurredAt());
    }
}
