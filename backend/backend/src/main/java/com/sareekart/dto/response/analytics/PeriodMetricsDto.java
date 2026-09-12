package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PeriodMetricsDto {
    private BigDecimal grossSales;
    private BigDecimal netRevenue;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal aov;
    private Long completedOrders;
    private Long totalUnitsSold;
}
