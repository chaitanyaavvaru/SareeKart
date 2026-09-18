package com.sareekart.repository;

import com.sareekart.dto.projection.ProductSitemapProjection;
import com.sareekart.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("SELECT p.id AS id, p.updatedAt AS updatedAt, p.createdAt AS createdAt FROM Product p WHERE p.active = true ORDER BY p.id ASC")
    List<ProductSitemapProjection> findSitemapProjectionsByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    List<Product> findByActiveTrue();

    Optional<Product> findByIdAndActiveTrue(Long id);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.fabric) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:minPrice IS NULL OR p.price >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.price <= :maxPrice) " +
           "AND (:fabric IS NULL OR LOWER(p.fabric) = LOWER(:fabric))")
    Page<Product> findByFilters(
            @Param("categoryId") Long categoryId,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("fabric") String fabric,
            Pageable pageable);

    List<Product> findByStockQuantityLessThan(Integer threshold);

    long countByActiveTrue();

    // SAFEGUARD 4: Safe Category Deletion guard
    long countByCategoryId(Long categoryId);

    // PHASE 5: Atomic Conditional Stock Decrement (InnoDB exclusive row lock, prevents overselling)
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity - :quantity " +
           "WHERE p.id = :productId AND p.stockQuantity >= :quantity AND p.active = true")
    int decrementStockIfAvailable(@Param("productId") Long productId, @Param("quantity") Integer quantity);

    // PHASE 5: Atomic Stock Restoration for Cancellations
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Product p SET p.stockQuantity = p.stockQuantity + :quantity " +
           "WHERE p.id = :productId")
    int incrementStock(@Param("productId") Long productId, @Param("quantity") Integer quantity);
}
