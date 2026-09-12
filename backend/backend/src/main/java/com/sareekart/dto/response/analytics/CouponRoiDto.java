package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponRoiDto {
    private String code;
    private Integer uses;
    private BigDecimal discountTotal;
    private BigDecimal revenueGenerated;
    private Double roi;
}
