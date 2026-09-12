package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesTelemetryResponse {

    // Summary totals
    private BigDecimal grossSales;
    private BigDecimal netRevenue;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal aov;
    private Long completedOrders;
    private Long totalOrders;
    private Long completedTransactions;
    private Long totalUnitsSold;

    // Visual timeline points for charts
    private List<SalesTimelinePoint> timeline;

    // Payment method distribution
    private List<PaymentDistributionItem> paymentDistribution;

    // Coupon campaign utilization & ROI
    private List<CouponUtilizationItem> couponUtilization;

    // Interval context
    private String range;
    private Object startDate;
    private Object endDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesTimelinePoint {
        private String date;         // YYYY-MM-DD
        private BigDecimal revenue;
        private Long orders;
        private Long units;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDistributionItem {
        private String method;       // UPI, Cards, NetBanking, COD
        private Long count;
        private BigDecimal amount;
        private Double percentage;   // e.g. 45.5
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CouponUtilizationItem {
        private String code;
        private Integer uses;
        private BigDecimal discountTotal;
        private BigDecimal revenueGenerated;
        private Double roi;          // revenueGenerated / discountTotal
    }
}
