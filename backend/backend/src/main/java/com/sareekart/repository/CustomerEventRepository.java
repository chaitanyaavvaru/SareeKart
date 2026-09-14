package com.sareekart.repository;

import com.sareekart.entity.CustomerEvent;
import com.sareekart.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerEventRepository extends JpaRepository<CustomerEvent, Long> {

    boolean existsByClientEventId(String clientEventId);

    Optional<CustomerEvent> findByClientEventId(String clientEventId);

    List<CustomerEvent> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<CustomerEvent> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<CustomerEvent> findBySessionIdOrderByCreatedAtDesc(String sessionId, Pageable pageable);

    List<CustomerEvent> findBySessionId(String sessionId);

    List<CustomerEvent> findTop50ByOrderByCreatedAtDesc();

    List<CustomerEvent> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime start, LocalDateTime end);

    List<CustomerEvent> findByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    long countByEventTypeAndCreatedAtBetween(String eventType, LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM CustomerEvent e WHERE e.createdAt BETWEEN :start AND :end")
    long countDistinctSessionsBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT COUNT(DISTINCT e.sessionId) FROM CustomerEvent e WHERE e.eventType = :eventType AND e.createdAt BETWEEN :start AND :end")
    long countDistinctSessionsByEventTypeBetween(@Param("eventType") String eventType, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT DISTINCT e.user.id FROM CustomerEvent e WHERE e.sessionId = :sessionId AND e.user IS NOT NULL")
    List<Long> findDistinctUserIdsBySessionId(@Param("sessionId") String sessionId);

    @Modifying
    @Query("UPDATE CustomerEvent e SET e.user = :user WHERE e.sessionId = :sessionId AND e.user IS NULL")
    int linkSessionToUser(@Param("sessionId") String sessionId, @Param("user") User user);
}
