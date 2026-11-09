package com.pos.order.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemEventDto {
    private String sku;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
}
