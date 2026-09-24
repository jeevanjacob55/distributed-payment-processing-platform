package com.paymentplatform.payment.api;

import com.paymentplatform.payment.domain.Refund;
import com.paymentplatform.payment.domain.RefundStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record RefundResponse(
        UUID id,
        UUID paymentId,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        String reference,
        Instant createdAt) {
    public static RefundResponse from(Refund refund) {
        return new RefundResponse(
                refund.getId(),
                refund.getPaymentId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getStatus(),
                refund.getReference(),
                refund.getCreatedAt());
    }
}
