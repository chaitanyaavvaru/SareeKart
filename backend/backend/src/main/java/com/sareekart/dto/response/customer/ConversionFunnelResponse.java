package com.sareekart.dto.response.customer;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionFunnelResponse {
    private String range;
    private String startDate;
    private String endDate;
    private long totalEvents;
    private long activeSessions;

    // Full 8-stage eCommerce conversion funnel
    private List<BehavioralFunnelStageDto> stages;

    // Core conversion & abandonment metrics
    private double overallConversionRate;      // Landing to Purchase (%)
    private double detailToCartRate;           // Product View to Add to Cart (%)
    private double cartToCheckoutRate;         // Add to Cart to Begin Checkout (%)
    private double checkoutToPaymentRate;      // Begin Checkout to Payment Attempt (%)
    private double paymentToOrderRate;         // Payment Attempt to Order Completed (%)
    private double cartAbandonmentRate;        // Abandonment between Cart and Purchase (%)
    private double checkoutAbandonmentRate;    // Abandonment between Checkout and Purchase (%)

    // Auxiliary Channel Attributions
    private Map<String, Long> channelEngagement;
}
