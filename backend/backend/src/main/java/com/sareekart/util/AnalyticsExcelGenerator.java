package com.sareekart.util;

import com.sareekart.dto.response.analytics.InventoryVelocityResponse;
import com.sareekart.dto.response.analytics.SalesTelemetryResponse;
import com.sareekart.dto.response.analytics.SkuVelocityItem;
import com.sareekart.dto.response.analytics.StockoutAlertItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

public final class AnalyticsExcelGenerator {

    private AnalyticsExcelGenerator() {}

    public static byte[] generateSalesWorkbook(SalesTelemetryResponse data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle integerStyle = createIntegerStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);

            // Sheet 1: Sales Telemetry (Overview)
            Sheet s1 = workbook.createSheet("Sales Telemetry");
            createTitleRow(s1, "SareeKart Sales & Financial Telemetry", headerStyle);
            int r = 2;
            createKpiRow(s1, r++, "Date Range", data.getRange() != null ? data.getRange() : "30D", null);
            createKpiRow(s1, r++, "Window", String.valueOf(data.getStartDate()) + " to " + String.valueOf(data.getEndDate()), null);
            createKpiRow(s1, r++, "Gross Sales", data.getGrossSales() != null ? data.getGrossSales() : BigDecimal.ZERO, currencyStyle);
            createKpiRow(s1, r++, "Net Revenue", data.getNetRevenue() != null ? data.getNetRevenue() : BigDecimal.ZERO, currencyStyle);
            createKpiRow(s1, r++, "GST Tax (5%)", data.getTaxAmount() != null ? data.getTaxAmount() : BigDecimal.ZERO, currencyStyle);
            createKpiRow(s1, r++, "Shipping Collected", data.getShippingAmount() != null ? data.getShippingAmount() : BigDecimal.ZERO, currencyStyle);
            createKpiRow(s1, r++, "Average Order Value (AOV)", data.getAov() != null ? data.getAov() : BigDecimal.ZERO, currencyStyle);
            createKpiRow(s1, r++, "Completed Orders", data.getCompletedOrders() != null ? data.getCompletedOrders() : 0L, integerStyle);
            createKpiRow(s1, r++, "Total Units Sold", data.getTotalUnitsSold() != null ? data.getTotalUnitsSold() : 0L, integerStyle);

            // Sheet 2: Daily Timeline
            Sheet s2 = workbook.createSheet("Daily Timeline");
            Row h2 = s2.createRow(0);
            String[] cols2 = {"Date", "Gross Sales", "Net Revenue", "Orders", "Units", "GST (5%)", "Shipping"};
            for (int i = 0; i < cols2.length; i++) {
                Cell c = h2.createCell(i);
                c.setCellValue(cols2[i]);
                c.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            if (data.getTimeline() != null) {
                for (SalesTelemetryResponse.SalesTimelinePoint d : data.getTimeline()) {
                    Row row = s2.createRow(rowIdx++);
                    Cell c0 = row.createCell(0);
                    c0.setCellValue(d.getDate() != null ? d.getDate() : "");
                    c0.setCellStyle(dateStyle);

                    Cell c1 = row.createCell(1);
                    c1.setCellValue(d.getRevenue() != null ? d.getRevenue().doubleValue() : 0.0);
                    c1.setCellStyle(currencyStyle);

                    Cell c2 = row.createCell(2);
                    c2.setCellValue(d.getRevenue() != null ? d.getRevenue().doubleValue() : 0.0);
                    c2.setCellStyle(currencyStyle);

                    Cell c3 = row.createCell(3);
                    c3.setCellValue(d.getOrders() != null ? d.getOrders() : 0L);
                    c3.setCellStyle(integerStyle);

                    Cell c4 = row.createCell(4);
                    c4.setCellValue(d.getUnits() != null ? d.getUnits() : 0L);
                    c4.setCellStyle(integerStyle);

                    Cell c5 = row.createCell(5);
                    double gst = d.getRevenue() != null ? d.getRevenue().multiply(BigDecimal.valueOf(0.05)).doubleValue() : 0.0;
                    c5.setCellValue(gst);
                    c5.setCellStyle(currencyStyle);

                    Cell c6 = row.createCell(6);
                    c6.setCellValue(0.0);
                    c6.setCellStyle(currencyStyle);
                }
            }

            autoSize(s1, 2);
            autoSize(s2, cols2.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Sales Excel report", e);
        }
    }

    public static byte[] generateInventoryWorkbook(InventoryVelocityResponse data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle integerStyle = createIntegerStyle(workbook);

            // Sheet 1: Stockout Alerts
            Sheet s1 = workbook.createSheet("Stockout Alerts");
            Row h1 = s1.createRow(0);
            String[] cols1 = {"Priority", "Score", "SKU", "Product Name", "Warehouse", "Stock Available", "Run-Rate", "Days Remaining", "Reorder Qty"};
            for (int i = 0; i < cols1.length; i++) {
                Cell c = h1.createCell(i);
                c.setCellValue(cols1[i]);
                c.setCellStyle(headerStyle);
            }
            int r1 = 1;
            if (data.getStockoutAlerts() != null) {
                for (StockoutAlertItem a : data.getStockoutAlerts()) {
                    Row row = s1.createRow(r1++);
                    row.createCell(0).setCellValue(a.getPriorityLevel() != null ? a.getPriorityLevel() : "LOW");
                    row.createCell(1).setCellValue(a.getPriorityScore() != null ? a.getPriorityScore() : 0);
                    row.createCell(2).setCellValue(a.getSku() != null ? a.getSku() : "");
                    row.createCell(3).setCellValue(a.getProductName() != null ? a.getProductName() : "");
                    row.createCell(4).setCellValue(a.getWarehouseCode() != null ? a.getWarehouseCode() : "");
                    row.createCell(5).setCellValue(a.getAvailable() != null ? a.getAvailable() : 0);
                    row.createCell(6).setCellValue(a.getDailyRunRate() != null ? a.getDailyRunRate() : 0.0);
                    row.createCell(7).setCellValue(a.getDaysRemaining() != null ? a.getDaysRemaining() : 0.0);
                    row.createCell(8).setCellValue(a.getRecommendedReorderQty() != null ? a.getRecommendedReorderQty() : 0);
                }
            }

            // Sheet 2: SKU Velocity Rankings
            Sheet s2 = workbook.createSheet("SKU Velocity Rankings");
            Row h2 = s2.createRow(0);
            String[] cols2 = {"Classification", "SKU", "Product Name", "Category", "Units Sold", "Run-Rate", "Available Stock", "DOIR"};
            for (int i = 0; i < cols2.length; i++) {
                Cell c = h2.createCell(i);
                c.setCellValue(cols2[i]);
                c.setCellStyle(headerStyle);
            }
            int r2 = 1;
            if (data.getFastMovingSkus() != null) {
                for (SkuVelocityItem s : data.getFastMovingSkus()) {
                    Row row = s2.createRow(r2++);
                    row.createCell(0).setCellValue("FAST_MOVING");
                    row.createCell(1).setCellValue(s.getSku() != null ? s.getSku() : "");
                    row.createCell(2).setCellValue(s.getProductName() != null ? s.getProductName() : "");
                    row.createCell(3).setCellValue(s.getCategory() != null ? s.getCategory() : "");
                    row.createCell(4).setCellValue(s.getUnitsSold() != null ? s.getUnitsSold() : 0L);
                    row.createCell(5).setCellValue(s.getDailyRunRate() != null ? s.getDailyRunRate() : 0.0);
                    row.createCell(6).setCellValue(s.getAvailableStock() != null ? s.getAvailableStock() : 0);
                    row.createCell(7).setCellValue(s.getDaysRemaining() != null ? s.getDaysRemaining() : 0.0);
                }
            }
            if (data.getSlowMovingSkus() != null) {
                for (SkuVelocityItem s : data.getSlowMovingSkus()) {
                    Row row = s2.createRow(r2++);
                    row.createCell(0).setCellValue("SLOW_MOVING");
                    row.createCell(1).setCellValue(s.getSku() != null ? s.getSku() : "");
                    row.createCell(2).setCellValue(s.getProductName() != null ? s.getProductName() : "");
                    row.createCell(3).setCellValue(s.getCategory() != null ? s.getCategory() : "");
                    row.createCell(4).setCellValue(s.getUnitsSold() != null ? s.getUnitsSold() : 0L);
                    row.createCell(5).setCellValue(s.getDailyRunRate() != null ? s.getDailyRunRate() : 0.0);
                    row.createCell(6).setCellValue(s.getAvailableStock() != null ? s.getAvailableStock() : 0);
                    row.createCell(7).setCellValue(s.getDaysRemaining() != null ? s.getDaysRemaining() : 0.0);
                }
            }

            autoSize(s1, cols1.length);
            autoSize(s2, cols2.length);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Inventory Excel report", e);
        }
    }

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.ROYAL_BLUE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static CellStyle createCurrencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("₹#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private static CellStyle createIntegerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("#,##0"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }

    private static CellStyle createDateStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private static void autoSize(Sheet sheet, int cols) {
        for (int i = 0; i < cols; i++) {
            sheet.autoSizeColumn(i);
            sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1024, 15 * 256));
        }
    }

    private static void createTitleRow(Sheet sheet, String title, CellStyle style) {
        Row r = sheet.createRow(0);
        Cell c = r.createCell(0);
        c.setCellValue(title);
    }

    private static void createKpiRow(Sheet sheet, int rowNum, String label, Object val, CellStyle style) {
        Row r = sheet.createRow(rowNum);
        r.createCell(0).setCellValue(label);
        Cell c = r.createCell(1);
        if (val instanceof Number n) {
            c.setCellValue(n.doubleValue());
            if (style != null) c.setCellStyle(style);
        } else {
            c.setCellValue(String.valueOf(val));
        }
    }
}
