package com.sareekart.dto.coupon;

import com.sareekart.entity.enums.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Customer-safe coupon preview. Contains NO internal ids, usage counters, or
 * admin fields — only what the checkout needs to display.
 *
 * subtotal/discount/finalAmount are always server-computed from the caller's
 * live cart; the client never supplies money values.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponPreviewResponse {
    private String code;
    private String description;
    private DiscountType discountType;
    /** Present when valid. */
    private BigDecimal discount;
    /** Server-computed cart subtotal the discount was applied against. */
    private BigDecimal subtotal;
    /** subtotal − discount (never negative; excludes tax/shipping). */
    private BigDecimal finalAmount;
    private boolean valid;
    private String message;

    public static CouponPreviewResponse invalid(String code, String message) {
        return CouponPreviewResponse.builder()
            .code(code)
            .valid(false)
            .message(message)
            .build();
    }
}