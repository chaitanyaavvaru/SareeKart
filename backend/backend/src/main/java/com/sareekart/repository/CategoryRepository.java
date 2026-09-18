package com.sareekart.repository;

import com.sareekart.dto.projection.CategorySitemapProjection;
import com.sareekart.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    @Query("SELECT c.slug AS slug, c.name AS name, c.updatedAt AS updatedAt FROM Category c WHERE c.active = true ORDER BY c.displayOrder ASC, c.name ASC")
    List<CategorySitemapProjection> findSitemapProjectionsByActiveTrue();

    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);

    List<Category> findByActiveTrueOrderByDisplayOrderAsc();
    List<Category> findAllByOrderByDisplayOrderAsc();

    List<Category> findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc();
    List<Category> findByParentIsNullOrderByDisplayOrderAsc();

    List<Category> findByParentIdAndActiveTrueOrderByDisplayOrderAsc(Long parentId);
    List<Category> findByParentIdOrderByDisplayOrderAsc(Long parentId);

    long countByParentId(Long parentId);
}
