package com.paymentplatform.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "accounts", schema = "payment")
public class Account {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id", nullable = false) private User user;
    @Column(nullable = false, length = 3) private String currency;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private AccountStatus status;
    @Column(name = "available_balance", nullable = false, precision = 19, scale = 4) private BigDecimal availableBalance;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Account() {}

    public UUID getId() {
        return id;
    }

    public String getCurrency() { return currency; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getAvailableBalance() { return availableBalance; }

    public void debit(BigDecimal amount) {
        if (availableBalance.compareTo(amount) < 0) {
            throw new IllegalStateException("insufficient available balance");
        }
        availableBalance = availableBalance.subtract(amount);
        updatedAt = Instant.now();
    }

    public void credit(BigDecimal amount) {
        availableBalance = availableBalance.add(amount);
        updatedAt = Instant.now();
    }
}
