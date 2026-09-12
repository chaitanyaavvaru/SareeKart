package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkuVelocityItem {
    private String sku;
    private String name;
    private String productName;
    private String category;
    private String warehouseCode;
    private String warehouseName;
    private Long unitsSold;
    private Integer unitsSold30d;
    private Double runRate;
    private Double dailyRunRate;
    private Integer availableStock;
    private Double daysRemaining;
    private Integer daysOfInventoryRemaining;
    private BigDecimal unitPrice;
    private String status;

    public String getName() {
        return name != null ? name : productName;
    }

    public String getProductName() {
        return productName != null ? productName : name;
    }

    public Long getUnitsSold() {
        if (unitsSold != null) return unitsSold;
        return unitsSold30d != null ? unitsSold30d.longValue() : 0L;
    }

    public Integer getUnitsSold30d() {
        if (unitsSold30d != null) return unitsSold30d;
        return unitsSold != null ? unitsSold.intValue() : 0;
    }

    public Double getRunRate() {
        return runRate != null ? runRate : dailyRunRate;
    }

    public Double getDailyRunRate() {
        return dailyRunRate != null ? dailyRunRate : runRate;
    }

    public Double getDaysRemaining() {
        if (daysRemaining != null) return daysRemaining;
        return daysOfInventoryRemaining != null ? daysOfInventoryRemaining.doubleValue() : null;
    }

    public Integer getDaysOfInventoryRemaining() {
        if (daysOfInventoryRemaining != null) return daysOfInventoryRemaining;
        return daysRemaining != null ? daysRemaining.intValue() : null;
    }
}
