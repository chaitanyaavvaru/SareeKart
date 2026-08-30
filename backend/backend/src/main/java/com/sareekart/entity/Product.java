package com.sareekart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "products", uniqueConstraints = {
    @UniqueConstraint(name = "uk_products_slug", columnNames = "slug"),
    @UniqueConstraint(name = "uk_products_sku", columnNames = "sku")
})
public class Product extends BaseEntity {

    @NotBlank
    @Size(max = 255)
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @NotBlank
    @Size(max = 255)
    @Column(name = "slug", nullable = false, unique = true, length = 255)
    private String slug;

    @Size(max = 2000)
    @Column(name = "description", length = 2000)
    private String description;

    @Size(max = 500)
    @Column(name = "short_description", length = 500)
    private String shortDescription;

    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @DecimalMin(value = "0.0")
    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    @ToString.Exclude
    private Category category;

    @Size(max = 50)
    @Column(name = "fabric", length = 50)
    private String fabric;

    @Size(max = 50)
    @Column(name = "occasion", length = 50)
    private String occasion;

    @Size(max = 100)
    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "featured", nullable = false)
    private Boolean featured = false;

    @Size(max = 255)
    @Column(name = "meta_title", length = 255)
    private String metaTitle;

    @Size(max = 500)
    @Column(name = "meta_description", length = 500)
    private String metaDescription;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY, cascade = jakarta.persistence.CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    private List<ProductVariant> variants = new ArrayList<>();

    public BigDecimal getEffectivePrice() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0 ? salePrice : basePrice;
    }

    public boolean isOnSale() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0
                && salePrice.compareTo(basePrice) < 0;
    }

    public int getTotalStock() {
        return variants.stream()
            .filter(ProductVariant::getActive)
            .mapToInt(ProductVariant::getStockQuantity)
            .sum();
    }

    public ProductImage getPrimaryImage() {
        return images.stream()
            .filter(ProductImage::getIsPrimary)
            .findFirst()
            .orElse(images.isEmpty() ? null : images.get(0));
    }
}