package com.paymentplatform.fraud.api;

import java.util.List;

public record FraudEvaluationResponse(String decision, List<RuleMatch> matches) {
    public record RuleMatch(String code, String action, String reason) {}
}
