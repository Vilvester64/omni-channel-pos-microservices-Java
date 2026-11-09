package com.pos.order.service;

import com.pos.order.dto.*;
import com.pos.order.entity.Order;
import com.pos.order.entity.OrderItem;
import com.pos.order.entity.OrderStatus;
import com.pos.order.event.OrderCreatedEvent;
import com.pos.order.event.OrderEventProducer;
import com.pos.order.event.OrderItemEventDto;
import com.pos.order.repository.OrderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderServiceImpl(OrderRepository orderRepository, OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    @Override
    public OrderResponseDto createOrder(OrderRequestDto request, String cashierId) {
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        List<OrderItemEventDto> eventItems = new ArrayList<>();

        for (OrderItemRequestDto itemDto : request.getItems()) {
            BigDecimal subtotal = itemDto.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            OrderItem item = OrderItem.builder()
                    .sku(itemDto.getSku())
                    .productName(itemDto.getProductName())
                    .price(itemDto.getPrice())
                    .quantity(itemDto.getQuantity())
                    .subtotal(subtotal)
                    .build();
            items.add(item);

            eventItems.add(OrderItemEventDto.builder()
                    .sku(itemDto.getSku())
                    .productName(itemDto.getProductName())
                    .price(itemDto.getPrice())
                    .quantity(itemDto.getQuantity())
                    .build());
        }

        BigDecimal discount = request.getDiscountAmount() != null ? request.getDiscountAmount() : BigDecimal.ZERO;
        BigDecimal finalAmount = totalAmount.subtract(discount).max(BigDecimal.ZERO);

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .cashierId(cashierId)
                .totalAmount(totalAmount)
                .discountAmount(discount)
                .finalAmount(finalAmount)
                .status(OrderStatus.PENDING)
                .build();

        for (OrderItem item : items) {
            order.addItem(item);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Created order {} with status PENDING, finalAmount: {}", savedOrder.getId(), finalAmount);

        OrderCreatedEvent event = OrderCreatedEvent.builder()
                .orderId(savedOrder.getId())
                .orderNumber(savedOrder.getOrderNumber())
                .cashierId(cashierId)
                .items(eventItems)
                .totalAmount(finalAmount)
                .build();

        eventProducer.publishOrderCreated(event);

        return mapToDto(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with id: " + id));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new EntityNotFoundException("Order not found with orderNumber: " + orderNumber));
        return mapToDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream().map(this::mapToDto).toList();
    }

    @Override
    public void handleInventoryReserved(Long orderId) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.SUCCESS);
                orderRepository.save(order);
                log.info("Order {} status updated to SUCCESS", orderId);
            } else {
                log.warn("Order {} not in PENDING status (current: {}), ignoring reservation",
                        orderId, order.getStatus());
            }
        }, () -> log.error("Order {} not found for inventory reserved event", orderId));
    }

    @Override
    public void handleInventoryFailed(Long orderId, String reason) {
        orderRepository.findById(orderId).ifPresentOrElse(order -> {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.FAILED);
                order.setFailureReason(reason);
                orderRepository.save(order);
                log.info("Order {} status updated to FAILED. Reason: {}", orderId, reason);
            } else {
                log.warn("Order {} not in PENDING status (current: {}), ignoring failure event",
                        orderId, order.getStatus());
            }
        }, () -> log.error("Order {} not found for inventory failed event", orderId));
    }

    private OrderResponseDto mapToDto(Order order) {
        List<OrderItemResponseDto> itemDtos = order.getItems().stream()
                .map(item -> OrderItemResponseDto.builder()
                        .id(item.getId())
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .price(item.getPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build())
                .toList();

        return OrderResponseDto.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .cashierId(order.getCashierId())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .status(order.getStatus())
                .failureReason(order.getFailureReason())
                .items(itemDtos)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}

