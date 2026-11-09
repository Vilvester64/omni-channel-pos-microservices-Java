package com.pos.inventory.event;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockResponseDto;
import com.pos.inventory.service.StockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventorySagaProcessorTest {

    @Mock
    private StockService stockService;

    @Mock
    private InventoryEventProducer producer;

    @InjectMocks
    private InventorySagaProcessor sagaProcessor;

    private OrderCreatedEvent orderCreatedEvent;

    @BeforeEach
    void setUp() {
        orderCreatedEvent = OrderCreatedEvent.builder()
                .orderId(101L)
                .orderNumber("ORD-20261107-101")
                .cashierId("cashier-1")
                .totalAmount(new BigDecimal("200000.00"))
                .items(List.of(
                        OrderItemEventDto.builder()
                                .sku("SKU-A")
                                .productName("Item A")
                                .price(new BigDecimal("50000.00"))
                                .quantity(2)
                                .build(),
                        OrderItemEventDto.builder()
                                .sku("SKU-B")
                                .productName("Item B")
                                .price(new BigDecimal("100000.00"))
                                .quantity(1)
                                .build()
                ))
                .build();
    }

    @Test
    void processOrderCreated_SendsInventoryReserved_WhenAllItemsHaveSufficientStock() {
        when(stockService.deductStock(any(StockAdjustmentDto.class)))
                .thenReturn(StockResponseDto.builder().quantity(10).build());

        sagaProcessor.processOrderCreated(orderCreatedEvent);

        verify(stockService, times(2)).deductStock(any(StockAdjustmentDto.class));
        ArgumentCaptor<InventoryReservedEvent> captor = ArgumentCaptor.forClass(InventoryReservedEvent.class);
        verify(producer, times(1)).sendInventoryReserved(captor.capture());
        verify(producer, never()).sendInventoryFailed(any());

        InventoryReservedEvent reserved = captor.getValue();
        assertThat(reserved.getOrderId()).isEqualTo(101L);
        assertThat(reserved.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    void processOrderCreated_CompensatesAndSendsInventoryFailed_WhenStockIsInsufficient() {
        when(stockService.deductStock(argThat(dto -> dto != null && "SKU-A".equals(dto.getSku()))))
                .thenReturn(StockResponseDto.builder().quantity(8).build());
        when(stockService.deductStock(argThat(dto -> dto != null && "SKU-B".equals(dto.getSku()))))
                .thenThrow(new IllegalArgumentException("Insufficient stock for SKU 'SKU-B'"));

        sagaProcessor.processOrderCreated(orderCreatedEvent);

        ArgumentCaptor<StockAdjustmentDto> compCaptor = ArgumentCaptor.forClass(StockAdjustmentDto.class);
        verify(stockService, times(1)).addStock(compCaptor.capture());
        assertThat(compCaptor.getValue().getSku()).isEqualTo("SKU-A");
        assertThat(compCaptor.getValue().getQuantity()).isEqualTo(2);

        ArgumentCaptor<InventoryFailedEvent> failedCaptor = ArgumentCaptor.forClass(InventoryFailedEvent.class);
        verify(producer, times(1)).sendInventoryFailed(failedCaptor.capture());
        verify(producer, never()).sendInventoryReserved(any());

        InventoryFailedEvent failed = failedCaptor.getValue();
        assertThat(failed.getOrderId()).isEqualTo(101L);
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getReason()).contains("Insufficient stock for SKU 'SKU-B'");
    }
}
