package com.sareekart.controller;

import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.analytics.AnalyticsOverviewResponse;
import com.sareekart.dto.response.analytics.CustomerAnalyticsResponse;
import com.sareekart.dto.response.analytics.InventoryVelocityResponse;
import com.sareekart.dto.response.analytics.SalesTelemetryResponse;
import com.sareekart.exception.BadRequestException;
import com.sareekart.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/analytics")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
@Tag(name = "Admin Analytics", description = "Endpoints for sales telemetry, inventory velocity, customer retention cohorts, and report export")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/overview")
    @Operation(summary = "Get high-level analytics overview with period-over-period comparison")
    public ResponseEntity<ApiResponse<AnalyticsOverviewResponse>> getOverview(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(range, startDate, endDate);
        AnalyticsOverviewResponse response = analyticsService.getOverviewAnalytics(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/sales")
    @Operation(summary = "Get detailed sales telemetry, timeline, payment splits, and coupon ROI")
    public ResponseEntity<ApiResponse<SalesTelemetryResponse>> getSalesTelemetry(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(range, startDate, endDate);
        SalesTelemetryResponse response = analyticsService.getSalesTelemetry(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/inventory")
    @Operation(summary = "Get inventory velocity, run rates, days remaining, aging categories, and stockout alerts")
    public ResponseEntity<ApiResponse<InventoryVelocityResponse>> getInventoryVelocity() {
        InventoryVelocityResponse response = analyticsService.getInventoryVelocity();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/customers")
    @Operation(summary = "Get customer LTV tiers, new vs returning revenue, regional breakdown, and conversion funnel")
    public ResponseEntity<ApiResponse<CustomerAnalyticsResponse>> getCustomerAnalytics(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        validateDateRange(range, startDate, endDate);
        CustomerAnalyticsResponse response = analyticsService.getCustomerAnalytics(range, startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/export/sales")
    @Operation(summary = "Export sales telemetry as CSV or Excel (.xlsx) file")
    public ResponseEntity<byte[]> exportSales(
            @RequestParam(defaultValue = "30D") String range,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "csv") String format) {
        validateDateRange(range, startDate, endDate);
        String fmt = format.toLowerCase();
        if (!fmt.equals("csv") && !fmt.equals("xlsx")) {
            throw new BadRequestException("Unsupported export format: " + format + ". Supported formats are 'csv' and 'xlsx'.");
        }

        byte[] fileBytes = analyticsService.exportSalesTelemetry(range, startDate, endDate, fmt);
        String filename = "sales_telemetry_report_" + range.toLowerCase() + "." + fmt;
        MediaType mediaType = fmt.equals("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(fileBytes);
    }

    @GetMapping("/export/inventory")
    @Operation(summary = "Export inventory velocity report as CSV or Excel (.xlsx) file")
    public ResponseEntity<byte[]> exportInventory(@RequestParam(defaultValue = "csv") String format) {
        String fmt = format.toLowerCase();
        if (!fmt.equals("csv") && !fmt.equals("xlsx")) {
            throw new BadRequestException("Unsupported export format: " + format + ". Supported formats are 'csv' and 'xlsx'.");
        }

        byte[] fileBytes = analyticsService.exportInventoryVelocity(fmt);
        String filename = "inventory_velocity_report." + fmt;
        MediaType mediaType = fmt.equals("xlsx")
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : MediaType.parseMediaType("text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(mediaType)
                .body(fileBytes);
    }

    private void validateDateRange(String range, LocalDate startDate, LocalDate endDate) {
        if ("CUSTOM".equalsIgnoreCase(range)) {
            if (startDate == null || endDate == null) {
                throw new BadRequestException("Start date and end date are required for CUSTOM range.");
            }
            if (startDate.isAfter(endDate)) {
                throw new BadRequestException("Start date cannot be after end date.");
            }
        }
    }
}
