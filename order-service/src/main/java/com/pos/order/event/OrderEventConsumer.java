package com.pos.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.order.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderEventConsumer {

    public static final String INVENTORY_RESERVED_TOPIC = "inventory-reserved-topic";
    public static final String INVENTORY_FAILED_TOPIC = "inventory-failed-topic";

    private final OrderService orderService;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(OrderService orderService, ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = INVENTORY_RESERVED_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:order-group}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    public void consumeInventoryReserved(String payload) {
        log.info("Received message on {}: {}", INVENTORY_RESERVED_TOPIC, payload);
        try {
            InventoryReservedEvent event = objectMapper.readValue(payload, InventoryReservedEvent.class);
            orderService.handleInventoryReserved(event.getOrderId());
        } catch (Exception e) {
            log.error("Failed to parse or process InventoryReservedEvent: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(
            topics = INVENTORY_FAILED_TOPIC,
            groupId = "${spring.kafka.consumer.group-id:order-group}",
            autoStartup = "${spring.kafka.listener.auto-startup:true}"
    )
    public void consumeInventoryFailed(String payload) {
        log.info("Received message on {}: {}", INVENTORY_FAILED_TOPIC, payload);
        try {
            InventoryFailedEvent event = objectMapper.readValue(payload, InventoryFailedEvent.class);
            orderService.handleInventoryFailed(event.getOrderId(), event.getReason());
        } catch (Exception e) {
            log.error("Failed to parse or process InventoryFailedEvent: {}", e.getMessage(), e);
        }
    }
}
