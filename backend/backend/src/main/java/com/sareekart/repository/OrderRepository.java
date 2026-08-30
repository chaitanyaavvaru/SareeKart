package com.sareekart.repository;

import com.sareekart.entity.Order;
import com.sareekart.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND o.status IN :statuses ORDER BY o.createdAt DESC")
    List<Order> findByUserIdAndStatusIn(Long userId, List<OrderStatus> statuses);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
    long countByStatus(OrderStatus status);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = :status AND o.createdAt >= :since")
    BigDecimal sumTotalAmountByStatusSince(@Param("status") OrderStatus status, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :since")
    long countOrdersSince(@Param("since") LocalDateTime since);

    Page<Order> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Order> findTop5ByOrderByCreatedAtDesc();

    /** Total revenue across all non-cancelled orders. */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status <> :cancelled")
    BigDecimal sumRevenueExcludingCancelled(@Param("cancelled") OrderStatus cancelled);

    /** Per-user order count and lifetime spend, excluding cancelled orders. */
    @Query("SELECT o.user.id, COUNT(o), COALESCE(SUM(o.totalAmount), 0) " +
           "FROM Order o WHERE o.status <> :cancelled GROUP BY o.user.id")
    List<Object[]> aggregateOrderStatsPerUser(@Param("cancelled") OrderStatus cancelled);

    /**
     * Sweeper candidates: unpaid ONLINE orders stuck in PENDING/PENDING past
     * the cutoff. COD orders are excluded by paymentMethod (they commit at
     * placement); PAID/PROCESSING+ are excluded by status/paymentStatus.
     */
    @Query("SELECT o FROM Order o WHERE o.paymentMethod = com.sareekart.entity.enums.PaymentMethod.RAZORPAY " +
           "AND o.status = com.sareekart.entity.enums.OrderStatus.PENDING " +
           "AND o.paymentStatus = com.sareekart.entity.enums.PaymentStatus.PENDING " +
           "AND o.createdAt < :cutoff ORDER BY o.createdAt ASC")
    List<Order> findStaleUnpaidOnlineOrders(@Param("cutoff") LocalDateTime cutoff,
                                            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT o FROM Order o WHERE (:status IS NULL OR o.status = :status) " +
           "AND (:userId IS NULL OR o.user.id = :userId) " +
           "AND (:startDate IS NULL OR o.createdAt >= :startDate) " +
           "AND (:endDate IS NULL OR o.createdAt <= :endDate) " +
           "ORDER BY o.createdAt DESC")
    Page<Order> findWithFilters(
            @Param("status") OrderStatus status,
            @Param("userId") Long userId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);
}