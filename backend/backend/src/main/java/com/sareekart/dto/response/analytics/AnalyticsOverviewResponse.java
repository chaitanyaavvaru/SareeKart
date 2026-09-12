package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsOverviewResponse {

    // Structured period summaries
    private PeriodMetricsDto currentPeriod;
    private PeriodMetricsDto previousPeriod;

    // Current period direct metrics for backward compatibility & flat binding
    private BigDecimal grossSales;
    private BigDecimal netRevenue;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal aov;
    private Long completedOrders;
    private Long totalUnitsSold;

    // Prior period direct metrics
    private BigDecimal priorGrossSales;
    private BigDecimal priorNetRevenue;
    private BigDecimal priorTaxAmount;
    private BigDecimal priorShippingAmount;
    private BigDecimal priorAov;
    private Long priorCompletedOrders;

    // Period-over-period percentage changes: "grossSales" -> +18.4, "aov" -> -3.2
    private Map<String, Double> percentageChanges;

    // Context
    private String range;
    private Object startDate;
    private Object endDate;
    private Object priorStartDate;
    private Object priorEndDate;
}
