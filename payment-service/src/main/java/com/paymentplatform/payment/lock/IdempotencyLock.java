package com.paymentplatform.payment.lock;

public interface IdempotencyLock extends AutoCloseable {
    @Override
    void close();
}
