package com.pos.order.service;

import com.pos.order.dto.OrderRequestDto;
import com.pos.order.dto.OrderResponseDto;

import java.util.List;

public interface OrderService {

    OrderResponseDto createOrder(OrderRequestDto request, String cashierId);

    OrderResponseDto getOrderById(Long id);

    OrderResponseDto getOrderByOrderNumber(String orderNumber);

    List<OrderResponseDto> getAllOrders();

    void handleInventoryReserved(Long orderId);

    void handleInventoryFailed(Long orderId, String reason);
}
