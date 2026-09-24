package com.paymentplatform.payment.event;

import com.paymentplatform.payment.domain.Payment;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventPublisher {
    private final KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate;
    private final String paymentCompletedTopic;

    public PaymentEventPublisher(
            KafkaTemplate<String, PaymentCompletedEvent> kafkaTemplate,
            @Value("${payment.events.completed-topic:payment.completed}") String paymentCompletedTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.paymentCompletedTopic = paymentCompletedTopic;
    }

    public void publishCompleted(Payment payment) {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getPayerAccount().getId(),
                payment.getPayeeAccount().getId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getMerchantReference(),
                payment.getUpdatedAt());
        kafkaTemplate.send(paymentCompletedTopic, payment.getId().toString(), event);
    }
}
