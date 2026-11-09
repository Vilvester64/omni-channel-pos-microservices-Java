package com.pos.inventory.controller;

import com.pos.inventory.dto.ApiResponse;
import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockRequestDto;
import com.pos.inventory.dto.StockResponseDto;
import com.pos.inventory.service.StockService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/stocks")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<StockResponseDto>> createOrUpdateStock(
            @Valid @RequestBody StockRequestDto request,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        StockResponseDto response = stockService.createOrUpdateStock(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stock initialized/updated successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<StockResponseDto>>> getAllStock(
            @RequestParam(value = "branchCode", required = false) String branchCode) {
        List<StockResponseDto> stocks = stockService.getAllStock(branchCode);
        return ResponseEntity.ok(ApiResponse.success(stocks, "Stock list retrieved successfully"));
    }

    @GetMapping("/{sku}")
    public ResponseEntity<ApiResponse<StockResponseDto>> getStockBySku(
            @PathVariable String sku,
            @RequestParam(value = "branchCode", required = false) String branchCode) {
        StockResponseDto stock = stockService.getStockBySku(sku, branchCode);
        return ResponseEntity.ok(ApiResponse.success(stock, "Stock retrieved successfully"));
    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponse<StockResponseDto>> addStock(
            @Valid @RequestBody StockAdjustmentDto adjustment,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        StockResponseDto response = stockService.addStock(adjustment);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock added successfully"));
    }

    @PostMapping("/deduct")
    public ResponseEntity<ApiResponse<StockResponseDto>> deductStock(
            @Valid @RequestBody StockAdjustmentDto adjustment,
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        verifyAdminRoleIfPresent(role);
        StockResponseDto response = stockService.deductStock(adjustment);
        return ResponseEntity.ok(ApiResponse.success(response, "Stock deducted successfully"));
    }

    private void verifyAdminRoleIfPresent(String role) {
        if (role != null && !role.isBlank() && !role.contains("ROLE_ADMIN")) {
            throw new SecurityException("Access denied: ROLE_ADMIN required");
        }
    }
}
