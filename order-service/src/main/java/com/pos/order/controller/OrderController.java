package com.pos.order.controller;

import com.pos.order.dto.ApiResponse;
import com.pos.order.dto.OrderRequestDto;
import com.pos.order.dto.OrderResponseDto;
import com.pos.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/order/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponseDto>> createOrder(
            @Valid @RequestBody OrderRequestDto request,
            @RequestHeader(value = "X-User-ID", required = false) String userId,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyCashierOrAdminRoleIfPresent(role);
        String cashier = (userId != null && !userId.isBlank()) ? userId : "ANONYMOUS";
        OrderResponseDto created = orderService.createOrder(request, cashier);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(created, "Order created with status PENDING"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponseDto>>> getAllOrders() {
        List<OrderResponseDto> orders = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.success(orders, "Orders retrieved successfully"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderById(@PathVariable Long id) {
        OrderResponseDto order = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    @GetMapping("/number/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponseDto>> getOrderByNumber(@PathVariable String orderNumber) {
        OrderResponseDto order = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(ApiResponse.success(order, "Order retrieved successfully"));
    }

    private void verifyCashierOrAdminRoleIfPresent(String role) {
        if (role != null && !role.isBlank()) {
            boolean hasPermission = role.contains("ROLE_CASHIER") || role.contains("ROLE_ADMIN");
            if (!hasPermission) {
                throw new SecurityException("Access denied: ROLE_CASHIER or ROLE_ADMIN required");
            }
        }
    }
}
