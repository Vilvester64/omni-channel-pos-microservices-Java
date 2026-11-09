package com.pos.order.service;

import com.pos.order.dto.OrderItemRequestDto;
import com.pos.order.dto.OrderRequestDto;
import com.pos.order.dto.OrderResponseDto;
import com.pos.order.entity.Order;
import com.pos.order.entity.OrderStatus;
import com.pos.order.event.OrderCreatedEvent;
import com.pos.order.event.OrderEventProducer;
import com.pos.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer eventProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    private OrderRequestDto sampleRequest;

    @BeforeEach
    void setUp() {
        sampleRequest = OrderRequestDto.builder()
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .sku("SKU-1")
                                .productName("Product 1")
                                .price(new BigDecimal("10000.00"))
                                .quantity(2)
                                .build(),
                        OrderItemRequestDto.builder()
                                .sku("SKU-2")
                                .productName("Product 2")
                                .price(new BigDecimal("25000.00"))
                                .quantity(3)
                                .build()
                ))
                .discountAmount(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    void createOrder_CreatesPendingOrderAndPublishesEvent_WhenValidInput() {
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        OrderResponseDto response = orderService.createOrder(sampleRequest, "cashier-101");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("95000.00"));
        assertThat(response.getDiscountAmount()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(new BigDecimal("90000.00"));

        ArgumentCaptor<OrderCreatedEvent> captor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(eventProducer, times(1)).publishOrderCreated(captor.capture());

        OrderCreatedEvent published = captor.getValue();
        assertThat(published.getOrderId()).isEqualTo(10L);
        assertThat(published.getTotalAmount()).isEqualByComparingTo(new BigDecimal("90000.00"));
        assertThat(published.getItems()).hasSize(2);
    }

    @Test
    void createOrder_CalculatesTotalsAndDiscountCorrectly_WhenDiscountExceedsTotal() {
        OrderRequestDto heavyDiscount = OrderRequestDto.builder()
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .sku("SKU-1")
                                .productName("Product 1")
                                .price(new BigDecimal("10000.00"))
                                .quantity(1)
                                .build()
                ))
                .discountAmount(new BigDecimal("50000.00"))
                .build();

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(11L);
            return o;
        });

        OrderResponseDto response = orderService.createOrder(heavyDiscount, "cashier-101");

        assertThat(response.getTotalAmount()).isEqualByComparingTo(new BigDecimal("10000.00"));
        assertThat(response.getFinalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void createOrder_ThrowsIllegalArgumentException_WhenItemsAreEmpty() {
        OrderRequestDto emptyRequest = OrderRequestDto.builder()
                .items(Collections.emptyList())
                .build();

        assertThatThrownBy(() -> orderService.createOrder(emptyRequest, "cashier-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");

        verify(orderRepository, never()).save(any());
        verify(eventProducer, never()).publishOrderCreated(any());
    }

    @Test
    void handleInventoryReserved_UpdatesStatusToSuccess_WhenOrderPending() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50000.00"))
                .finalAmount(new BigDecimal("50000.00"))
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.handleInventoryReserved(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.SUCCESS);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void handleInventoryReserved_IgnoresEvent_WhenOrderNotPending() {
        Order order = Order.builder()
                .id(1L)
                .status(OrderStatus.FAILED)
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        orderService.handleInventoryReserved(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void handleInventoryFailed_UpdatesStatusToFailedAndSetsReason_WhenOrderPending() {
        Order order = Order.builder()
                .id(2L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("50000.00"))
                .finalAmount(new BigDecimal("50000.00"))
                .build();

        when(orderRepository.findById(2L)).thenReturn(Optional.of(order));

        orderService.handleInventoryFailed(2L, "Out of stock for SKU-999");

        assertThat(order.getStatus()).isEqualTo(OrderStatus.FAILED);
        assertThat(order.getFailureReason()).isEqualTo("Out of stock for SKU-999");
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void getOrderById_ThrowsEntityNotFoundException_WhenNotFound() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Order not found with id: 999");
    }
}

