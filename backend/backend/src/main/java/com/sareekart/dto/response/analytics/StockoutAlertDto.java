package com.sareekart.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockoutAlertDto {
    private String sku;
    private String productName;
    private String name;
    private String warehouseCode;
    private String warehouseName;
    private Integer availableStock;
    private Integer available;
    private Double dailyRunRate;
    private Double daysRemaining;
    private String status;
    private Integer priorityScore;
    private String priorityLevel;
    private Integer recommendedReorderQty;
}
