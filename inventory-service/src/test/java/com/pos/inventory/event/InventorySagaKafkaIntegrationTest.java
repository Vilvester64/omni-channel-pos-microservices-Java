package com.pos.inventory.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventory.entity.Stock;
import com.pos.inventory.repository.StockRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.stereotype.Component;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

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
@Import(InventorySagaKafkaIntegrationTest.TestSagaResultListener.class)
@DirtiesContext
class InventorySagaKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestSagaResultListener resultListener;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
        resultListener.clear();
    }

    @Test
    void sagaInventoryConsumer_DeductsStockAndPublishesReservedEvent_WhenStockSufficient() throws Exception {
        stockRepository.saveAndFlush(Stock.builder()
                .sku("LAPTOP-MBP-14")
                .branchCode("MAIN")
                .quantity(10)
                .build());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(501L)
                .orderNumber("ORD-501")
                .items(List.of(OrderItemEventDto.builder()
                        .sku("LAPTOP-MBP-14")
                        .quantity(3)
                        .build()))
                .build();

        kafkaTemplate.send("order-created-topic", "501", objectMapper.writeValueAsString(event));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Stock updatedStock = stockRepository.findBySkuAndBranchCode("LAPTOP-MBP-14", "MAIN").orElseThrow();
            assertThat(updatedStock.getQuantity()).isEqualTo(7);
            assertThat(updatedStock.getVersion()).isEqualTo(1L);

            InventoryReservedEvent reserved = resultListener.getReservedEvents().poll();
            assertThat(reserved).isNotNull();
            assertThat(reserved.getOrderId()).isEqualTo(501L);
            assertThat(reserved.getStatus()).isEqualTo("SUCCESS");
        });
    }

    @Test
    void sagaInventoryConsumer_PublishesFailedEventAndKeepsStockIntact_WhenOutOfStock() throws Exception {
        stockRepository.saveAndFlush(Stock.builder()
                .sku("LAPTOP-MBP-14")
                .branchCode("MAIN")
                .quantity(2)
                .build());

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(502L)
                .orderNumber("ORD-502")
                .items(List.of(OrderItemEventDto.builder()
                        .sku("LAPTOP-MBP-14")
                        .quantity(5)
                        .build()))
                .build();

        kafkaTemplate.send("order-created-topic", "502", objectMapper.writeValueAsString(event));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            Stock unchangedStock = stockRepository.findBySkuAndBranchCode("LAPTOP-MBP-14", "MAIN").orElseThrow();
            assertThat(unchangedStock.getQuantity()).isEqualTo(2);

            InventoryFailedEvent failed = resultListener.getFailedEvents().poll();
            assertThat(failed).isNotNull();
            assertThat(failed.getOrderId()).isEqualTo(502L);
            assertThat(failed.getStatus()).isEqualTo("FAILED");
            assertThat(failed.getReason()).contains("Insufficient stock");
        });
    }

    @Component
    static class TestSagaResultListener {
        private final BlockingQueue<InventoryReservedEvent> reservedEvents = new LinkedBlockingQueue<>();
        private final BlockingQueue<InventoryFailedEvent> failedEvents = new LinkedBlockingQueue<>();
        private final ObjectMapper mapper = new ObjectMapper();

        @KafkaListener(topics = "inventory-reserved-topic", groupId = "test-inv-reserved-verifier")
        public void onReserved(ConsumerRecord<String, String> record) throws Exception {
            reservedEvents.add(mapper.readValue(record.value(), InventoryReservedEvent.class));
        }

        @KafkaListener(topics = "inventory-failed-topic", groupId = "test-inv-failed-verifier")
        public void onFailed(ConsumerRecord<String, String> record) throws Exception {
            failedEvents.add(mapper.readValue(record.value(), InventoryFailedEvent.class));
        }

        public BlockingQueue<InventoryReservedEvent> getReservedEvents() {
            return reservedEvents;
        }

        public BlockingQueue<InventoryFailedEvent> getFailedEvents() {
            return failedEvents;
        }

        public void clear() {
            reservedEvents.clear();
            failedEvents.clear();
        }
    }
}
