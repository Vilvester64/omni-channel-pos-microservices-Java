package com.pos.inventory.service;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockRequestDto;
import com.pos.inventory.dto.StockResponseDto;

import java.util.List;

public interface StockService {

    StockResponseDto createOrUpdateStock(StockRequestDto request);

    StockResponseDto getStockBySku(String sku, String branchCode);

    List<StockResponseDto> getAllStock(String branchCode);

    StockResponseDto addStock(StockAdjustmentDto adjustment);

    StockResponseDto deductStock(StockAdjustmentDto adjustment);
}
