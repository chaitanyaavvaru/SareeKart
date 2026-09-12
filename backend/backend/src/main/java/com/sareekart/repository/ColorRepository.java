package com.sareekart.repository;

import com.sareekart.entity.Color;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColorRepository extends JpaRepository<Color, Long> {
    List<Color> findByActiveTrueOrderByDisplayOrderAsc();
    List<Color> findAllByOrderByDisplayOrderAsc();
    Optional<Color> findByNameIgnoreCase(String name);
    Optional<Color> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
}
