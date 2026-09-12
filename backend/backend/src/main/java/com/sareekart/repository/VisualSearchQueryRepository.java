package com.sareekart.repository;

import com.sareekart.entity.VisualSearchQuery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VisualSearchQueryRepository extends JpaRepository<VisualSearchQuery, Long> {

    Page<VisualSearchQuery> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<VisualSearchQuery> findTop10ByOrderByCreatedAtDesc();

    long countByCreatedAtAfter(LocalDateTime since);

    @Query("SELECT AVG(v.confidenceScore) FROM VisualSearchQuery v")
    Double findAverageConfidenceScore();

    @Query("SELECT v.extractedPrimaryColor, COUNT(v) FROM VisualSearchQuery v GROUP BY v.extractedPrimaryColor ORDER BY COUNT(v) DESC")
    List<Object[]> findTopQueriedColors();
}
