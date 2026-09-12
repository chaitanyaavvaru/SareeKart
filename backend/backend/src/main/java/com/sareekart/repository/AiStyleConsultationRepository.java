package com.sareekart.repository;

import com.sareekart.entity.AiStyleConsultation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiStyleConsultationRepository extends JpaRepository<AiStyleConsultation, Long> {

    long countByConvertedToTailoringTrue();

    List<AiStyleConsultation> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT c.occasion, COUNT(c) FROM AiStyleConsultation c WHERE c.occasion IS NOT NULL GROUP BY c.occasion ORDER BY COUNT(c) DESC")
    List<Object[]> findOccasionFrequencies();

    @Query("SELECT c.sareeName, COUNT(c) FROM AiStyleConsultation c GROUP BY c.sareeName ORDER BY COUNT(c) DESC")
    List<Object[]> findTopStyledSarees();
}
