package com.sareekart.dto.category;

import com.sareekart.entity.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String imageUrl;
    private Long parentId;
    private Integer sortOrder;
    private Boolean active;
    private List<CategoryResponse> children;

    public static CategoryResponse from(Category category) {
        return from(category, true);
    }

    public static CategoryResponse from(Category category, boolean includeChildren) {
        if (category == null) return null;

        CategoryResponse response = CategoryResponse.builder()
            .id(category.getId())
            .name(category.getName())
            .slug(category.getSlug())
            .description(category.getDescription())
            .imageUrl(category.getImageUrl())
            .parentId(category.getParent() != null ? category.getParent().getId() : null)
            .sortOrder(category.getSortOrder())
            .active(category.getActive())
            .build();

        if (includeChildren && category.getChildren() != null && !category.getChildren().isEmpty()) {
            response.setChildren(category.getChildren().stream()
                .filter(c -> c.getActive())
                .map(c -> from(c, true))
                .collect(Collectors.toList()));
        }

        return response;
    }

    public static List<CategoryResponse> fromList(List<Category> categories) {
        return categories.stream()
            .map(CategoryResponse::from)
            .collect(Collectors.toList());
    }
}