package com.sareekart.dto.coupon;

import com.sareekart.entity.Coupon;
import com.sareekart.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/** Admin-facing coupon representation (includes usage counters). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponAdminResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minimumOrderAmount;
    private BigDecimal maximumDiscountAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Integer totalUsageLimit;
    private Integer perUserUsageLimit;
    private Boolean active;
    private long totalUsedCount;
    private LocalDateTime createdAt;

    public static CouponAdminResponse from(Coupon c, long usedCount) {
        return CouponAdminResponse.builder()
            .id(c.getId())
            .code(c.getCode())
            .description(c.getDescription())
            .discountType(c.getDiscountType())
            .discountValue(c.getDiscountValue())
            .minimumOrderAmount(c.getMinimumOrderAmount())
            .maximumDiscountAmount(c.getMaximumDiscountAmount())
            .validFrom(c.getValidFrom())
            .validUntil(c.getValidUntil())
            .totalUsageLimit(c.getTotalUsageLimit())
            .perUserUsageLimit(c.getPerUserUsageLimit())
            .active(c.getActive())
            .totalUsedCount(usedCount)
            .createdAt(c.getCreatedAt())
            .build();
    }

    public static List<CouponAdminResponse> fromList(List<Coupon> coupons, java.util.Map<Long, Long> usage) {
        return coupons.stream()
            .map(c -> from(c, usage.getOrDefault(c.getId(), 0L)))
            .collect(Collectors.toList());
    }
}