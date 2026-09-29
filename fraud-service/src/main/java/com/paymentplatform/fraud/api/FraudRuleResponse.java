package com.paymentplatform.fraud.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentplatform.fraud.domain.FraudRuleAction;
import com.paymentplatform.fraud.domain.FraudRuleType;
import java.time.Instant;
import java.util.UUID;

public record FraudRuleResponse(
        UUID id,
        String code,
        FraudRuleType type,
        FraudRuleAction action,
        boolean enabled,
        JsonNode parameters,
        Instant createdAt,
        Instant updatedAt) {}
