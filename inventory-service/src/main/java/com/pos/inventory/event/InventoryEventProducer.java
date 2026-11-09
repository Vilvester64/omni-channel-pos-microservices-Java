package com.pos.inventory.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class InventoryEventProducer {

    public static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved-topic";
    public static final String INVENTORY_FAILED_TOPIC = "inventory-failed-topic";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public InventoryEventProducer(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendInventoryReserved(InventoryReservedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing to {}: {}", INVENTORY_RESERVED_TOPIC, payload);
            kafkaTemplate.send(INVENTORY_RESERVED_TOPIC, String.valueOf(event.getOrderId()), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize InventoryReservedEvent: {}", e.getMessage(), e);
        }
    }

    public void sendInventoryFailed(InventoryFailedEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            log.info("Publishing to {}: {}", INVENTORY_FAILED_TOPIC, payload);
            kafkaTemplate.send(INVENTORY_FAILED_TOPIC, String.valueOf(event.getOrderId()), payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize InventoryFailedEvent: {}", e.getMessage(), e);
        }
    }
}
