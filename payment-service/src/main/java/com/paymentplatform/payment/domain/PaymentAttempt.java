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
@Table(name = "payment_attempts", schema = "payment")
public class PaymentAttempt {
    @Id private UUID id;
    @ManyToOne(optional = false) @JoinColumn(name = "payment_id", nullable = false) private Payment payment;
    @Column(name = "attempt_number", nullable = false) private int attemptNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private PaymentAttemptStatus status;
    @Column(name = "processor_reference", length = 128) private String processorReference;
    @Column(name = "failure_code", length = 64) private String failureCode;
    @Column(name = "started_at", nullable = false) private Instant startedAt;
    @Column(name = "completed_at") private Instant completedAt;

    protected PaymentAttempt() {}
}
