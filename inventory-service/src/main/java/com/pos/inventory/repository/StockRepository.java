package com.pos.inventory.repository;

import com.pos.inventory.entity.Stock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findBySkuAndBranchCode(String sku, String branchCode);

    Optional<Stock> findBySku(String sku);

    boolean existsBySkuAndBranchCode(String sku, String branchCode);

    List<Stock> findByBranchCode(String branchCode);
}
