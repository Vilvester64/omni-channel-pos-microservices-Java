package com.pos.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockRequestDto;
import com.pos.inventory.entity.Stock;
import com.pos.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
    }

    @Test
    void createOrUpdateStock_Returns201AndStandardResponse() throws Exception {
        StockRequestDto request = StockRequestDto.builder()
                .sku("SKU-INT-1")
                .branchCode("MAIN")
                .quantity(100)
                .build();

        mockMvc.perform(post("/api/inventory/stocks")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.data.sku").value("SKU-INT-1"))
                .andExpect(jsonPath("$.data.quantity").value(100))
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void addStock_Returns200AndIncreasedQuantity() throws Exception {
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .sku("SKU-INT-2")
                .branchCode("MAIN")
                .quantity(20)
                .build());

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-INT-2")
                .branchCode("MAIN")
                .quantity(15)
                .reason("Delivery")
                .build();

        mockMvc.perform(post("/api/inventory/stocks/add")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(35));
    }

    @Test
    void deductStock_Returns200AndDecreasedQuantity_WhenStockAvailable() throws Exception {
        stockRepository.saveAndFlush(Stock.builder()
                .sku("SKU-INT-3")
                .branchCode("MAIN")
                .quantity(50)
                .build());

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-INT-3")
                .branchCode("MAIN")
                .quantity(10)
                .reason("Customer checkout")
                .build();

        mockMvc.perform(post("/api/inventory/stocks/deduct")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustment)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.quantity").value(40));
    }

    @Test
    void deductStock_Returns400_WhenStockIsInsufficient() throws Exception {
        stockRepository.saveAndFlush(Stock.builder()
                .sku("SKU-INT-4")
                .branchCode("MAIN")
                .quantity(5)
                .build());

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-INT-4")
                .branchCode("MAIN")
                .quantity(20)
                .reason("Customer checkout")
                .build();

        mockMvc.perform(post("/api/inventory/stocks/deduct")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustment)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void deductStock_Returns403_WhenNonAdminRoleProvided() throws Exception {
        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-INT-5")
                .branchCode("MAIN")
                .quantity(5)
                .build();

        mockMvc.perform(post("/api/inventory/stocks/deduct")
                        .header("X-User-Role", "ROLE_CASHIER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjustment)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }
}
