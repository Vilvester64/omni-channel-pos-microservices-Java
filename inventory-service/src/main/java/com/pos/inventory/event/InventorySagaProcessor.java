package com.pos.inventory.event;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class InventorySagaProcessor {

    private final StockService stockService;
    private final InventoryEventProducer producer;

    public InventorySagaProcessor(StockService stockService, InventoryEventProducer producer) {
        this.stockService = stockService;
        this.producer = producer;
    }

    public void processOrderCreated(OrderCreatedEvent event) {
        log.info("Processing OrderCreatedEvent for orderId: {}", event.getOrderId());
        List<OrderItemEventDto> items = event.getItems();
        List<StockAdjustmentDto> successfullyDeducted = new ArrayList<>();

        try {
            if (items == null || items.isEmpty()) {
                throw new IllegalArgumentException("Order has no items");
            }

            for (OrderItemEventDto item : items) {
                StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                        .sku(item.getSku())
                        .branchCode("MAIN")
                        .quantity(item.getQuantity())
                        .reason("Order checkout " + event.getOrderNumber())
                        .build();

                stockService.deductStock(adjustment);
                successfullyDeducted.add(adjustment);
            }

            InventoryReservedEvent reservedEvent = InventoryReservedEvent.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .status("SUCCESS")
                    .message("All items reserved successfully")
                    .build();
            producer.sendInventoryReserved(reservedEvent);

        } catch (Exception ex) {
            log.warn("Inventory reservation failed for order {}: {}. Compensating previously deducted items...",
                    event.getOrderId(), ex.getMessage());

            for (StockAdjustmentDto comp : successfullyDeducted) {
                try {
                    stockService.addStock(StockAdjustmentDto.builder()
                            .sku(comp.getSku())
                            .branchCode(comp.getBranchCode())
                            .quantity(comp.getQuantity())
                            .reason("Compensation for failed order " + event.getOrderNumber())
                            .build());
                } catch (Exception rollEx) {
                    log.error("Failed to compensate stock for SKU {}: {}", comp.getSku(), rollEx.getMessage());
                }
            }

            InventoryFailedEvent failedEvent = InventoryFailedEvent.builder()
                    .orderId(event.getOrderId())
                    .orderNumber(event.getOrderNumber())
                    .status("FAILED")
                    .reason(ex.getMessage())
                    .build();
            producer.sendInventoryFailed(failedEvent);
        }
    }
}
