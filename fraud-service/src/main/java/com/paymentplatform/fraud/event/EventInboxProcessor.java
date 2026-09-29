package com.paymentplatform.fraud.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.fraud.api.FraudEvaluationRequest;
import com.paymentplatform.fraud.api.FraudEvaluationResponse;
import com.paymentplatform.fraud.service.FraudEvaluationService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventInboxProcessor {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final FraudEvaluationService evaluationService;

    public EventInboxProcessor(
            JdbcTemplate jdbcTemplate, ObjectMapper objectMapper, FraudEvaluationService evaluationService) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.evaluationService = evaluationService;
    }

    @Transactional
    public void process(JsonNode event, String payload) {
        UUID eventId = UUID.fromString(event.required("eventId").asText());
        String eventType = event.required("eventType").asText();
        int inserted = jdbcTemplate.update(
                "insert into fraud.event_inbox (event_id, event_type, payload) values (?, ?, ?::jsonb) on conflict (event_id) do nothing",
                eventId,
                eventType,
                payload);
        if (inserted == 0 || !"payment.created.v1".equals(eventType)) {
            return;
        }

        UUID paymentId = UUID.fromString(event.required("paymentId").asText());
        FraudEvaluationRequest request = new FraudEvaluationRequest(
                UUID.fromString(event.required("payerAccountId").asText()),
                new BigDecimal(event.required("amount").asText()),
                event.required("currency").asText(),
                event.required("merchantReference").asText());
        FraudEvaluationResponse evaluation = evaluationService.evaluateForEvent(request, paymentId);
        Instant decidedAt = Instant.now();
        FraudDecisionEvent decisionEvent = new FraudDecisionEvent(
                UUID.randomUUID(), 1, "fraud.decision.v1", paymentId, eventId,
                evaluation.decision(), evaluation.matches(), decidedAt);
        try {
            String decisionPayload = objectMapper.writeValueAsString(decisionEvent);
            jdbcTemplate.update(
                    "insert into fraud.fraud_decisions (id, payment_id, source_event_id, decision, payload, decided_at) values (?, ?, ?, ?, ?::jsonb, ?)",
                    decisionEvent.eventId(),
                    paymentId,
                    eventId,
                    decisionEvent.decision(),
                    decisionPayload,
                    decidedAt);
            jdbcTemplate.update(
                    "insert into fraud.fraud_outbox_events (id, aggregate_id, payload, occurred_at) values (?, ?, ?::jsonb, ?)",
                    UUID.randomUUID(),
                    paymentId,
                    decisionPayload,
                    decidedAt);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize fraud decision event", exception);
        }
    }
}
