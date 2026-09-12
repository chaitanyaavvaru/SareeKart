package com.sareekart.mapper;

import com.sareekart.dto.response.CategoryResponse;
import com.sareekart.entity.Category;
import com.sareekart.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryMapper {

    private final ProductRepository productRepository;

    public CategoryResponse toResponse(Category category) {
        if (category == null) return null;

        Long count = productRepository.countByCategoryId(category.getId());

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .parentName(category.getParent() != null ? category.getParent().getName() : null)
                .displayOrder(category.getDisplayOrder() != null ? category.getDisplayOrder() : 0)
                .active(category.getActive() != null ? category.getActive() : true)
                .productCount(count)
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }

    public CategoryResponse toResponseTree(Category category) {
        if (category == null) return null;

        CategoryResponse res = toResponse(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            List<CategoryResponse> children = category.getSubcategories().stream()
                    .filter(c -> Boolean.TRUE.equals(c.getActive()))
                    .map(this::toResponseTree)
                    .toList();
            res.setSubcategories(children);
        } else {
            res.setSubcategories(Collections.emptyList());
        }
        return res;
    }
}
