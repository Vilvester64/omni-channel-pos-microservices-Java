package com.pos.order.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventProducer {

    public static final String ORDER_CREATED_TOPIC = "order-created-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OrderEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing to {}: {}", ORDER_CREATED_TOPIC, payload);
            kafkaTemplate.send(ORDER_CREATED_TOPIC, String.valueOf(event.getOrderId()), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderCreatedEvent: {}", e.getMessage(), e);
        }
    }
}
