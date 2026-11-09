package com.pos.catalog.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private Long id;
    private String sku;
    private String name;
    private String description;
    private BigDecimal price;
    private String category;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
