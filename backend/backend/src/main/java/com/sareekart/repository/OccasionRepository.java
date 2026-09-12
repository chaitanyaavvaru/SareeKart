package com.sareekart.repository;

import com.sareekart.entity.Occasion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OccasionRepository extends JpaRepository<Occasion, Long> {
    List<Occasion> findByActiveTrueOrderByDisplayOrderAsc();
    List<Occasion> findAllByOrderByDisplayOrderAsc();
    Optional<Occasion> findByNameIgnoreCase(String name);
    Optional<Occasion> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
}
