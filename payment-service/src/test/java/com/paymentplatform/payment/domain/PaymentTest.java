package com.paymentplatform.payment.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PaymentTest {
    @Test
    void permitsTheHappyPathStateTransitions() {
        Payment payment = payment();

        payment.transitionTo(PaymentStatus.VALIDATED);
        payment.transitionTo(PaymentStatus.AUTHORIZED);
        payment.transitionTo(PaymentStatus.COMPLETED);

        assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    }

    @Test
    void rejectsInvalidStateTransitions() {
        Payment payment = payment();

        assertThrows(IllegalStateException.class, () -> payment.transitionTo(PaymentStatus.COMPLETED));
    }

    private Payment payment() {
        return Payment.create(null, null, new BigDecimal("10.00"), "USD", "order-123", "key-123");
    }
}
