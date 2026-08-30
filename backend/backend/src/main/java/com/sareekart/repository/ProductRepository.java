package com.sareekart.repository;

import com.sareekart.entity.Product;
import com.sareekart.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySlug(String slug);

    Optional<Product> findBySku(String sku);

    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    Page<Product> findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable);

    List<Product> findByFabricAndActiveTrue(String fabric);

    Page<Product> findByFabricAndActiveTrue(String fabric, Pageable pageable);

    List<Product> findByOccasionAndActiveTrue(String occasion);

    Page<Product> findByOccasionAndActiveTrue(String occasion, Pageable pageable);

    List<Product> findByActiveTrueAndFeaturedTrue();

    Page<Product> findByActiveTrueAndFeaturedTrue(Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND (" +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.shortDescription) LIKE LOWER(CONCAT('%', :query, '%'))" +
           ") ORDER BY p.createdAt DESC")
    Page<Product> searchProducts(@Param("query") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true " +
           "AND (:categoryId IS NULL OR p.category.id = :categoryId) " +
           "AND (:fabric IS NULL OR p.fabric = :fabric) " +
           "AND (:occasion IS NULL OR p.occasion = :occasion) " +
           "AND (:minPrice IS NULL OR p.basePrice >= :minPrice) " +
           "AND (:maxPrice IS NULL OR p.basePrice <= :maxPrice) " +
           "ORDER BY p.createdAt DESC")
    Page<Product> filterProducts(
            @Param("categoryId") Long categoryId,
            @Param("fabric") String fabric,
            @Param("occasion") String occasion,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId AND p.id != :excludeId ORDER BY p.createdAt DESC")
    List<Product> findRelatedProducts(@Param("categoryId") Long categoryId, @Param("excludeId") Long excludeId, Pageable pageable);

    Page<Product> findByActiveTrueOrderByCreatedAtDesc(Pageable pageable);

    List<Product> findByActiveTrueOrderByBasePriceAsc();

    List<Product> findByActiveTrueOrderByBasePriceDesc();

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    long countByActiveTrue();

    /**
     * IDs of active products whose total variant stock is at or below the
     * given threshold (low-stock alert).
     */
    @Query("SELECT v.product.id FROM ProductVariant v " +
           "WHERE v.active = true AND v.product.active = true " +
           "GROUP BY v.product.id HAVING SUM(v.stockQuantity) <= :threshold")
    List<Long> findLowStockProductIds(@Param("threshold") int threshold);

    /** Best-selling products by quantity sold, excluding cancelled orders. */
    @Query("SELECT oi.product.id, oi.productName, SUM(oi.quantity), SUM(oi.totalPrice) " +
           "FROM OrderItem oi WHERE oi.order.status <> :cancelled " +
           "GROUP BY oi.product.id, oi.productName ORDER BY SUM(oi.quantity) DESC")
    List<Object[]> findTopSellingProducts(@Param("cancelled") OrderStatus cancelled, Pageable pageable);
}