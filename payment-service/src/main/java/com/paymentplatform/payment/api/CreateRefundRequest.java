package com.paymentplatform.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateRefundRequest(
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @NotBlank @Size(max = 128) String reference) {}
