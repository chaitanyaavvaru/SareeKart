package com.sareekart.service;

import com.sareekart.dto.request.ProductRequest;
import com.sareekart.dto.request.ProductSearchCriteria;
import com.sareekart.dto.response.PagedResponse;
import com.sareekart.dto.response.ProductResponse;
import java.math.BigDecimal;

public interface ProductService {

    PagedResponse<ProductResponse> getAllProducts(int page, int size, String sortBy, String sortDir);

    ProductResponse getProductById(Long id);

    ProductResponse createProduct(ProductRequest request);

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    PagedResponse<ProductResponse> searchProducts(String query, int page, int size, String sortBy, String sortDir);

    PagedResponse<ProductResponse> filterProducts(Long categoryId, BigDecimal minPrice, BigDecimal maxPrice, String fabric, int page, int size, String sortBy, String sortDir);

    PagedResponse<ProductResponse> searchAndFilterProducts(ProductSearchCriteria criteria);
}
