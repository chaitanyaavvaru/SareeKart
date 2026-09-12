package com.sareekart.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionFunnelSummary {
    private Long cartsCreated;
    private Long cartsWithItems;
    private Long checkoutInitiated;
    private Long ordersCompleted;
    private Double cartAbandonmentRate;
    private Double abandonmentRate;
    private Double checkoutAbandonmentRate;
    private Double overallConversionRate;
    private Double conversionRate;

    public Double getCartAbandonmentRate() {
        return cartAbandonmentRate != null ? cartAbandonmentRate : abandonmentRate;
    }

    public Double getAbandonmentRate() {
        return abandonmentRate != null ? abandonmentRate : cartAbandonmentRate;
    }

    public Double getOverallConversionRate() {
        return overallConversionRate != null ? overallConversionRate : conversionRate;
    }

    public Double getConversionRate() {
        return conversionRate != null ? conversionRate : overallConversionRate;
    }
}
