package com.sareekart.repository;

import com.sareekart.entity.TrousseauVote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrousseauVoteRepository extends JpaRepository<TrousseauVote, Long> {

    List<TrousseauVote> findByItemIdOrderByCreatedAtDesc(Long itemId);

    long countByItemIdAndReaction(Long itemId, String reaction);

    long countByItemId(Long itemId);

    Optional<TrousseauVote> findByItemIdAndUserId(Long itemId, Long userId);

    Optional<TrousseauVote> findByItemIdAndVoterName(Long itemId, String voterName);
}
