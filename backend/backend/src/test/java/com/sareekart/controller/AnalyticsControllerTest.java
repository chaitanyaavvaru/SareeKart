package com.sareekart.controller;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.service.AnalyticsService;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

@ExtendWith(MockitoExtension.class)
public class AnalyticsControllerTest {

    @Mock
    private AnalyticsService analyticsService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new AnalyticsController(analyticsService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void testGetOverview_Success() throws Exception {
        AnalyticsOverviewResponse mockResponse = AnalyticsOverviewResponse.builder()
                .currentPeriod(PeriodMetricsDto.builder()
                        .grossSales(new BigDecimal("100000.00"))
                        .netRevenue(new BigDecimal("93750.00"))
                        .taxAmount(new BigDecimal("4750.00"))
                        .shippingAmount(new BigDecimal("1500.00"))
                        .aov(new BigDecimal("10000.00"))
                        .completedOrders(10L)
                        .build())
                .previousPeriod(PeriodMetricsDto.builder()
                        .grossSales(new BigDecimal("80000.00"))
                        .completedOrders(8L)
                        .build())
                .percentageChanges(Map.of("grossSales", 25.0, "orders", 25.0))
                .startDate(LocalDate.now().minusDays(30))
                .endDate(LocalDate.now())
                .build();

        when(analyticsService.getOverviewAnalytics(eq("30D"), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/analytics/overview?range=30D")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentPeriod.grossSales").value(100000.00))
                .andExpect(jsonPath("$.data.percentageChanges.grossSales").value(25.0));
    }

    @Test
    void testGetOverview_CustomRangeMissingDates_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview?range=CUSTOM")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Start date and end date are required for CUSTOM range."));
    }

    @Test
    void testGetOverview_InvertedDates_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/overview?range=CUSTOM&startDate=2026-09-10&endDate=2026-09-01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Start date cannot be after end date."));
    }

    @Test
    void testGetSalesTelemetry_Success() throws Exception {
        SalesTelemetryResponse mockResponse = SalesTelemetryResponse.builder()
                .grossSales(new BigDecimal("50000.00"))
                .netRevenue(new BigDecimal("47000.00"))
                .taxAmount(new BigDecimal("2350.00"))
                .shippingAmount(new BigDecimal("650.00"))
                .aov(new BigDecimal("10000.00"))
                .totalOrders(5L)
                .completedTransactions(5L)
                .timeline(Collections.emptyList())
                .paymentDistribution(Collections.emptyList())
                .couponUtilization(Collections.emptyList())
                .build();

        when(analyticsService.getSalesTelemetry(eq("30D"), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/analytics/sales?range=30D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.grossSales").value(50000.00));
    }

    @Test
    void testGetInventoryVelocity_Success() throws Exception {
        InventoryVelocityResponse mockResponse = InventoryVelocityResponse.builder()
                .daysOfInventoryRemaining(45.5)
                .fastMovingSkus(Collections.emptyList())
                .slowMovingSkus(Collections.emptyList())
                .agingCategories(InventoryAgingDto.builder().build())
                .stockoutAlerts(Collections.emptyList())
                .build();

        when(analyticsService.getInventoryVelocity()).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/analytics/inventory"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.daysOfInventoryRemaining").value(45.5));
    }

    @Test
    void testGetCustomerAnalytics_Success() throws Exception {
        CustomerAnalyticsResponse mockResponse = CustomerAnalyticsResponse.builder()
                .ltvTiers(Collections.emptyList())
                .newVsReturning(NewVsReturningDto.builder().repeatPurchaseRate(33.3).build())
                .regionalBreakdown(RegionalBreakdownDto.builder().build())
                .conversionFunnel(ConversionFunnelDto.builder().conversionRate(12.5).build())
                .build();

        when(analyticsService.getCustomerAnalytics(eq("30D"), any(), any())).thenReturn(mockResponse);

        mockMvc.perform(get("/api/admin/analytics/customers?range=30D"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.newVsReturning.repeatPurchaseRate").value(33.3));
    }

    @Test
    void testExportSales_Csv_ReturnsFileAttachment() throws Exception {
        byte[] mockCsv = "Date,Gross Sales\n2026-09-01,15000\n".getBytes();
        when(analyticsService.exportSalesTelemetry(eq("30D"), any(), any(), eq("csv"))).thenReturn(mockCsv);

        mockMvc.perform(get("/api/admin/analytics/export/sales?range=30D&format=csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("sales_telemetry_report_30d.csv")))
                .andExpect(content().bytes(mockCsv));
    }

    @Test
    void testExportSales_InvalidFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/export/sales?format=pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(Matchers.containsString("Unsupported export format: pdf")));
    }

    @Test
    void testExportInventory_Success() throws Exception {
        byte[] mockCsv = "Priority Level,Priority Score\nCRITICAL,95\n".getBytes();
        when(analyticsService.exportInventoryVelocity(eq("csv"))).thenReturn(mockCsv);

        mockMvc.perform(get("/api/admin/analytics/export/inventory?format=csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
                .andExpect(header().string("Content-Disposition", Matchers.containsString("inventory_velocity_report.csv")))
                .andExpect(content().bytes(mockCsv));
    }

    @Test
    void testAccessDeniedException_ReturnsForbiddenWithExactMessage() throws Exception {
        when(analyticsService.getOverviewAnalytics(eq("30D"), any(), any()))
                .thenThrow(new org.springframework.security.access.AccessDeniedException("Access is denied"));

        mockMvc.perform(get("/api/admin/analytics/overview?range=30D"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
    }
}
