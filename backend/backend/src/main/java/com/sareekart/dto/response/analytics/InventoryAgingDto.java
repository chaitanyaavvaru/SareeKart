package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAgingDto {
    private String category;
    private String displayName;
    private Integer itemCount;
    private Integer totalUnits;
    private BigDecimal valuation;
    private Double valuationPercentage;
}
