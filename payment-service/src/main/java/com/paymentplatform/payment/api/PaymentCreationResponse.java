package com.paymentplatform.payment.api;

public record PaymentCreationResponse(PaymentResponse payment, boolean created) {}
