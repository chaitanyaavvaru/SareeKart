package com.sareekart.repository;

import com.sareekart.entity.ReturnRequest;
import com.sareekart.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    /**
     * Finds all return requests submitted by a specific user, newest first.
     */
    @Query("SELECT r FROM ReturnRequest r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Finds the return request associated with a specific order.
     */
    @Query("SELECT r FROM ReturnRequest r WHERE r.order.id = :orderId")
    Optional<ReturnRequest> findByOrderId(@Param("orderId") Long orderId);

    /**
     * Finds all return requests matching a specific status enum, newest first.
     */
    @Query("SELECT r FROM ReturnRequest r WHERE r.status = :status ORDER BY r.createdAt DESC")
    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(@Param("status") ReturnStatus status);

    /**
     * Finds all return requests across all statuses, newest first.
     */
    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    /**
     * Checks whether a return request already exists for an order.
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN TRUE ELSE FALSE END FROM ReturnRequest r WHERE r.order.id = :orderId")
    boolean existsByOrderId(@Param("orderId") Long orderId);

    /**
     * Counts returns by status for Admin KPI cards.
     */
    @Query("SELECT COUNT(r) FROM ReturnRequest r WHERE r.status = :status")
    long countByStatus(@Param("status") ReturnStatus status);
}
