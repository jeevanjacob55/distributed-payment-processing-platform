package com.paymentplatform.fraud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.fraud.api.FraudEvaluationRequest;
import com.paymentplatform.fraud.api.FraudRuleResponse;
import com.paymentplatform.fraud.domain.FraudRuleAction;
import com.paymentplatform.fraud.domain.FraudRuleType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class FraudEvaluationServiceTest {
    private FraudRuleService ruleService;
    private FraudEvaluationService service;

    @BeforeEach
    void setUp() {
        ruleService = mock(FraudRuleService.class);
        service = new FraudEvaluationService(ruleService, mock(JdbcTemplate.class));
    }

    @Test
    void blocksAnAmountAboveAConfiguredBlockingThreshold() throws Exception {
        when(ruleService.enabledRules()).thenReturn(List.of(rule(
                FraudRuleType.MAX_AMOUNT, FraudRuleAction.BLOCK, "{\"threshold\":100}")));

        var result = service.evaluate(new FraudEvaluationRequest(
                UUID.randomUUID(), new BigDecimal("100.01"), "USD", "order-1"));

        assertEquals("BLOCKED", result.decision());
        assertEquals("amount-limit", result.matches().getFirst().code());
    }

    @Test
    void approvesWhenNoRulesMatch() {
        when(ruleService.enabledRules()).thenReturn(List.of());

        var result = service.evaluate(new FraudEvaluationRequest(
                UUID.randomUUID(), new BigDecimal("10.00"), "USD", "order-2"));

        assertEquals("APPROVED", result.decision());
        assertEquals(List.of(), result.matches());
    }

    private FraudRuleResponse rule(FraudRuleType type, FraudRuleAction action, String parameters) throws Exception {
        Instant now = Instant.now();
        return new FraudRuleResponse(UUID.randomUUID(), "amount-limit", type, action, true,
                new ObjectMapper().readTree(parameters), now, now);
    }
}
