package com.sareekart.repository;

import com.sareekart.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM Notification n WHERE (n.userId = :userId) OR (n.userId IS NULL AND (n.targetRole IS NULL OR n.targetRole = :role)) ORDER BY n.createdAt DESC")
    List<Notification> findForUserOrRole(@Param("userId") Long userId, @Param("role") String role);

    @Query("SELECT COUNT(n) FROM Notification n WHERE ((n.userId = :userId) OR (n.userId IS NULL AND (n.targetRole IS NULL OR n.targetRole = :role))) AND n.isRead = false")
    long countUnreadForUserOrRole(@Param("userId") Long userId, @Param("role") String role);

    List<Notification> findAllByOrderByCreatedAtDesc();
}
