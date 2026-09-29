package com.paymentplatform.payment.event;

import com.paymentplatform.payment.domain.Payment;
import com.paymentplatform.payment.domain.OutboxEvent;
import com.paymentplatform.payment.domain.Refund;
import com.paymentplatform.payment.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public PaymentEventPublisher(OutboxEventRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void record(Payment payment, String status) {
        String eventType = "payment." + status.toLowerCase(java.util.Locale.ROOT) + ".v1";
        PaymentLifecycleEvent event = new PaymentLifecycleEvent(
                UUID.randomUUID(),
                1,
                eventType,
                payment.getId(),
                payment.getPayerAccount().getId(),
                payment.getPayeeAccount().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantReference(),
                status,
                Instant.now());
        try {
            outboxRepository.save(OutboxEvent.create(
                    payment.getId(), eventType, objectMapper.writeValueAsString(event), event.occurredAt()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize payment lifecycle event", exception);
        }
    }

    public void recordRefund(Refund refund) {
        Payment payment = refund.getPayment();
        Instant occurredAt = refund.getCreatedAt();
        String eventType = "refund.completed.v1";
        PaymentLifecycleEvent event = new PaymentLifecycleEvent(
                UUID.randomUUID(),
                1,
                eventType,
                payment.getId(),
                payment.getPayerAccount().getId(),
                payment.getPayeeAccount().getId(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getReference(),
                "REFUNDED",
                occurredAt);
        try {
            outboxRepository.save(OutboxEvent.create(
                    payment.getId(), eventType, objectMapper.writeValueAsString(event), occurredAt));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize refund lifecycle event", exception);
        }
    }
}
