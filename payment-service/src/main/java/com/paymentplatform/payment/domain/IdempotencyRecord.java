package com.paymentplatform.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "idempotency_records", schema = "payment")
public class IdempotencyRecord {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String scope;
    @Column(name = "idempotency_key", nullable = false, length = 255) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @ManyToOne @JoinColumn(name = "payment_id") private Payment payment;
    @Column(name = "response_status") private Integer responseStatus;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private String responseBody;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IdempotencyStatus state;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected IdempotencyRecord() {}

    public static IdempotencyRecord inProgress(String scope, String idempotencyKey, String requestHash) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.id = UUID.randomUUID();
        record.scope = scope;
        record.idempotencyKey = idempotencyKey;
        record.requestHash = requestHash;
        record.state = IdempotencyStatus.IN_PROGRESS;
        record.createdAt = Instant.now();
        record.updatedAt = record.createdAt;
        record.expiresAt = record.createdAt.plus(24, ChronoUnit.HOURS);
        return record;
    }

    public boolean hasRequestHash(String candidate) { return requestHash.equals(candidate); }
    public Payment getPayment() { return payment; }
    public IdempotencyStatus getState() { return state; }

    public void complete(Payment completedPayment, String completedResponse) {
        payment = completedPayment;
        responseStatus = 201;
        responseBody = completedResponse;
        state = IdempotencyStatus.COMPLETED;
        updatedAt = Instant.now();
    }
}
