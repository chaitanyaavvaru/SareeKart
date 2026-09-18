package com.sareekart.repository;

import com.sareekart.entity.TrousseauCollaborator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrousseauCollaboratorRepository extends JpaRepository<TrousseauCollaborator, Long> {

    List<TrousseauCollaborator> findByBoardIdOrderByCreatedAtAsc(Long boardId);

    Optional<TrousseauCollaborator> findByBoardIdAndPhone(Long boardId, String phone);

    Optional<TrousseauCollaborator> findByBoardIdAndEmail(Long boardId, String email);
}
