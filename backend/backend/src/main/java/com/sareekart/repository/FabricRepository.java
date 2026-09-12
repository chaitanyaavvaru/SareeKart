package com.sareekart.repository;

import com.sareekart.entity.Fabric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FabricRepository extends JpaRepository<Fabric, Long> {
    List<Fabric> findByActiveTrueOrderByDisplayOrderAsc();
    List<Fabric> findAllByOrderByDisplayOrderAsc();
    Optional<Fabric> findByNameIgnoreCase(String name);
    Optional<Fabric> findBySlug(String slug);
    boolean existsByNameIgnoreCase(String name);
}
