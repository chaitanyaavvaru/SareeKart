package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuVelocityDto {
    private String sku;
    private String productName;
    private String name;
    private String category;
    private String warehouseCode;
    private Integer availableStock;
    private Integer unitsSold30d;
    private Long unitsSold;
    private Double dailyRunRate;
    private Double runRate;
    private Integer daysOfInventoryRemaining;
    private Double daysRemaining;
    private BigDecimal unitPrice;
    private String status;
}
