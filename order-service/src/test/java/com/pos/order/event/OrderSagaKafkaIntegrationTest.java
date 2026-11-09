package com.pos.order.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.order.dto.OrderItemRequestDto;
import com.pos.order.dto.OrderRequestDto;
import com.pos.order.dto.OrderResponseDto;
import com.pos.order.entity.Order;
import com.pos.order.entity.OrderStatus;
import com.pos.order.repository.OrderRepository;
import com.pos.order.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.listener.auto-startup=true"
})
@EmbeddedKafka(partitions = 1, topics = {
        "order-created-topic",
        "inventory-reserved-topic",
        "inventory-failed-topic"
})
@ActiveProfiles("test")
@Import(OrderSagaKafkaIntegrationTest.SimulatedInventorySagaWorker.class)
@DirtiesContext
class OrderSagaKafkaIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SimulatedInventorySagaWorker inventoryWorker;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        inventoryWorker.reset();
    }

    @Test
    void fullSagaFlow_CreatesPendingOrder_DeductsStock_AndUpdatesOrderStatusToSuccess() {
        inventoryWorker.setStock("LAPTOP-MBP-14", 10);

        OrderRequestDto request = OrderRequestDto.builder()
                .discountAmount(new BigDecimal("500000.00"))
                .items(List.of(OrderItemRequestDto.builder()
                        .sku("LAPTOP-MBP-14")
                        .productName("MacBook Pro 14")
                        .quantity(2)
                        .price(new BigDecimal("25000000.00"))
                        .build()))
                .build();

        OrderResponseDto createdOrder = orderService.createOrder(request, "CASHIER-01");
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        Long orderId = createdOrder.getId();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(inventoryWorker.getStock("LAPTOP-MBP-14")).isEqualTo(8);

            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.SUCCESS);
            assertThat(updatedOrder.getFinalAmount()).isEqualByComparingTo("49500000.00");
        });
    }

    @Test
    void fullSagaFlow_UpdatesOrderStatusToFailed_WhenOutOfStockOccurs() {
        inventoryWorker.setStock("LAPTOP-MBP-14", 1);

        OrderRequestDto request = OrderRequestDto.builder()
                .discountAmount(BigDecimal.ZERO)
                .items(List.of(OrderItemRequestDto.builder()
                        .sku("LAPTOP-MBP-14")
                        .productName("MacBook Pro 14")
                        .quantity(5)
                        .price(new BigDecimal("25000000.00"))
                        .build()))
                .build();

        OrderResponseDto createdOrder = orderService.createOrder(request, "CASHIER-01");
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        Long orderId = createdOrder.getId();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(inventoryWorker.getStock("LAPTOP-MBP-14")).isEqualTo(1);

            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(updatedOrder.getFailureReason()).contains("Insufficient stock for SKU 'LAPTOP-MBP-14'");
        });
    }

    @Test
    void fullSagaFlow_UpdatesOrderStatusToFailed_WhenOptimisticLockingFailureOccurs() {
        inventoryWorker.setStock("LAPTOP-MBP-14", 10);
        inventoryWorker.setSimulateOptimisticLockFailure(true);

        OrderRequestDto request = OrderRequestDto.builder()
                .discountAmount(BigDecimal.ZERO)
                .items(List.of(OrderItemRequestDto.builder()
                        .sku("LAPTOP-MBP-14")
                        .productName("MacBook Pro 14")
                        .quantity(1)
                        .price(new BigDecimal("25000000.00"))
                        .build()))
                .build();

        OrderResponseDto createdOrder = orderService.createOrder(request, "CASHIER-02");
        assertThat(createdOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        Long orderId = createdOrder.getId();

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Order updatedOrder = orderRepository.findById(orderId).orElseThrow();
            assertThat(updatedOrder.getStatus()).isEqualTo(OrderStatus.FAILED);
            assertThat(updatedOrder.getFailureReason()).contains("ObjectOptimisticLockingFailureException");
        });
    }

    @Component
    static class SimulatedInventorySagaWorker {

        private final Map<String, Integer> stockStore = new ConcurrentHashMap<>();
        private final AtomicBoolean simulateOptimisticLockFailure = new AtomicBoolean(false);
        private final KafkaTemplate<String, String> kafkaTemplate;
        private final ObjectMapper objectMapper;

        public SimulatedInventorySagaWorker(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
            this.kafkaTemplate = kafkaTemplate;
            this.objectMapper = objectMapper;
        }

        @KafkaListener(topics = "order-created-topic", groupId = "test-simulated-inventory-group")
        public void handleOrderCreated(String payload) throws Exception {
            OrderCreatedEvent event = objectMapper.readValue(payload, OrderCreatedEvent.class);
            try {
                if (simulateOptimisticLockFailure.get()) {
                    throw new ObjectOptimisticLockingFailureException("Stock", event.getOrderId());
                }

                for (OrderItemEventDto item : event.getItems()) {
                    int available = stockStore.getOrDefault(item.getSku(), 0);
                    if (available < item.getQuantity()) {
                        throw new IllegalArgumentException(
                                "Insufficient stock for SKU '" + item.getSku() + "'. Available: "
                                        + available + ", requested: " + item.getQuantity());
                    }
                    stockStore.put(item.getSku(), available - item.getQuantity());
                }

                InventoryReservedEvent reserved = InventoryReservedEvent.builder()
                        .orderId(event.getOrderId())
                        .orderNumber(event.getOrderNumber())
                        .status("SUCCESS")
                        .message("All items reserved successfully")
                        .build();
                kafkaTemplate.send("inventory-reserved-topic",
                        String.valueOf(event.getOrderId()),
                        objectMapper.writeValueAsString(reserved));

            } catch (Exception ex) {
                InventoryFailedEvent failed = InventoryFailedEvent.builder()
                        .orderId(event.getOrderId())
                        .orderNumber(event.getOrderNumber())
                        .status("FAILED")
                        .reason(ex.getClass().getSimpleName() + ": " + ex.getMessage())
                        .build();
                kafkaTemplate.send("inventory-failed-topic",
                        String.valueOf(event.getOrderId()),
                        objectMapper.writeValueAsString(failed));
            }
        }

        public void setStock(String sku, int quantity) {
            stockStore.put(sku, quantity);
        }

        public int getStock(String sku) {
            return stockStore.getOrDefault(sku, 0);
        }

        public void setSimulateOptimisticLockFailure(boolean value) {
            simulateOptimisticLockFailure.set(value);
        }

        public void reset() {
            stockStore.clear();
            simulateOptimisticLockFailure.set(false);
        }
    }
}
