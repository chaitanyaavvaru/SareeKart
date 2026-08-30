package com.sareekart.repository;

import com.sareekart.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {
    List<ProductVariant> findByProductIdAndActiveTrue(Long productId);

    Optional<ProductVariant> findBySku(String sku);

    boolean existsBySku(String sku);

    void deleteByProductId(Long productId);

    List<ProductVariant> findByProductIdAndSizeAndColorAndActiveTrue(Long productId, String size, String color);

    /**
     * Atomic decrement guarded by available stock. Returns 0 when stock is
     * insufficient — the caller converts that into a domain exception.
     */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity - :qty " +
           "WHERE v.id = :id AND v.stockQuantity >= :qty")
    int decrementStock(@Param("id") Long id, @Param("qty") Integer qty);

    /** Restock on cancellation/refund paths. */
    @Modifying
    @Query("UPDATE ProductVariant v SET v.stockQuantity = v.stockQuantity + :qty WHERE v.id = :id")
    int incrementStock(@Param("id") Long id, @Param("qty") Integer qty);
}