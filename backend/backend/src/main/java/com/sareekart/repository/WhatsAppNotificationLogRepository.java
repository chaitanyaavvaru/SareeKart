package com.sareekart.repository;

import com.sareekart.entity.WhatsAppNotificationLog;
import com.sareekart.enums.WhatsAppEventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WhatsAppNotificationLogRepository extends JpaRepository<WhatsAppNotificationLog, Long> {

    List<WhatsAppNotificationLog> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    List<WhatsAppNotificationLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<WhatsAppNotificationLog> findByEventTypeOrderByCreatedAtDesc(WhatsAppEventType eventType, Pageable pageable);

    Page<WhatsAppNotificationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByEventType(WhatsAppEventType eventType);

    long countBySimulated(Boolean simulated);

    @Query("SELECT COUNT(w) FROM WhatsAppNotificationLog w WHERE w.deliveryStatus = 'DELIVERED'")
    long countDelivered();

    @Query("SELECT COUNT(w) FROM WhatsAppNotificationLog w WHERE w.deliveryStatus = 'FAILED'")
    long countFailed();
}
