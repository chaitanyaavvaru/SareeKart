package com.sareekart.controller;

import com.sareekart.dto.common.ApiResponse;
import com.sareekart.dto.common.PageResponse;
import com.sareekart.dto.product.ProductFilterRequest;
import com.sareekart.dto.product.ProductRequest;
import com.sareekart.dto.product.ProductResponse;
import com.sareekart.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product APIs")
public class ProductController {

    private final ProductService productService;
    private final org.springframework.beans.factory.ObjectProvider<com.sareekart.storage.FileStorageService> storageProvider;

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> getProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<ProductResponse> products = PageResponse.from(
            productService.getProducts(page, size, sortBy, sortDir));
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/search")
    @Operation(summary = "Search products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> searchProducts(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        PageResponse<ProductResponse> products = PageResponse.from(
            productService.searchProducts(q, page, size));
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/filter")
    @Operation(summary = "Filter products")
    public ResponseEntity<ApiResponse<PageResponse<ProductResponse>>> filterProducts(
            @ModelAttribute ProductFilterRequest filter) {
        PageResponse<ProductResponse> products = PageResponse.from(
            productService.filterProducts(filter));
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts(
            @RequestParam(defaultValue = "4") int limit) {
        List<ProductResponse> products = productService.getFeaturedProducts(limit);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/fabrics")
    @Operation(summary = "Get all available fabrics")
    public ResponseEntity<ApiResponse<List<String>>> getFabrics() {
        List<String> fabrics = productService.getAllFabrics();
        return ResponseEntity.ok(ApiResponse.success(fabrics));
    }

    @GetMapping("/occasions")
    @Operation(summary = "Get all available occasions")
    public ResponseEntity<ApiResponse<List<String>>> getOccasions() {
        List<String> occasions = productService.getAllOccasions();
        return ResponseEntity.ok(ApiResponse.success(occasions));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/slug/{slug}")
    @Operation(summary = "Get product by slug")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySlug(@PathVariable String slug) {
        ProductResponse product = productService.getProductBySlug(slug);
        return ResponseEntity.ok(ApiResponse.success(product));
    }

    @GetMapping("/{id}/related")
    @Operation(summary = "Get related products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getRelatedProducts(@PathVariable Long id) {
        ProductResponse product = productService.getProductById(id);
        List<ProductResponse> related = productService.getRelatedProducts(
            product.getCategoryId(), id);
        return ResponseEntity.ok(ApiResponse.success(related));
    }

    // ── Admin endpoints ──────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.ok(ApiResponse.success("Product created successfully", product));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update product (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse product = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Quick stock adjustment (Admin)")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer stock) {
        ProductResponse product = productService.updateStock(id, stock);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", product));
    }

    /**
     * Uploads images for a product. Server validates content type, size and
     * magic bytes; generates the object key internally. Returns publicly
     * accessible URLs for each stored file.
     */
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Upload product images (Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files) {

        var storage = storageProvider.getObject();

        List<String> urls = files.stream()
            .map(file -> storage.store(id, file))
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(
            Map.of("urls", urls, "productId", id)));
    }

    /**
     * Deletes a single product image by its URL. Idempotent — missing
     * objects do not error.
     */
    @DeleteMapping("/{id}/images")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete product image by URL (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable Long id,
            @RequestParam String url) {
        var storage = storageProvider.getObject();
        storage.delete(url);
        return ResponseEntity.ok(ApiResponse.success("Image deleted", null));
    }
}
