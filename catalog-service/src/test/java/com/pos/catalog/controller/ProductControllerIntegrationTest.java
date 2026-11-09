package com.pos.catalog.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.catalog.dto.ProductRequestDto;
import com.pos.catalog.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void createProduct_Returns201AndStandardResponse_WhenValid() throws Exception {
        ProductRequestDto request = ProductRequestDto.builder()
                .sku("SKU-INT-1")
                .name("Keyboard")
                .description("Mechanical keyboard")
                .price(new BigDecimal("750000.00"))
                .category("Accessories")
                .build();

        mockMvc.perform(post("/api/catalog/products")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.status").value("OK"))
                .andExpect(jsonPath("$.message").value("Product created successfully"))
                .andExpect(jsonPath("$.data.sku").value("SKU-INT-1"))
                .andExpect(jsonPath("$.data.name").value("Keyboard"));
    }

    @Test
    void createProduct_Returns400_WhenValidationFails() throws Exception {
        ProductRequestDto request = ProductRequestDto.builder()
                .sku("")
                .name("")
                .price(new BigDecimal("-10.00"))
                .category("")
                .build();

        mockMvc.perform(post("/api/catalog/products")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.status").value("BAD_REQUEST"));
    }

    @Test
    void createProduct_Returns403_WhenRoleIsNotAdmin() throws Exception {
        ProductRequestDto request = ProductRequestDto.builder()
                .sku("SKU-INT-2")
                .name("Headphones")
                .price(new BigDecimal("300000.00"))
                .category("Audio")
                .build();

        mockMvc.perform(post("/api/catalog/products")
                        .header("X-User-Role", "ROLE_CASHIER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.status").value("FORBIDDEN"));
    }

    @Test
    void getProductById_Returns404_WhenNotFound() throws Exception {
        mockMvc.perform(get("/api/catalog/products/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.status").value("NOT_FOUND"));
    }
}
