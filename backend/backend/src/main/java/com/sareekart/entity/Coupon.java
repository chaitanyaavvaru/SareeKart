package com.sareekart.entity;

import com.sareekart.entity.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString(callSuper = true)
@Entity
@Table(name = "coupons", uniqueConstraints = {
    @UniqueConstraint(name = "uk_coupons_code", columnNames = "code")
})
public class Coupon extends BaseEntity {

    /** Uppercase A-Z/0-9/-/_ only; normalized on write. */
    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Code may contain only A-Z, 0-9, hyphen and underscore")
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    /** PERCENTAGE: 0–100 · FIXED_AMOUNT: rupees. */
    @NotNull
    @DecimalMin(value = "0.01")
    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @NotNull
    @DecimalMin(value = "0.00")
    @Column(name = "minimum_order_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal minimumOrderAmount = BigDecimal.ZERO;

    /** Optional cap; NULL = uncapped (PERCENTAGE only in practice). */
    @DecimalMin(value = "0.01")
    @Column(name = "maximum_discount_amount", precision = 10, scale = 2)
    private BigDecimal maximumDiscountAmount;

    @Column(name = "valid_from")
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    /** NULL = unlimited. */
    @Column(name = "total_usage_limit")
    private Integer totalUsageLimit;

    @Column(name = "per_user_usage_limit")
    private Integer perUserUsageLimit;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    public boolean isStarted(LocalDateTime now) {
        return validFrom == null || !now.isBefore(validFrom);
    }

    public boolean isExpired(LocalDateTime now) {
        return validUntil != null && now.isAfter(validUntil);
    }
}