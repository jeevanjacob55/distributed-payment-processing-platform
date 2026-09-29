package com.paymentplatform.payment.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudDecisionProcessor {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public FraudDecisionProcessor(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void store(String payload) throws Exception {
        JsonNode event = objectMapper.readTree(payload);
        UUID eventId = UUID.fromString(event.required("eventId").asText());
        UUID paymentId = UUID.fromString(event.required("paymentId").asText());
        UUID sourceEventId = UUID.fromString(event.required("sourceEventId").asText());
        String decision = event.required("decision").asText();
        Instant decidedAt = Instant.parse(event.required("decidedAt").asText());
        jdbcTemplate.update(
                "insert into payment.fraud_decisions (id, payment_id, source_event_id, decision, payload, decided_at) values (?, ?, ?, ?, ?::jsonb, ?) on conflict (source_event_id) do nothing",
                eventId,
                paymentId,
                sourceEventId,
                decision,
                payload,
                Timestamp.from(decidedAt));
    }
}
