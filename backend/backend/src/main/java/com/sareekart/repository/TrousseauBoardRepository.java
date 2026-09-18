package com.sareekart.repository;

import com.sareekart.entity.TrousseauBoard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrousseauBoardRepository extends JpaRepository<TrousseauBoard, Long> {

    List<TrousseauBoard> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<TrousseauBoard> findByShareToken(String shareToken);

    Optional<TrousseauBoard> findByIdAndUserId(Long id, Long userId);

    boolean existsByShareToken(String shareToken);
}
