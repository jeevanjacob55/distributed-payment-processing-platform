package com.paymentplatform.payment.exception;

public class DistributedLockUnavailableException extends RuntimeException {
    public DistributedLockUnavailableException(Throwable cause) {
        super("Idempotency coordination is temporarily unavailable", cause);
    }
}
