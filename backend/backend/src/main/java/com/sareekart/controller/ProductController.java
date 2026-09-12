package com.sareekart.controller;

import com.sareekart.dto.request.ProductRequest;
import com.sareekart.dto.request.ProductSearchCriteria;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.PagedResponse;
import com.sareekart.dto.response.ProductResponse;
import com.sareekart.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * Unified search & multi-faceted filtering endpoint with pagination and sorting.
     */
    @GetMapping("/products")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> getProducts(
            @ModelAttribute ProductSearchCriteria criteria) {
        PagedResponse<ProductResponse> products = productService.searchAndFilterProducts(criteria);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Get a single product by ID.
     */
    @GetMapping("/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    /**
     * Backward-compatible search alias delegating to unified query engine.
     */
    @GetMapping("/products/search")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> searchProducts(
            @ModelAttribute ProductSearchCriteria criteria) {
        PagedResponse<ProductResponse> products = productService.searchAndFilterProducts(criteria);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Backward-compatible filter alias delegating to unified query engine.
     */
    @GetMapping("/products/filter")
    public ResponseEntity<ApiResponse<PagedResponse<ProductResponse>>> filterProducts(
            @RequestParam(required = false) Long categoryId,
            @ModelAttribute ProductSearchCriteria criteria) {
        if (criteria.getCategory() == null && categoryId != null) {
            criteria.setCategory(String.valueOf(categoryId));
        }
        PagedResponse<ProductResponse> products = productService.searchAndFilterProducts(criteria);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    /**
     * Create a new product (Admin only - will be secured in Phase 3).
     */
    @PostMapping("/admin/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", product));
    }

    /**
     * Update an existing product (Admin only).
     */
    @PutMapping("/admin/products/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    /**
     * Soft-delete a product (Admin only).
     */
    @DeleteMapping("/admin/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }
}
