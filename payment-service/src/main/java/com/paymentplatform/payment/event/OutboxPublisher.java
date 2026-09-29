package com.paymentplatform.payment.event;

import com.paymentplatform.payment.domain.OutboxEvent;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxPublisher {
    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final int batchSize;

    public OutboxPublisher(
            OutboxEventRepository outboxRepository,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${payment.events.lifecycle-topic:payment.lifecycle.v1}") String topic,
            @Value("${payment.outbox.batch-size:50}") int batchSize) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.poll-interval:PT1S}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> events = outboxRepository.lockNextBatch(batchSize);
        for (OutboxEvent event : events) {
            try {
                kafkaTemplate.send(topic, event.getAggregateId().toString(), event.getPayload())
                        .get(Duration.ofSeconds(10).toMillis(), TimeUnit.MILLISECONDS);
                event.markPublished();
            } catch (Exception exception) {
                event.markFailed(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
                log.warn("Outbox event {} publish attempt {} failed", event.getId(), event.getAttempts());
            }
        }
    }
}
