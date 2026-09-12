package com.sareekart.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversionFunnelDto {
    private Long cartsCreated;
    private Long cartsWithItems;
    private Long checkoutInitiated;
    private Long ordersCompleted;
    private Double cartAbandonmentRate;
    private Double abandonmentRate;
    private Double conversionRate;
}
