package com.paymentplatform.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_transactions", schema = "ledger")
public class LedgerTransaction {
    @Id private UUID id;
    @Column(name = "reference_type", nullable = false, length = 32) private String referenceType;
    @Column(name = "reference_id", nullable = false) private UUID referenceId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LedgerTransactionStatus status;
    @Column(nullable = false, length = 255) private String description;
    @ManyToOne @JoinColumn(name = "reversal_of_transaction_id") private LedgerTransaction reversalOfTransaction;
    @Column(name = "occurred_at", nullable = false) private Instant occurredAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected LedgerTransaction() {}
}
