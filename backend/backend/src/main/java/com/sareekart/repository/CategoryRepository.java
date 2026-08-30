package com.sareekart.repository;

import com.sareekart.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Category> findByParentIsNullAndActiveTrueOrderBySortOrderAsc();

    List<Category> findByParentIdAndActiveTrueOrderBySortOrderAsc(Long parentId);

    @Query("SELECT c FROM Category c WHERE c.active = true AND (c.parent IS NULL OR c.parent.active = true) ORDER BY c.sortOrder")
    List<Category> findAllActiveHierarchy();

    Page<Category> findByActiveTrue(Pageable pageable);
}