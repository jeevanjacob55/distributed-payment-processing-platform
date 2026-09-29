package com.paymentplatform.notification.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
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

    @KafkaListener(topics = "${notification.events.lifecycle-topic:payment.lifecycle.v1}")
    public void consume(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) throws Exception {
        JsonNode event = objectMapper.readTree(record.value());
        inboxProcessor.store(UUID.fromString(event.required("eventId").asText()),
                event.required("eventType").asText(), record.value());
        acknowledgment.acknowledge();
    }
}
