package com.sareekart.repository;

import com.sareekart.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    /**
     * Sums total units sold for a given product within a date range for non-cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.product.id = :productId " +
           "AND oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    Long sumUnitsSoldByProductIdInPeriod(
            @Param("productId") Long productId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sums total units sold within a date range for non-cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    Long sumTotalUnitsSoldBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sums gross sales (item price * quantity before order-level discounts) in date range.
     */
    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    BigDecimal sumGrossSalesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Checks if a user has purchased a specific product in any non-cancelled order.
     */
    @Query("SELECT COUNT(oi) > 0 FROM OrderItem oi " +
           "WHERE oi.order.user.id = :userId " +
           "AND oi.product.id = :productId " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    boolean hasUserPurchasedProduct(
            @Param("userId") Long userId,
            @Param("productId") Long productId);
}

