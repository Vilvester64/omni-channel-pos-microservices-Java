package com.pos.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryEventConsumer {

    public static final String ORDER_CREATED_TOPIC = "order-created-topic";

    private final InventorySagaProcessor sagaProcessor;
    private final ObjectMapper objectMapper;

    public InventoryEventConsumer(InventorySagaProcessor sagaProcessor, ObjectMapper objectMapper) {
        this.sagaProcessor = sagaProcessor;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = ORDER_CREATED_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:inventory-group}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    public void consumeOrderCreated(String payload) {
        log.info("Received message on {}: {}", ORDER_CREATED_TOPIC, payload);
        try {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            sagaProcessor.processOrderCreated(event);
        } catch (Exception e) {
            log.error("Failed to parse or process order created event: {}", e.getMessage(), e);
        }
    }
}
