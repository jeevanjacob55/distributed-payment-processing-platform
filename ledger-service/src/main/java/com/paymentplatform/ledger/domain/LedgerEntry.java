package com.paymentplatform.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_entries", schema = "ledger")
public class LedgerEntry {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "transaction_id", nullable = false) private LedgerTransaction transaction;
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LedgerEntryDirection direction;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private LedgerEntryStatus status;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;

    protected LedgerEntry() {}
}
