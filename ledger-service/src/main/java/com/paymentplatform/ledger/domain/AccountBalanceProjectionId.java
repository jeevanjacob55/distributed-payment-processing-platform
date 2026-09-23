package com.paymentplatform.ledger.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AccountBalanceProjectionId implements Serializable {
    @Column(name = "account_id", nullable = false) private UUID accountId;
    @Column(nullable = false, length = 3) private String currency;

    protected AccountBalanceProjectionId() {}

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof AccountBalanceProjectionId that)) return false;
        return Objects.equals(accountId, that.accountId) && Objects.equals(currency, that.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountId, currency);
    }
}
