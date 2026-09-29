package com.paymentplatform.payment.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.api.CreatePaymentRequest;
import com.paymentplatform.payment.domain.OutboxEvent;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentFailureEventRecorder {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentFailureEventRecorder(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(CreatePaymentRequest request) {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();
        String eventType = "payment.failed.v1";
        PaymentLifecycleEvent event = new PaymentLifecycleEvent(
                eventId,
                1,
                eventType,
                null,
                request.payerAccountId(),
                request.payeeAccountId(),
                request.amount(),
                request.currency(),
                request.merchantReference(),
                "FAILED",
                occurredAt);
        try {
            outboxRepository.save(OutboxEvent.create(
                    eventId, eventType, objectMapper.writeValueAsString(event), occurredAt));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize failed payment event", exception);
        }
    }
}
