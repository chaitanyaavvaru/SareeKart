package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LtvTierDto {
    private String tier;
    private String displayName;
    private Integer customerCount;
    private BigDecimal totalSpend;
    private BigDecimal totalRevenue;
    private BigDecimal averageSpend;
    private Double customerPercentage;
    private Double revenuePercentage;
}
