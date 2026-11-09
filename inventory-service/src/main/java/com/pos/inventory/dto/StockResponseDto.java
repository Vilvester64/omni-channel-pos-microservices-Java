package com.pos.inventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockResponseDto {
    private Long id;
    private String sku;
    private String branchCode;
    private Integer quantity;
    private Long version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
