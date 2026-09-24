package com.paymentplatform.payment.api;

import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.domain.PaymentStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID payerAccountId,
        UUID payeeAccountId,
        BigDecimal amount,
        String currency,
        String merchantReference,
        PaymentStatus status,
        String failureCode,
        Instant createdAt,
        Instant updatedAt) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getPayerAccount().getId(),
                payment.getPayeeAccount().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantReference(),
                payment.getStatus(),
                payment.getFailureCode(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
