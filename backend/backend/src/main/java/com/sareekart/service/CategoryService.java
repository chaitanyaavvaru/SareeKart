package com.sareekart.service;

import com.sareekart.dto.request.CategoryRequest;
import com.sareekart.dto.response.CategoryResponse;
import java.util.List;

public interface CategoryService {

    List<CategoryResponse> getAllCategories(Boolean activeOnly);

    List<CategoryResponse> getCategoryTree();

    CategoryResponse getCategoryById(Long id);

    CategoryResponse createCategory(CategoryRequest request);

    CategoryResponse updateCategory(Long id, CategoryRequest request);

    void deleteCategory(Long id);

    CategoryResponse toggleCategoryStatus(Long id);
}
