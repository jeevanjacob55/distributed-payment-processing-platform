package com.paymentplatform.payment.exception;

public class PaymentProcessingException extends RuntimeException {
    public PaymentProcessingException() {
        super("A request with this Idempotency-Key is already being processed");
    }
}
