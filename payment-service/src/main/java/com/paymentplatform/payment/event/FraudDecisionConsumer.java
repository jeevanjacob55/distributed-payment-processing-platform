package com.paymentplatform.payment.event;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class FraudDecisionConsumer {
    private final FraudDecisionProcessor processor;

    public FraudDecisionConsumer(FraudDecisionProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(topics = "${payment.events.fraud-decision-topic:payment.fraud-decision.v1}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        processor.store(record.value());
        acknowledgment.acknowledge();
    }
}
