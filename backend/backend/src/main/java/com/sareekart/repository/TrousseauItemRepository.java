package com.sareekart.repository;

import com.sareekart.entity.TrousseauItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrousseauItemRepository extends JpaRepository<TrousseauItem, Long> {

    List<TrousseauItem> findByCeremonyIdOrderByCreatedAtAsc(Long ceremonyId);

    List<TrousseauItem> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    long countByCeremonyId(Long ceremonyId);

    boolean existsByCeremonyIdAndProductId(Long ceremonyId, Long productId);

    Optional<TrousseauItem> findByIdAndBoardId(Long id, Long boardId);

    Optional<TrousseauItem> findByIdAndCeremonyId(Long id, Long ceremonyId);
}
