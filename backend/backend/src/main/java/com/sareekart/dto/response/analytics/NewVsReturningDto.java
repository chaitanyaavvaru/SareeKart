package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewVsReturningDto {
    private Object newCustomers;
    private Object returningCustomers;
    private BigDecimal newRevenue;
    private BigDecimal returningRevenue;
    private Double repeatPurchaseRate;
}
