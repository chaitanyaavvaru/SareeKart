package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LtvTierSummary {
    private String tier;
    private String displayName;
    private Long customerCount;
    private BigDecimal totalRevenue;
    private BigDecimal totalSpend;
    private BigDecimal averageSpend;
    private BigDecimal averageLtv;
    private Double percentageOfCustomers;
    private Double percentageOfRevenue;

    public BigDecimal getTotalRevenue() {
        return totalRevenue != null ? totalRevenue : totalSpend;
    }

    public BigDecimal getTotalSpend() {
        return totalSpend != null ? totalSpend : totalRevenue;
    }

    public BigDecimal getAverageSpend() {
        return averageSpend != null ? averageSpend : averageLtv;
    }

    public BigDecimal getAverageLtv() {
        return averageLtv != null ? averageLtv : averageSpend;
    }
}
