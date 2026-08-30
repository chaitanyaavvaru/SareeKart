package com.sareekart.dto.product;

import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductFilterRequest {
    private Long categoryId;
    private String fabric;
    private String occasion;
    
    @DecimalMin(value = "0.0")
    private BigDecimal minPrice;
    
    @DecimalMin(value = "0.0")
    private BigDecimal maxPrice;

    private String sortBy = "createdAt";
    private String sortDir = "desc";
    private int page = 0;
    private int size = 12;
}