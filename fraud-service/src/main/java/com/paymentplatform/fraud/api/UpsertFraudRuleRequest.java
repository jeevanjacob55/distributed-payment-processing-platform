package com.paymentplatform.fraud.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentplatform.fraud.domain.FraudRuleAction;
import com.paymentplatform.fraud.domain.FraudRuleType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpsertFraudRuleRequest(
        @NotBlank @Pattern(regexp = "[A-Z0-9_.-]{2,64}") String code,
        @NotNull FraudRuleType type,
        @NotNull FraudRuleAction action,
        boolean enabled,
        @NotNull JsonNode parameters) {}
