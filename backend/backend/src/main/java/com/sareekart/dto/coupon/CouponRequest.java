package com.sareekart.dto.coupon;

import com.sareekart.entity.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {

    @NotBlank
    @Size(max = 50)
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Code may contain only letters, digits, hyphen and underscore")
    private String code;

    @Size(max = 255)
    private String description;

    @NotNull
    private DiscountType discountType;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal discountValue;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal minimumOrderAmount;

    @DecimalMin(value = "0.01")
    private BigDecimal maximumDiscountAmount; // optional cap

    private LocalDateTime validFrom;

    private LocalDateTime validUntil;

    private Integer totalUsageLimit;      // null = unlimited

    private Integer perUserUsageLimit;    // null = unlimited

    private Boolean active = true;
}