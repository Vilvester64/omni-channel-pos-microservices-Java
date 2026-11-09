package com.pos.inventory.service;

import com.pos.inventory.dto.StockAdjustmentDto;
import com.pos.inventory.entity.Stock;
import com.pos.inventory.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class StockOptimisticLockingTest {

    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        stockRepository.deleteAll();
    }

    @Test
    void optimisticLocking_ThrowsException_WhenConcurrentUpdateHasStaleVersion() {
        Stock initialStock = stockRepository.saveAndFlush(Stock.builder()
                .sku("SKU-CONCURRENT")
                .branchCode("MAIN")
                .quantity(100)
                .build());

        Long stockId = initialStock.getId();

        Stock client1Stock = stockRepository.findById(stockId).orElseThrow();
        Stock client2Stock = stockRepository.findById(stockId).orElseThrow();

        assertThat(client1Stock.getVersion()).isEqualTo(client2Stock.getVersion());

        client1Stock.setQuantity(client1Stock.getQuantity() - 10);
        stockRepository.saveAndFlush(client1Stock);

        client2Stock.setQuantity(client2Stock.getQuantity() - 20);

        assertThatThrownBy(() -> stockRepository.saveAndFlush(client2Stock))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }
}
