package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity managing distributed, concurrency-safe rate limiting state.
 *
 * <p>Keyed by a privacy-preserving identifier (SHA-256 hash) for each rate-limit dimension (IP or Account)
 * with a unique constraint. Database row-level locks (SELECT ... FOR UPDATE) serialize checks and updates
 * across multiple application instances sharing the database.
 */
@Entity
@Table(name = "rate_limit_coordinators", indexes = {
    @Index(name = "uk_rate_limit_coordinate_key", columnList = "coordinate_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RateLimitCoordinator {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "coordinate_key", nullable = false, unique = true, length = 191)
    private String coordinateKey;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "first_failure_at")
    private LocalDateTime firstFailureAt;

    @Column(name = "last_failure_at")
    private LocalDateTime lastFailureAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
