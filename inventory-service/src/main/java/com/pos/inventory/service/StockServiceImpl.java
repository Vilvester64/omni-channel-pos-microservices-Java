package com.pos.inventory.service;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockRequestDto;
import com.pos.inventory.dto.StockResponseDto;
import com.pos.inventory.entity.Stock;
import com.pos.inventory.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class StockServiceImpl implements StockService {

    private final StockRepository stockRepository;

    public StockServiceImpl(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    private String resolveBranch(String branchCode) {
        return (branchCode != null && !branchCode.isBlank()) ? branchCode.trim() : "MAIN";
    }

    @Override
    public StockResponseDto createOrUpdateStock(StockRequestDto request) {
        String branch = resolveBranch(request.getBranchCode());
        Stock stock = stockRepository.findBySkuAndBranchCode(request.getSku(), branch)
                .orElse(Stock.builder()
                        .sku(request.getSku())
                        .branchCode(branch)
                        .quantity(0)
                        .build());

        stock.setQuantity(request.getQuantity());
        Stock saved = stockRepository.saveAndFlush(stock);
        return mapToDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public StockResponseDto getStockBySku(String sku, String branchCode) {
        String branch = resolveBranch(branchCode);
        Stock stock = stockRepository.findBySkuAndBranchCode(sku, branch)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for SKU: '" + sku + "' at branch: '" + branch + "'"));
        return mapToDto(stock);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockResponseDto> getAllStock(String branchCode) {
        List<Stock> stocks;
        if (branchCode != null && !branchCode.isBlank()) {
            stocks = stockRepository.findByBranchCode(branchCode.trim());
        } else {
            stocks = stockRepository.findAll();
        }
        return stocks.stream().map(this::mapToDto).toList();
    }

    @Override
    public StockResponseDto addStock(StockAdjustmentDto adjustment) {
        String branch = resolveBranch(adjustment.getBranchCode());
        Stock stock = stockRepository.findBySkuAndBranchCode(adjustment.getSku(), branch)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for SKU: '" + adjustment.getSku() + "' at branch: '" + branch + "'"));

        stock.setQuantity(stock.getQuantity() + adjustment.getQuantity());
        Stock saved = stockRepository.saveAndFlush(stock);
        return mapToDto(saved);
    }

    @Override
    public StockResponseDto deductStock(StockAdjustmentDto adjustment) {
        String branch = resolveBranch(adjustment.getBranchCode());
        Stock stock = stockRepository.findBySkuAndBranchCode(adjustment.getSku(), branch)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Stock not found for SKU: '" + adjustment.getSku() + "' at branch: '" + branch + "'"));

        if (stock.getQuantity() < adjustment.getQuantity()) {
            throw new IllegalArgumentException(
                    "Insufficient stock for SKU '" + adjustment.getSku() + "'. Available: "
                            + stock.getQuantity() + ", requested: " + adjustment.getQuantity());
        }

        stock.setQuantity(stock.getQuantity() - adjustment.getQuantity());
        Stock saved = stockRepository.saveAndFlush(stock);
        return mapToDto(saved);
    }

    private StockResponseDto mapToDto(Stock stock) {
        return StockResponseDto.builder()
                .id(stock.getId())
                .sku(stock.getSku())
                .branchCode(stock.getBranchCode())
                .quantity(stock.getQuantity())
                .version(stock.getVersion())
                .createdAt(stock.getCreatedAt())
                .updatedAt(stock.getUpdatedAt())
                .build();
    }
}
