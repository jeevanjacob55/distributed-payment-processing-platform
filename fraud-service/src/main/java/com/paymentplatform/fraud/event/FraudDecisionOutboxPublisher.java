package com.paymentplatform.fraud.event;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FraudDecisionOutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(FraudDecisionOutboxPublisher.class);
    private final JdbcTemplate jdbcTemplate;
    private final KafkaTemplate<Object, Object> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public FraudDecisionOutboxPublisher(
            JdbcTemplate jdbcTemplate,
            KafkaTemplate<Object, Object> kafkaTemplate,
            @Value("${fraud.events.decision-topic:payment.fraud-decision.v1}") String topic,
            @Value("${fraud.outbox.batch-size:50}") int batchSize) {
        this.jdbcTemplate = jdbcTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${fraud.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishPending() {
        List<OutboxRow> events = jdbcTemplate.query(
                "select id, aggregate_id, payload, attempts from fraud.fraud_outbox_events where published_at is null order by occurred_at, id limit ? for update skip locked",
                (rs, rowNum) -> new OutboxRow(
                        rs.getObject("id", UUID.class),
                        rs.getObject("aggregate_id", UUID.class),
                        rs.getString("payload"),
                        rs.getInt("attempts")),
                batchSize);
        for (OutboxRow event : events) {
            try {
                kafkaTemplate.send(topic, event.aggregateId().toString(), event.payload())
                        .get(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
                jdbcTemplate.update(
                        "update fraud.fraud_outbox_events set published_at = CURRENT_TIMESTAMP, last_error = null where id = ?",
                        event.id());
            } catch (Exception exception) {
                String message = exception.getMessage() == null
                        ? exception.getClass().getSimpleName()
                        : exception.getMessage();
                jdbcTemplate.update(
                        "update fraud.fraud_outbox_events set attempts = attempts + 1, last_error = ? where id = ?",
                        message.length() > 1000 ? message.substring(0, 1000) : message,
                        event.id());
                log.warn("Fraud decision event {} publish attempt {} failed", event.id(), event.attempts() + 1);
            }
        }
    }

    private record OutboxRow(UUID id, UUID aggregateId, String payload, int attempts) {}
}
