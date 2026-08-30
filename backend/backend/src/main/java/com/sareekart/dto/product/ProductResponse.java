package com.sareekart.dto.product;

import com.sareekart.entity.Product;
import com.sareekart.entity.ProductImage;
import com.sareekart.entity.ProductVariant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String shortDescription;
    /** Effective selling price (sale price when active, else base price). Primary display field. */
    private BigDecimal price;
    private BigDecimal basePrice;
    private BigDecimal salePrice;
    private boolean onSale;
    private Long categoryId;
    private String categoryName;
    private String fabric;
    private String occasion;
    private String sku;
    private Boolean active;
    private Boolean featured;
    private int totalStock;
    /**
     * Image URLs ordered primary-first. Exposed as plain strings to match the
     * storefront rendering pipeline (SafeImage, hover-swap, cart snapshots).
     */
    private List<String> images;
    private List<ProductVariantResponse> variants;

    public static ProductResponse from(Product product) {
        if (product == null) return null;

        List<String> imageUrls = product.getImages() == null ? List.of() : product.getImages().stream()
            .sorted(Comparator
                .comparing((ProductImage i) -> Boolean.TRUE.equals(i.getIsPrimary()) ? 0 : 1)
                .thenComparing(ProductImage::getSortOrder))
            .map(ProductImage::getUrl)
            .collect(Collectors.toList());

        List<ProductVariantResponse> variantResponses = product.getVariants() == null ? List.of()
            : product.getVariants().stream()
                .filter(v -> !Boolean.FALSE.equals(v.getActive()))
                .map(ProductVariantResponse::from)
                .collect(Collectors.toList());

        return ProductResponse.builder()
            .id(product.getId())
            .name(product.getName())
            .slug(product.getSlug())
            .description(product.getDescription())
            .shortDescription(product.getShortDescription())
            .price(product.getEffectivePrice())
            .basePrice(product.getBasePrice())
            .salePrice(product.getSalePrice())
            .onSale(product.isOnSale())
            .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
            .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
            .fabric(product.getFabric())
            .occasion(product.getOccasion())
            .sku(product.getSku())
            .active(product.getActive())
            .featured(product.getFeatured())
            .totalStock(product.getTotalStock())
            .images(imageUrls)
            .variants(variantResponses)
            .build();
    }

    public static List<ProductResponse> fromList(List<Product> products) {
        return products.stream()
            .map(ProductResponse::from)
            .collect(Collectors.toList());
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductVariantResponse {
        private Long id;
        private String size;
        private String color;
        private Integer stockQuantity;
        private BigDecimal priceAdjustment;
        private BigDecimal effectivePrice;
        private String sku;
        private boolean inStock;

        public static ProductVariantResponse from(ProductVariant variant) {
            if (variant == null) return null;
            return ProductVariantResponse.builder()
                .id(variant.getId())
                .size(variant.getSize())
                .color(variant.getColor())
                .stockQuantity(variant.getStockQuantity())
                .priceAdjustment(variant.getPriceAdjustment())
                .effectivePrice(variant.getEffectivePrice())
                .sku(variant.getSku())
                .inStock(variant.isInStock())
                .build();
        }
    }
}