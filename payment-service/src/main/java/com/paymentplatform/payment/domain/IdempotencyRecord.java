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
import java.util.UUID;

@Entity
@Table(name = "idempotency_records", schema = "payment")
public class IdempotencyRecord {
    @Id private UUID id;
    @Column(nullable = false, length = 64) private String scope;
    @Column(name = "idempotency_key", nullable = false, length = 255) private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64) private String requestHash;
    @ManyToOne @JoinColumn(name = "payment_id") private Payment payment;
    @Column(name = "response_status") private Integer responseStatus;
    @Column(name = "response_body", columnDefinition = "jsonb") private String responseBody;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private IdempotencyStatus state;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected IdempotencyRecord() {}
}
