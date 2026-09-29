package com.paymentplatform.payment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "outbox_events", schema = "payment")
public class OutboxEvent {
    @Id private UUID id;
    @Column(name = "aggregate_id", nullable = false) private UUID aggregateId;
    @Column(name = "event_type", nullable = false, length = 100) private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;
    @Column(name = "occurred_at", nullable = false, updatable = false) private Instant occurredAt;
    @Column(name = "published_at") private Instant publishedAt;
    @Column(name = "attempts", nullable = false) private int attempts;
    @Column(name = "last_error", length = 1000) private String lastError;

    protected OutboxEvent() {}

    public static OutboxEvent create(UUID aggregateId, String eventType, String payload, Instant occurredAt) {
        OutboxEvent event = new OutboxEvent();
        event.id = UUID.randomUUID();
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.payload = payload;
        event.occurredAt = occurredAt;
        event.attempts = 0;
        return event;
    }

    public UUID getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public int getAttempts() { return attempts; }

    public void markPublished() {
        publishedAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        attempts++;
        lastError = error.length() > 1000 ? error.substring(0, 1000) : error;
    }
}
