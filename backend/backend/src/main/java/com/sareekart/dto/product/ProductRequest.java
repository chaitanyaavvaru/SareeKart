package com.sareekart.dto.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {
    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 255)
    private String slug;

    @Size(max = 2000)
    private String description;

    @Size(max = 500)
    private String shortDescription;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0")
    private BigDecimal salePrice;

    @NotNull
    private Long categoryId;

    @Size(max = 50)
    private String fabric;

    @Size(max = 50)
    private String occasion;

    @Size(max = 100)
    private String sku;

    private Boolean active = true;

    private Boolean featured = false;

    @Size(max = 255)
    private String metaTitle;

    @Size(max = 500)
    private String metaDescription;

    private List<ProductImageRequest> images;

    private List<ProductVariantRequest> variants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductImageRequest {
        private String url;
        private String altText;
        private Boolean isPrimary = false;
        private Integer sortOrder = 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantRequest {
        @Size(max = 20)
        private String size;

        @Size(max = 50)
        private String color;

        private Integer stockQuantity = 0;

        private BigDecimal priceAdjustment = BigDecimal.ZERO;

        @Size(max = 100)
        private String sku;

        private Boolean active = true;
    }
}