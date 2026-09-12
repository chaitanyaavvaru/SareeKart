package com.sareekart.repository;

import com.sareekart.entity.ApprovalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequest, Long> {
    List<ApprovalRequest> findByStatusOrderByCreatedAtDesc(String status);
    List<ApprovalRequest> findAllByOrderByCreatedAtDesc();
    Optional<ApprovalRequest> findByEntityTypeAndTargetEntityIdAndStatus(String entityType, String targetEntityId, String status);
}
