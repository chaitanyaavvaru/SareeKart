package com.sareekart.repository;

import com.sareekart.entity.Payment;
import com.sareekart.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);

    Optional<Payment> findByProviderOrderId(String providerOrderId);

    Optional<Payment> findByProviderPaymentId(String providerPaymentId);

    Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<Payment> findByStatus(PaymentStatus status);

    /** Row-level lock so concurrent verify/webhook calls serialize on one payment. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.providerOrderId = :razorpayOrderId")
    Optional<Payment> findByProviderOrderIdForUpdate(@Param("razorpayOrderId") String razorpayOrderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdForUpdate(@Param("id") Long id);

    /**
     * Row lock shared with verify/webhook paths: expiring an order and
     * capturing its payment can never interleave.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId ORDER BY p.createdAt DESC")
    List<Payment> findByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.status = :status " +
           "ORDER BY p.createdAt DESC")
    List<Payment> findByOrderIdAndStatusForUpdate(@Param("orderId") Long orderId,
                                                  @Param("status") PaymentStatus status);
}