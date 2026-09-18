package com.sareekart.repository;

import com.sareekart.entity.TrousseauCeremony;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrousseauCeremonyRepository extends JpaRepository<TrousseauCeremony, Long> {

    List<TrousseauCeremony> findByBoardIdOrderByDisplayOrderAsc(Long boardId);

    Optional<TrousseauCeremony> findByIdAndBoardId(Long id, Long boardId);
}
