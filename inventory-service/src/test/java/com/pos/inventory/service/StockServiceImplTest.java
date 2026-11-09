package com.pos.inventory.service;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.dto.StockRequestDto;
import com.pos.inventory.dto.StockResponseDto;
import com.pos.inventory.entity.Stock;
import com.pos.inventory.repository.StockRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockServiceImplTest {

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private StockServiceImpl stockService;

    private Stock testStock;

    @BeforeEach
    void setUp() {
        testStock = Stock.builder()
                .id(1L)
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(50)
                .version(1L)
                .build();
    }

    @Test
    void deductStock_DeductsQuantitySuccessfully_WhenSufficientStockAvailable() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(20)
                .reason("Order checkout")
                .build();

        StockResponseDto response = stockService.deductStock(adjustment);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(30);
        verify(stockRepository, times(1)).saveAndFlush(testStock);
    }

    @Test
    void deductStock_ThrowsIllegalArgumentException_WhenStockIsInsufficient() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(60)
                .reason("Large order")
                .build();

        assertThatThrownBy(() -> stockService.deductStock(adjustment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Insufficient stock for SKU 'SKU-100'");

        verify(stockRepository, never()).saveAndFlush(any(Stock.class));
    }

    @Test
    void deductStock_ThrowsEntityNotFoundException_WhenStockNotFound() {
        when(stockRepository.findBySkuAndBranchCode("UNKNOWN", "MAIN")).thenReturn(Optional.empty());

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("UNKNOWN")
                .branchCode("MAIN")
                .quantity(10)
                .build();

        assertThatThrownBy(() -> stockService.deductStock(adjustment))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Stock not found for SKU: 'UNKNOWN'");

        verify(stockRepository, never()).saveAndFlush(any(Stock.class));
    }

    @Test
    void deductStock_ThrowsObjectOptimisticLockingFailureException_WhenConcurrentModificationOccurs() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenThrow(
                new ObjectOptimisticLockingFailureException(Stock.class, 1L));

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(5)
                .build();

        assertThatThrownBy(() -> stockService.deductStock(adjustment))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void addStock_ThrowsObjectOptimisticLockingFailureException_WhenConcurrentModificationOccurs() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenThrow(
                new ObjectOptimisticLockingFailureException(Stock.class, 1L));

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(10)
                .build();

        assertThatThrownBy(() -> stockService.addStock(adjustment))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    void addStock_IncreasesQuantitySuccessfully_WhenStockExists() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenAnswer(invocation -> invocation.getArgument(0));

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("SKU-100")
                .branchCode("MAIN")
                .quantity(30)
                .reason("Restocking")
                .build();

        StockResponseDto response = stockService.addStock(adjustment);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(80);
        verify(stockRepository, times(1)).saveAndFlush(testStock);
    }

    @Test
    void addStock_ThrowsEntityNotFoundException_WhenStockNotFound() {
        when(stockRepository.findBySkuAndBranchCode("UNKNOWN", "MAIN")).thenReturn(Optional.empty());

        StockAdjustmentDto adjustment = StockAdjustmentDto.builder()
                .sku("UNKNOWN")
                .branchCode("MAIN")
                .quantity(10)
                .build();

        assertThatThrownBy(() -> stockService.addStock(adjustment))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createOrUpdateStock_CreatesNewStock_WhenStockDoesNotExist() {
        when(stockRepository.findBySkuAndBranchCode("SKU-200", "MAIN")).thenReturn(Optional.empty());
        when(stockRepository.saveAndFlush(any(Stock.class))).thenAnswer(invocation -> {
            Stock s = invocation.getArgument(0);
            s.setId(2L);
            return s;
        });

        StockRequestDto request = StockRequestDto.builder()
                .sku("SKU-200")
                .branchCode("MAIN")
                .quantity(100)
                .build();

        StockResponseDto response = stockService.createOrUpdateStock(request);

        assertThat(response).isNotNull();
        assertThat(response.getQuantity()).isEqualTo(100);
        assertThat(response.getSku()).isEqualTo("SKU-200");
    }

    @Test
    void getStockBySku_ReturnsStock_WhenStockExists() {
        when(stockRepository.findBySkuAndBranchCode("SKU-100", "MAIN")).thenReturn(Optional.of(testStock));

        StockResponseDto response = stockService.getStockBySku("SKU-100", "MAIN");

        assertThat(response).isNotNull();
        assertThat(response.getSku()).isEqualTo("SKU-100");
        assertThat(response.getQuantity()).isEqualTo(50);
    }

    @Test
    void getStockBySku_ThrowsEntityNotFoundException_WhenStockNotFound() {
        when(stockRepository.findBySkuAndBranchCode("UNKNOWN", "MAIN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> stockService.getStockBySku("UNKNOWN", "MAIN"))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void getAllStock_ReturnsAllStock_WhenNoBranchProvided() {
        when(stockRepository.findAll()).thenReturn(List.of(testStock));

        List<StockResponseDto> result = stockService.getAllStock(null);

        assertThat(result).hasSize(1);
    }

    @Test
    void getAllStock_ReturnsFilteredStock_WhenBranchProvided() {
        when(stockRepository.findByBranchCode("MAIN")).thenReturn(List.of(testStock));

        List<StockResponseDto> result = stockService.getAllStock("MAIN");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBranchCode()).isEqualTo("MAIN");
    }
}

