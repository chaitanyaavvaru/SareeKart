package com.sareekart.dto.response.analytics;

import lombok.*;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryVelocityResponse {

    private Double daysOfInventoryRemaining;
    private Integer averageDaysRemaining;
    private Integer totalAvailableStock;
    private BigDecimal totalStockValuation;
    private BigDecimal totalValuation;
    private Integer totalSkus;
    private Integer lowStockCount;
    private Integer outOfStockCount;

    // Fast-moving & Slow-moving SKU rankings
    private List<SkuVelocityItem> fastMovingSkus;
    private List<SkuVelocityItem> slowMovingSkus;

    // Aging categories (<30d, 30-90d, >90d) - supports both List and aggregate container
    private Object agingCategories;

    // Stockout replenishment priority alerts
    private List<StockoutAlertItem> stockoutAlerts;

    public BigDecimal getTotalValuation() {
        return totalValuation != null ? totalValuation : totalStockValuation;
    }

    public BigDecimal getTotalStockValuation() {
        return totalStockValuation != null ? totalStockValuation : totalValuation;
    }

    public Double getDaysOfInventoryRemaining() {
        if (daysOfInventoryRemaining != null) return daysOfInventoryRemaining;
        return averageDaysRemaining != null ? averageDaysRemaining.doubleValue() : null;
    }

    public Integer getAverageDaysRemaining() {
        if (averageDaysRemaining != null) return averageDaysRemaining;
        return daysOfInventoryRemaining != null ? daysOfInventoryRemaining.intValue() : null;
    }
}
