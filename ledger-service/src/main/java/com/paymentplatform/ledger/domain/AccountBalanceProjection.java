package com.paymentplatform.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "account_balance_projections", schema = "ledger")
public class AccountBalanceProjection {
    @EmbeddedId private AccountBalanceProjectionId id;
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4) private BigDecimal availableBalance;
    @Column(name = "posted_balance", nullable = false, precision = 19, scale = 4) private BigDecimal postedBalance;
    @Column(name = "last_entry_id") private UUID lastEntryId;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @Version private long version;

    protected AccountBalanceProjection() {}
}
