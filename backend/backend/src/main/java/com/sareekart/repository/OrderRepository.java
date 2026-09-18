package com.sareekart.repository;

import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByTrackingNumber(String trackingNumber);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal sumTotalRevenue();

    List<Order> findTop5ByOrderByCreatedAtDesc();

    // ==================== Milestone M1 Analytics Telemetry Queries ====================

    /**
     * Net revenue sum (totalAmount) within [startDate, endDate] for non-cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED")
    BigDecimal sumNetRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Gross sales sum across all items of non-cancelled orders in date range.
     */
    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    BigDecimal sumGrossSalesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts completed / active non-cancelled transactions within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED")
    long countNonCancelledOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts completed orders specifically in CONFIRMED, SHIPPED, or DELIVERED status.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status IN (com.sareekart.entity.OrderStatus.CONFIRMED, " +
           "com.sareekart.entity.OrderStatus.SHIPPED, " +
           "com.sareekart.entity.OrderStatus.DELIVERED)")
    Long countCompletedOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts total orders placed in period regardless of status (for checkout initiation).
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countTotalOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Estimated shipping fees collected for orders below the ₹5,000 threshold.
     */
    @Query("SELECT COALESCE(COUNT(o) * 150.0, 0) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "AND o.totalAmount < 5000")
    BigDecimal sumShippingFeesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Fetches non-cancelled orders in date range for high-fidelity in-memory stream processing.
     */
    List<Order> findByCreatedAtBetweenAndStatusNot(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus status);

    List<Order> findByStatusNot(OrderStatus status);

    // PHASE 5: Idempotent order retrieval
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    // PHASE 13: Razorpay webhook & async payment order lookup
    Optional<Order> findByRazorpayOrderId(String razorpayOrderId);

    // PHASE 5: Sweep for abandoned PENDING orders
    List<Order> findByStatusAndPaymentStatusAndCreatedAtBefore(
            OrderStatus status,
            String paymentStatus,
            LocalDateTime cutoffTime);

    // PHASE 5: Atomic state transition for idempotent cancellation & stock restoration
    @org.springframework.data.jpa.repository.Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE Order o SET o.status = :newStatus, o.paymentStatus = :newPaymentStatus " +
           "WHERE o.id = :orderId AND o.status = :expectedStatus AND o.paymentStatus = :expectedPaymentStatus")
    int transitionOrderStatusAndPaymentStatus(
            @Param("orderId") Long orderId,
            @Param("expectedStatus") OrderStatus expectedStatus,
            @Param("expectedPaymentStatus") String expectedPaymentStatus,
            @Param("newStatus") OrderStatus newStatus,
            @Param("newPaymentStatus") String newPaymentStatus);
}
