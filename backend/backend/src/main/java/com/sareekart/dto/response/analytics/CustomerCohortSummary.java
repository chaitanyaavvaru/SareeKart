package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerCohortSummary {
    private Long newCustomers;
    private Long returningCustomers;
    private Long totalOrderingCustomers;
    private BigDecimal newRevenue;
    private BigDecimal returningRevenue;
    private Double repeatPurchaseRate;
    private BigDecimal averageLtv;
}
