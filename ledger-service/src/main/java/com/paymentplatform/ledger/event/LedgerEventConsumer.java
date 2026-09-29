package com.paymentplatform.ledger.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LedgerEventConsumer {
    private final ObjectMapper objectMapper;
    private final LedgerEventProcessor eventProcessor;

    public LedgerEventConsumer(ObjectMapper objectMapper, LedgerEventProcessor eventProcessor) {
        this.objectMapper = objectMapper;
        this.eventProcessor = eventProcessor;
    }

    @KafkaListener(topics = "${ledger.events.lifecycle-topic:payment.lifecycle.v1}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        PaymentLifecycleEvent event = objectMapper.readValue(record.value(), PaymentLifecycleEvent.class);
        eventProcessor.process(event);
        acknowledgment.acknowledge();
    }
}
