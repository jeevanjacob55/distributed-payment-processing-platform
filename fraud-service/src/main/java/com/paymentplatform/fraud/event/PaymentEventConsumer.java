package com.paymentplatform.fraud.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {
    private final ObjectMapper objectMapper;
    private final EventInboxProcessor inboxProcessor;

    public PaymentEventConsumer(ObjectMapper objectMapper, EventInboxProcessor inboxProcessor) {
        this.objectMapper = objectMapper;
        this.inboxProcessor = inboxProcessor;
    }

    @KafkaListener(topics = "${fraud.events.lifecycle-topic:payment.lifecycle.v1}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        JsonNode event = objectMapper.readTree(record.value());
        inboxProcessor.process(event, record.value());
        acknowledgment.acknowledge();
    }
}
