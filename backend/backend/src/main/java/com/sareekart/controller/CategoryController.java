package com.sareekart.controller;

import com.sareekart.dto.request.CategoryRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.CategoryResponse;
import com.sareekart.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * Get categories. If ?tree=true, returns hierarchical root categories with subcategories.
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories(
            @RequestParam(value = "tree", required = false, defaultValue = "false") boolean tree,
            @RequestParam(value = "activeOnly", required = false, defaultValue = "true") Boolean activeOnly) {
        if (tree) {
            List<CategoryResponse> treeList = categoryService.getCategoryTree();
            return ResponseEntity.ok(ApiResponse.success(treeList));
        }
        List<CategoryResponse> categories = categoryService.getAllCategories(activeOnly);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Get category by ID.
     */
    @GetMapping("/categories/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success(category));
    }

    /**
     * Admin: Get all categories including inactive ones with hierarchy info.
     */
    @GetMapping("/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAdminCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories(false);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    /**
     * Create a new category (Admin/Manager/Owner).
     */
    @PostMapping("/admin/categories")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Category created successfully", category));
    }

    /**
     * Update a category (Admin/Manager/Owner).
     */
    @PutMapping("/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse category = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success("Category updated successfully", category));
    }

    /**
     * Toggle active/inactive status (Admin/Manager/Owner).
     */
    @PatchMapping("/admin/categories/{id}/toggle-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<CategoryResponse>> toggleCategoryStatus(@PathVariable Long id) {
        CategoryResponse category = categoryService.toggleCategoryStatus(id);
        return ResponseEntity.ok(ApiResponse.success("Category status updated", category));
    }

    /**
     * Safe delete a category (Admin/Manager/Owner).
     * Rejects if products > 0 or subcategories > 0.
     */
    @DeleteMapping("/admin/categories/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OWNER', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.success("Category deleted successfully", null));
    }
}
