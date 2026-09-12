package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAgingCategory {
    private String category;
    private String displayName;
    private Long itemCount;
    private Long totalUnits;
    private BigDecimal valuation;
    private Double valuationPercentage;
}
