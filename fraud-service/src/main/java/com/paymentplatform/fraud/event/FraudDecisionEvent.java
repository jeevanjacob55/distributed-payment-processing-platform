package com.paymentplatform.fraud.event;

import com.paymentplatform.fraud.api.FraudEvaluationResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudDecisionEvent(
        UUID eventId,
        int schemaVersion,
        String eventType,
        UUID paymentId,
        UUID sourceEventId,
        String decision,
        List<FraudEvaluationResponse.RuleMatch> matches,
        Instant decidedAt) {}
