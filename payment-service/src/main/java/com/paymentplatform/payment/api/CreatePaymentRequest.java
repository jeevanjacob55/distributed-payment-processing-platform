package com.paymentplatform.payment.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID payerAccountId,
        @NotNull UUID payeeAccountId,
        @NotNull @DecimalMin(value = "0.0001") BigDecimal amount,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be an ISO-4217 uppercase currency code") String currency,
        @NotBlank @Size(max = 128) String merchantReference) {}
