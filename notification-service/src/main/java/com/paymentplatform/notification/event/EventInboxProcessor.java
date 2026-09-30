package com.paymentplatform.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventInboxProcessor {
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public EventInboxProcessor(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void store(UUID eventId, String eventType, String payload) {
        int inserted = jdbcTemplate.update(
                "insert into notification.event_inbox (event_id, event_type, payload) values (?, ?, ?::jsonb) on conflict (event_id) do nothing",
                eventId,
                eventType,
                payload);
        if (inserted == 0 || !(eventType.equals("payment.completed.v1")
                || eventType.equals("payment.failed.v1")
                || eventType.equals("payment.reversed.v1")
                || eventType.equals("refund.completed.v1"))) {
            return;
        }

        try {
            JsonNode event = objectMapper.readTree(payload);
            UUID paymentId = UUID.fromString(event.required("paymentId").asText());
            String status = event.required("status").asText();
            String message = "Payment " + paymentId + " is " + status.toLowerCase(Locale.ROOT) + ".";
            insertNotification(eventId, paymentId, event, "payerAccountId", message);
            insertNotification(eventId, paymentId, event, "payeeAccountId", message);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid payment lifecycle event", exception);
        }
    }

    private void insertNotification(
            UUID eventId, UUID paymentId, JsonNode event, String accountField, String message) {
        JsonNode account = event.get(accountField);
        if (account == null || account.isNull()) {
            return;
        }
        jdbcTemplate.update(
                "insert into notification.notifications (event_id, payment_id, account_id, event_type, message) values (?, ?, ?, ?, ?) on conflict (event_id, account_id) do nothing",
                eventId, paymentId, UUID.fromString(account.asText()), event.required("eventType").asText(), message);
    }
}
