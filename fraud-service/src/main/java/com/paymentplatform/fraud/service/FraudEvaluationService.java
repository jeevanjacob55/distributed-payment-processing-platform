package com.paymentplatform.fraud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.paymentplatform.fraud.api.FraudEvaluationRequest;
import com.paymentplatform.fraud.api.FraudEvaluationResponse;
import com.paymentplatform.fraud.api.FraudRuleResponse;
import com.paymentplatform.fraud.domain.FraudRuleAction;
import com.paymentplatform.fraud.domain.FraudRuleType;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudEvaluationService {
    private final FraudRuleService ruleService;
    private final JdbcTemplate jdbcTemplate;

    public FraudEvaluationService(FraudRuleService ruleService, JdbcTemplate jdbcTemplate) {
        this.ruleService = ruleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional(readOnly = true)
    public FraudEvaluationResponse evaluate(FraudEvaluationRequest request) {
        return evaluate(request, null);
    }

    @Transactional(readOnly = true)
    public FraudEvaluationResponse evaluateForEvent(FraudEvaluationRequest request, UUID currentPaymentId) {
        return evaluate(request, currentPaymentId);
    }

    private FraudEvaluationResponse evaluate(FraudEvaluationRequest request, UUID currentPaymentId) {
        List<FraudEvaluationResponse.RuleMatch> matches = new ArrayList<>();
        for (FraudRuleResponse rule : ruleService.enabledRules()) {
            String reason = evaluateRule(rule, request, currentPaymentId);
            if (reason != null) {
                matches.add(new FraudEvaluationResponse.RuleMatch(rule.code(), rule.action().name(), reason));
            }
        }
        String decision = matches.stream().anyMatch(match -> FraudRuleAction.BLOCK.name().equals(match.action()))
                ? "BLOCKED"
                : matches.isEmpty() ? "APPROVED" : "REVIEW";
        return new FraudEvaluationResponse(decision, List.copyOf(matches));
    }

    private String evaluateRule(FraudRuleResponse rule, FraudEvaluationRequest request, UUID currentPaymentId) {
        JsonNode parameters = rule.parameters();
        return switch (rule.type()) {
            case MAX_AMOUNT -> request.amount().compareTo(parameters.get("threshold").decimalValue()) > 0
                    ? "payment amount exceeds rule threshold"
                    : null;
            case VELOCITY -> velocityExceeded(request, parameters, currentPaymentId);
            case REPEATED_REFERENCE -> repeatedReference(request, parameters);
            case MAX_VOLUME -> volumeExceeded(request, parameters);
            case BLOCKED_ACCOUNT -> isBlocked(request.accountId(), parameters)
                    ? "account is present on the configured deny list"
                    : null;
        };
    }

    private String velocityExceeded(
            FraudEvaluationRequest request, JsonNode parameters, UUID currentPaymentId) {
        int maxTransactions = parameters.get("maxTransactions").intValue();
        int windowMinutes = parameters.get("windowMinutes").intValue();
        String sql = "select count(distinct payload->>'paymentId') from fraud.event_inbox where event_type = 'payment.created.v1' and payload->>'payerAccountId' = ? and received_at >= CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')";
        List<Object> arguments = new ArrayList<>();
        arguments.add(request.accountId().toString());
        arguments.add(windowMinutes);
        if (currentPaymentId != null) {
            sql += " and payload->>'paymentId' <> ?";
            arguments.add(currentPaymentId.toString());
        }
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, arguments.toArray());
        return count != null && count >= maxTransactions ? "account exceeded its transaction velocity limit" : null;
    }

    private String repeatedReference(FraudEvaluationRequest request, JsonNode parameters) {
        int maxAttempts = parameters.get("maxAttempts").intValue();
        int windowMinutes = parameters.get("windowMinutes").intValue();
        Integer attempts = jdbcTemplate.queryForObject(
                "select count(distinct event_id) from fraud.event_inbox where event_type = 'payment.failed.v1' and payload->>'payerAccountId' = ? and payload->>'merchantReference' = ? and received_at >= CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')",
                Integer.class,
                request.accountId().toString(),
                request.merchantReference(),
                windowMinutes);
        return attempts != null && attempts >= maxAttempts ? "merchant reference has repeated failed attempts" : null;
    }

    private String volumeExceeded(FraudEvaluationRequest request, JsonNode parameters) {
        BigDecimal maxVolume = parameters.get("maxVolume").decimalValue();
        int windowMinutes = parameters.get("windowMinutes").intValue();
        BigDecimal volume = jdbcTemplate.queryForObject(
                "select COALESCE(sum((payload->>'amount')::numeric), 0) from fraud.event_inbox where event_type = 'payment.completed.v1' and payload->>'payerAccountId' = ? and payload->>'currency' = ? and received_at >= CURRENT_TIMESTAMP - (? * INTERVAL '1 minute')",
                BigDecimal.class,
                request.accountId().toString(),
                request.currency(),
                windowMinutes);
        return volume != null && volume.add(request.amount()).compareTo(maxVolume) > 0
                ? "account exceeded its transaction volume limit"
                : null;
    }

    private boolean isBlocked(UUID accountId, JsonNode parameters) {
        for (JsonNode blockedId : parameters.get("accountIds")) {
            if (accountId.toString().equalsIgnoreCase(blockedId.asText())) {
                return true;
            }
        }
        return false;
    }
}
