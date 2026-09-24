package com.paymentplatform.payment.exception;

public class DuplicateReferenceException extends RuntimeException {
    public DuplicateReferenceException(String message) {
        super(message);
    }
}
