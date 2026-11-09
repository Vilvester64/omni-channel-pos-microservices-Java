package com.pos.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.order.dto.OrderItemRequestDto;
import com.pos.order.dto.OrderRequestDto;
import com.pos.order.event.OrderEventProducer;
import com.pos.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @MockBean
    private OrderEventProducer eventProducer;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
    }

    @Test
    void createOrder_Returns201AndPendingStatus_WhenValidInput() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .items(List.of(
                        OrderItemRequestDto.builder()
                                .sku("SKU-101")
                                .productName("Mechanical Keyboard")
                                .price(new BigDecimal("500000.00"))
                                .quantity(1)
                                .build()
                ))
                .discountAmount(new BigDecimal("50000.00"))
                .build();

        mockMvc.perform(post("/api/order/orders")
                        .header("X-User-ID", "cashier-007")
                        .header("X-User-Role", "ROLE_CASHIER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.cashierId").value("cashier-007"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.totalAmount").value(500000.0))
                .andExpect(jsonPath("$.data.discountAmount").value(50000.0))
                .andExpect(jsonPath("$.data.finalAmount").value(450000.0));
    }

    @Test
    void createOrder_Returns400_WhenItemsEmpty() throws Exception {
        OrderRequestDto request = OrderRequestDto.builder()
                .items(Collections.emptyList())
                .build();

        mockMvc.perform(post("/api/order/orders")
                        .header("X-User-Role", "ROLE_CASHIER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void getOrderById_Returns404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/order/orders/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }
}
