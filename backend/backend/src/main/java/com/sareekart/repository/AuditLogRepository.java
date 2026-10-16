package com.sareekart.repository;

import com.sareekart.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findAllByOrderByCreatedAtDesc();
    List<AuditLog> findByEntityTypeOrderByCreatedAtDesc(String entityType);

    long countByActionAndIpAddressAndCreatedAtAfter(String action, String ipAddress, LocalDateTime cutoff);
    long countByActionAndEntityIdAndCreatedAtAfter(String action, String entityId, LocalDateTime cutoff);
}
