package com.sareekart.dto.response.analytics;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockoutAlertItem {
    private String sku;
    private String name;
    private String productName;
    private String warehouse;
    private String warehouseCode;
    private String warehouseName;
    private String binLocation;
    private Integer available;
    private Integer availableStock;
    private Integer onHand;
    private Integer reserved;
    private Double dailyRunRate;
    private Double daysRemaining;
    private String status;
    private Integer priorityScore;
    private String priorityLevel;
    private Integer recommendedReorderQty;

    public String getName() {
        return name != null ? name : productName;
    }

    public String getProductName() {
        return productName != null ? productName : name;
    }

    public String getWarehouse() {
        return warehouse != null ? warehouse : warehouseCode;
    }

    public String getWarehouseCode() {
        return warehouseCode != null ? warehouseCode : warehouse;
    }

    public Integer getAvailable() {
        return available != null ? available : availableStock;
    }

    public Integer getAvailableStock() {
        return availableStock != null ? availableStock : available;
    }
}
