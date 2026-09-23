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
@Table(name = "payments", schema = "payment")
public class Payment {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "payer_account_id", nullable = false) private Account payerAccount;
    @ManyToOne(optional = false) @JoinColumn(name = "payee_account_id", nullable = false) private Account payeeAccount;
    @Column(nullable = false, precision = 19, scale = 4) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "merchant_reference", nullable = false, length = 128) private String merchantReference;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentStatus status;
    @Column(name = "failure_code", length = 64) private String failureCode;
    @Column(name = "idempotency_key", nullable = false, length = 255) private String idempotencyKey;
    @Version private long version;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected Payment() {}

    public static Payment create(
            Account payerAccount,
            Account payeeAccount,
            BigDecimal amount,
            String currency,
            String merchantReference,
            String idempotencyKey) {
        Payment payment = new Payment();
        payment.id = UUID.randomUUID();
        payment.payerAccount = payerAccount;
        payment.payeeAccount = payeeAccount;
        payment.amount = amount;
        payment.currency = currency;
        payment.merchantReference = merchantReference;
        payment.idempotencyKey = idempotencyKey;
        payment.status = PaymentStatus.CREATED;
        payment.createdAt = Instant.now();
        payment.updatedAt = payment.createdAt;
        return payment;
    }

    public UUID getId() { return id; }
    public Account getPayerAccount() { return payerAccount; }
    public Account getPayeeAccount() { return payeeAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getMerchantReference() { return merchantReference; }
    public PaymentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
}
