package com.sareekart.service;

import com.sareekart.dto.response.analytics.*;

import java.time.LocalDate;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverviewAnalytics(String range, LocalDate startDate, LocalDate endDate);

    AnalyticsOverviewResponse getOverview(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    SalesTelemetryResponse getSalesTelemetry(String range, LocalDate startDate, LocalDate endDate);

    SalesTelemetryResponse getSalesTelemetry(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    InventoryVelocityResponse getInventoryVelocity();

    CustomerAnalyticsResponse getCustomerAnalytics(String range, LocalDate startDate, LocalDate endDate);

    CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportSalesTelemetry(String range, LocalDate startDate, LocalDate endDate, String format);

    byte[] exportInventoryVelocity(String format);

    byte[] exportSalesTelemetryExcel(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportSalesTelemetryCsv(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportInventoryVelocityExcel();

    byte[] exportInventoryVelocityCsv();
}
