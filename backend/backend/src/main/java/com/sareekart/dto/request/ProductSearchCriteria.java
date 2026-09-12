package com.sareekart.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductSearchCriteria {

    /** Multi-token keyword search (matches name, description, category, fabric, occasion, color) */
    private String q;

    /** Category identifier: slug (e.g. 'silk-sarees'), numeric ID, or name */
    private String category;

    /** Fabric identifier: slug (e.g. 'tussar-silk'), numeric ID, or name */
    private String fabric;

    /** Occasion identifier: slug (e.g. 'wedding'), numeric ID, or name */
    private String occasion;

    /** Color identifier: slug (e.g. 'ruby-red'), numeric ID, or name */
    private String color;

    /** Canonical color family: Red, Pink, Blue, Green, Gold, etc. */
    private String colorFamily;

    /** Lower price bound */
    private BigDecimal minPrice;

    /** Upper price bound */
    private BigDecimal maxPrice;

    /** When true, only returns products with stockQuantity > 0 */
    private Boolean inStock;

    /** Sort column: 'createdAt', 'price', 'name' (default: 'createdAt') */
    @Builder.Default
    private String sortBy = "createdAt";

    /** Sort direction: 'asc', 'desc' (default: 'desc') */
    @Builder.Default
    private String sortDir = "desc";

    /** Zero-indexed page index (default: 0) */
    @Builder.Default
    private Integer page = 0;

    /** Page size (default: 12) */
    @Builder.Default
    private Integer size = 12;
}
