package com.paymentplatform.ledger.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentLifecycleEvent(
        UUID eventId,
        int schemaVersion,
        String eventType,
        UUID paymentId,
        UUID payerAccountId,
        UUID payeeAccountId,
        BigDecimal amount,
        String currency,
        String merchantReference,
        String status,
        Instant occurredAt) {}
