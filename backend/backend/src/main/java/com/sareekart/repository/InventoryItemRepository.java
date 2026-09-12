package com.sareekart.repository;

import com.sareekart.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {
    Optional<InventoryItem> findBySku(String sku);
    Optional<InventoryItem> findByProductId(Long productId);
    List<InventoryItem> findByWarehouseCode(String warehouseCode);
    List<InventoryItem> findAllByOrderByUpdatedAtDesc();

    long countByStatus(String status);

    @Query("SELECT COALESCE(SUM(i.available), 0) FROM InventoryItem i")
    Integer sumTotalAvailableStock();

    @Query("SELECT COALESCE(SUM(i.available * i.unitPrice), 0) FROM InventoryItem i")
    BigDecimal sumTotalStockValuation();
}
