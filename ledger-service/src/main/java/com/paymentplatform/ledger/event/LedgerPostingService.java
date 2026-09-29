package com.paymentplatform.ledger.event;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerPostingService {
    private final JdbcTemplate jdbcTemplate;

    public LedgerPostingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void post(PaymentLifecycleEvent event) {
        if (event.amount() == null || event.amount().signum() <= 0) {
            throw new IllegalArgumentException("ledger amount must be positive");
        }
        if (event.currency() == null || !event.currency().matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("ledger currency must be a three-letter uppercase code");
        }
        if (event.payerAccountId() == null || event.payeeAccountId() == null
                || event.payerAccountId().equals(event.payeeAccountId())) {
            throw new IllegalArgumentException("ledger transfer requires two distinct accounts");
        }

        boolean refund = "refund.completed.v1".equals(event.eventType());
        UUID transactionId = UUID.randomUUID();
        String referenceType = refund ? "REFUND" : "PAYMENT";
        jdbcTemplate.update(
                "insert into ledger.ledger_transactions (id, reference_type, reference_id, status, description, occurred_at) values (?, ?, ?, 'POSTED', ?, ?)",
                transactionId,
                referenceType,
                event.eventId(),
                referenceType + " " + event.paymentId(),
                event.occurredAt());

        UUID debitAccount = refund ? event.payeeAccountId() : event.payerAccountId();
        UUID creditAccount = refund ? event.payerAccountId() : event.payeeAccountId();
        UUID debitEntryId = insertEntry(transactionId, debitAccount, "DEBIT", event);
        UUID creditEntryId = insertEntry(transactionId, creditAccount, "CREDIT", event);
        updateProjection(debitAccount, event.currency(), event.amount().negate(), debitEntryId);
        updateProjection(creditAccount, event.currency(), event.amount(), creditEntryId);
    }

    private UUID insertEntry(UUID transactionId, UUID accountId, String direction, PaymentLifecycleEvent event) {
        UUID entryId = UUID.randomUUID();
        jdbcTemplate.update(
                "insert into ledger.ledger_entries (id, transaction_id, account_id, direction, amount, currency, status, created_at) values (?, ?, ?, ?, ?, ?, 'POSTED', ?)",
                entryId,
                transactionId,
                accountId,
                direction,
                event.amount(),
                event.currency(),
                event.occurredAt());
        return entryId;
    }

    private void updateProjection(UUID accountId, String currency, BigDecimal delta, UUID entryId) {
        jdbcTemplate.update(
                "insert into ledger.account_balance_projections (account_id, currency, available_balance, posted_balance, last_entry_id, updated_at) values (?, ?, ?, ?, ?, CURRENT_TIMESTAMP) on conflict (account_id, currency) do update set available_balance = ledger.account_balance_projections.available_balance + excluded.available_balance, posted_balance = ledger.account_balance_projections.posted_balance + excluded.posted_balance, last_entry_id = excluded.last_entry_id, updated_at = CURRENT_TIMESTAMP",
                accountId,
                currency,
                delta,
                delta,
                entryId);
    }
}
