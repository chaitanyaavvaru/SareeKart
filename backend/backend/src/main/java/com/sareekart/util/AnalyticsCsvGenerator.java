package com.sareekart.util;

import com.sareekart.dto.response.analytics.InventoryVelocityResponse;
import com.sareekart.dto.response.analytics.SalesTelemetryResponse;
import com.sareekart.dto.response.analytics.StockoutAlertItem;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;

public final class AnalyticsCsvGenerator {

    private static final String CRLF = "\r\n";
    private static final String BOM = "\uFEFF"; // UTF-8 BOM for Microsoft Excel

    private AnalyticsCsvGenerator() {}

    public static byte[] generateSalesCsv(SalesTelemetryResponse data) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Date,Gross Sales,Net Revenue,Orders,Units,GST (5%),Shipping").append(CRLF);

        if (data.getTimeline() != null && !data.getTimeline().isEmpty()) {
            for (SalesTelemetryResponse.SalesTimelinePoint d : data.getTimeline()) {
                BigDecimal rev = d.getRevenue() != null ? d.getRevenue() : BigDecimal.ZERO;
                BigDecimal gst = rev.multiply(BigDecimal.valueOf(0.05)).setScale(2, RoundingMode.HALF_UP);
                sb.append(escape(d.getDate())).append(",")
                  .append(rev).append(",")
                  .append(rev).append(",")
                  .append(d.getOrders() != null ? d.getOrders() : 0).append(",")
                  .append(d.getUnits() != null ? d.getUnits() : 0).append(",")
                  .append(gst).append(",")
                  .append("0.00").append(CRLF);
            }
        } else {
            // Include summary row if timeline is empty
            BigDecimal gross = data.getGrossSales() != null ? data.getGrossSales() : BigDecimal.ZERO;
            BigDecimal net = data.getNetRevenue() != null ? data.getNetRevenue() : BigDecimal.ZERO;
            BigDecimal tax = data.getTaxAmount() != null ? data.getTaxAmount() : BigDecimal.ZERO;
            BigDecimal ship = data.getShippingAmount() != null ? data.getShippingAmount() : BigDecimal.ZERO;
            long orders = data.getCompletedOrders() != null ? data.getCompletedOrders() : 0L;
            long units = data.getTotalUnitsSold() != null ? data.getTotalUnitsSold() : 0L;
            sb.append(escape(String.valueOf(data.getRange()))).append(",")
              .append(gross).append(",")
              .append(net).append(",")
              .append(orders).append(",")
              .append(units).append(",")
              .append(tax).append(",")
              .append(ship).append(CRLF);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateInventoryCsv(InventoryVelocityResponse data) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Priority Level,Priority Score,SKU,Product Name,Warehouse,Available Stock,Daily Run-Rate,Days Remaining,Recommended Reorder").append(CRLF);

        if (data.getStockoutAlerts() != null) {
            for (StockoutAlertItem a : data.getStockoutAlerts()) {
                sb.append(escape(a.getPriorityLevel())).append(",")
                  .append(a.getPriorityScore() != null ? a.getPriorityScore() : 0).append(",")
                  .append(escape(a.getSku())).append(",")
                  .append(escape(a.getProductName())).append(",")
                  .append(escape(a.getWarehouseCode())).append(",")
                  .append(a.getAvailable() != null ? a.getAvailable() : 0).append(",")
                  .append(a.getDailyRunRate() != null ? a.getDailyRunRate() : 0.0).append(",")
                  .append(a.getDaysRemaining() != null ? a.getDaysRemaining() : 0.0).append(",")
                  .append(a.getRecommendedReorderQty() != null ? a.getRecommendedReorderQty() : 0).append(CRLF);
            }
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escape(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n") || val.contains("\r")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
