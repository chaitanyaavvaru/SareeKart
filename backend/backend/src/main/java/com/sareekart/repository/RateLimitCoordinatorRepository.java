package com.sareekart.repository;

import com.sareekart.entity.RateLimitCoordinator;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitCoordinatorRepository extends JpaRepository<RateLimitCoordinator, Long> {

    Optional<RateLimitCoordinator> findByCoordinateKey(String coordinateKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM RateLimitCoordinator r WHERE r.coordinateKey = :coordinateKey")
    Optional<RateLimitCoordinator> findByCoordinateKeyForUpdate(@Param("coordinateKey") String coordinateKey);
}
