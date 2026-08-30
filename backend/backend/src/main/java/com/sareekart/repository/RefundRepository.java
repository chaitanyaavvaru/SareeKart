package com.sareekart.repository;

import com.sareekart.entity.Payment;
import com.sareekart.entity.Refund;
import com.sareekart.entity.enums.RefundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Refund> findByStatus(RefundStatus status);

    Optional<Refund> findByProviderRefundId(String providerRefundId);

    /** Sum of SUCCESSFUL refunds for a payment — the refundable remainder basis. */
    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM Refund r " +
           "WHERE r.payment.id = :paymentId AND r.status = :status")
    BigDecimal sumAmountByPaymentIdAndStatus(@Param("paymentId") Long paymentId,
                                             @Param("status") RefundStatus status);

    boolean existsByPaymentIdAndStatus(Long paymentId, RefundStatus status);

    /**
     * Reconciliation candidates: PENDING past the staleness cutoff whose
     * retry backoff has elapsed (NULL next_retry_at = due now).
     */
    @Query("SELECT r FROM Refund r WHERE r.status = 'PENDING' " +
           "AND r.createdAt < :cutoff " +
           "AND (r.nextRetryAt IS NULL OR r.nextRetryAt <= :now) " +
           "ORDER BY r.createdAt ASC")
    List<Refund> findStalePendingForReconciliation(@Param("cutoff") LocalDateTime cutoff,
                                                   @Param("now") LocalDateTime now,
                                                   Pageable pageable);


    /**
     * Serializes refund initiation / webhook confirmation / aggregate
     * recompute against the same payment row used by verify & sweeper.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<com.sareekart.entity.Payment> lockPayment(@Param("id") Long paymentId);
}