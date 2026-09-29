package com.paymentplatform.fraud.event;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventInboxProcessor {
    private final JdbcTemplate jdbcTemplate;

    public EventInboxProcessor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void store(UUID eventId, String eventType, String payload) {
        jdbcTemplate.update(
                "insert into fraud.event_inbox (event_id, event_type, payload) values (?, ?, ?::jsonb) on conflict (event_id) do nothing",
                eventId,
                eventType,
                payload);
    }
}
