package com.paymentplatform.ledger.api;

import com.paymentplatform.ledger.domain.LedgerEntry;
import com.paymentplatform.ledger.domain.LedgerTransaction;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        UUID entryId,
        UUID transactionId,
        String referenceType,
        UUID referenceId,
        String description,
        UUID accountId,
        String direction,
        BigDecimal amount,
        String currency,
        String status,
        Instant createdAt) {
    public static LedgerEntryResponse from(LedgerEntry entry) {
        LedgerTransaction transaction = entry.getTransaction();
        return new LedgerEntryResponse(
                entry.getId(),
                transaction.getId(),
                transaction.getReferenceType(),
                transaction.getReferenceId(),
                transaction.getDescription(),
                entry.getAccountId(),
                entry.getDirection().name(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getStatus().name(),
                entry.getCreatedAt());
    }
}
