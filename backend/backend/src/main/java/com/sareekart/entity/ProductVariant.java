package com.sareekart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@Entity
@Table(name = "product_variants", uniqueConstraints = {
    @UniqueConstraint(name = "uk_product_variants_sku", columnNames = "sku")
})
public class ProductVariant extends BaseEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @ToString.Exclude
    private Product product;

    @Size(max = 20)
    @Column(name = "size", length = 20)
    private String size;

    @Size(max = 50)
    @Column(name = "color", length = 50)
    private String color;

    @Min(0)
    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity = 0;

    @DecimalMin(value = "0.0")
    @Column(name = "price_adjustment", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceAdjustment = BigDecimal.ZERO;

    @Size(max = 100)
    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public BigDecimal getEffectivePrice() {
        return product.getEffectivePrice().add(priceAdjustment);
    }

    public boolean isInStock() {
        return active && stockQuantity > 0;
    }
}