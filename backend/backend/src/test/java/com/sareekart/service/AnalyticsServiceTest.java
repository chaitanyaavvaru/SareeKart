package com.sareekart.service;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.entity.InventoryItem;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.CouponRepository;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.OrderItemRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.service.impl.AnalyticsServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AnalyticsServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private InventoryItemRepository inventoryItemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    @Test
    void testOverviewAnalytics_30D_ComputesAccurateMetricsAndDeltas() {
        // Mock current period: 10 orders, 100,000 gross
        when(orderRepository.sumGrossSalesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("100000.00")) // current
                .thenReturn(new BigDecimal("80000.00"));  // prior

        when(orderRepository.countCompletedOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L)
                .thenReturn(8L);

        when(orderRepository.sumShippingFeesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("1500.00"))
                .thenReturn(new BigDecimal("1200.00"));

        AnalyticsOverviewResponse response = analyticsService.getOverviewAnalytics("30D", null, null);

        assertNotNull(response);
        assertNotNull(response.getCurrentPeriod());
        assertNotNull(response.getPreviousPeriod());
        assertEquals(new BigDecimal("100000.00"), response.getCurrentPeriod().getGrossSales());
        assertEquals(10L, response.getCurrentPeriod().getCompletedOrders());

        // AOV = 100000 / 10 = 10000
        assertEquals(new BigDecimal("10000.00"), response.getCurrentPeriod().getAov());

        // Delta for grossSales: (100000 - 80000) / 80000 * 100 = +25.0%
        Double delta = response.getPercentageChanges().get("grossSales");
        assertNotNull(delta);
        assertEquals(25.0, delta, 0.01);
    }

    @Test
    void testOverviewAnalytics_ZeroPriorPeriod_PreventsDivisionByZero() {
        when(orderRepository.sumGrossSalesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("50000.00"))
                .thenReturn(BigDecimal.ZERO);

        when(orderRepository.countCompletedOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L)
                .thenReturn(0L);

        when(orderRepository.sumShippingFeesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO)
                .thenReturn(BigDecimal.ZERO);

        AnalyticsOverviewResponse response = analyticsService.getOverviewAnalytics("7D", null, null);

        assertNotNull(response);
        Double delta = response.getPercentageChanges().get("grossSales");
        assertNotNull(delta);
        assertFalse(Double.isNaN(delta));
        assertFalse(Double.isInfinite(delta));
        assertEquals(100.0, delta, 0.01);
    }

    @Test
    void testInventoryVelocity_RunRateAndDaysRemaining() {
        InventoryItem item = InventoryItem.builder()
                .id(1L)
                .sku("SK-BANARASI-1")
                .productName("Royal Banarasi")
                .warehouseCode("WH-01")
                .warehouseName("Central Hub")
                .onHand(20)
                .reserved(2)
                .unitPrice(new BigDecimal("18000.00"))
                .status("OPTIMAL")
                .build();

        when(inventoryItemRepository.findAll()).thenReturn(List.of(item));
        when(orderItemRepository.sumUnitsSoldByProductIdInPeriod(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);

        InventoryVelocityResponse response = analyticsService.getInventoryVelocity();

        assertNotNull(response);
        assertNotNull(response.getFastMovingSkus());
        assertFalse(response.getFastMovingSkus().isEmpty());
        SkuVelocityItem sku = response.getFastMovingSkus().get(0);
        assertEquals("SK-BANARASI-1", sku.getSku());
        assertEquals(15L, sku.getUnitsSold());
        // dailyRunRate = 15 / 30 = 0.5
        assertEquals(0.5, sku.getDailyRunRate(), 0.01);
        // available = 18; daysRemaining = 18 / 0.5 = 36.0 days
        assertEquals(36.0, sku.getDaysRemaining(), 0.01);
    }

    @Test
    void testExportSalesTelemetry_GeneratesValidExcelAndCsv() throws Exception {
        when(orderRepository.sumGrossSalesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(new BigDecimal("50000.00"));
        when(orderRepository.countCompletedOrdersBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(5L);
        when(orderRepository.sumShippingFeesBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(BigDecimal.ZERO);

        // CSV export
        byte[] csvBytes = analyticsService.exportSalesTelemetry("30D", null, null, "csv");
        assertNotNull(csvBytes);
        assertTrue(csvBytes.length > 0);
        String csvContent = new String(csvBytes);
        assertTrue(csvContent.contains("Date,Gross Sales,Net Revenue,Orders,Units"));

        // Excel export
        byte[] xlsxBytes = analyticsService.exportSalesTelemetry("30D", null, null, "xlsx");
        assertNotNull(xlsxBytes);
        assertTrue(xlsxBytes.length > 0);
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(xlsxBytes))) {
            assertNotNull(wb.getSheet("Sales Telemetry"));
        }
    }

    @Test
    void testDateRangeCalculation() {
        DateRangeWindow todayWindow = DateRangeWindow.calculate(AnalyticsDateRange.TODAY, null, null);
        assertEquals(1, todayWindow.getDaysInWindow());

        DateRangeWindow weekWindow = DateRangeWindow.calculate(AnalyticsDateRange._7D, null, null);
        assertEquals(7, weekWindow.getDaysInWindow());

        DateRangeWindow monthWindow = DateRangeWindow.calculate(AnalyticsDateRange._30D, null, null);
        assertEquals(30, monthWindow.getDaysInWindow());

        DateRangeWindow quarterWindow = DateRangeWindow.calculate(AnalyticsDateRange._90D, null, null);
        assertEquals(90, quarterWindow.getDaysInWindow());

        LocalDate start = LocalDate.of(2026, 8, 1);
        LocalDate end = LocalDate.of(2026, 8, 10);
        DateRangeWindow customWindow = DateRangeWindow.calculate(AnalyticsDateRange.CUSTOM, start, end);
        assertEquals(10, customWindow.getDaysInWindow());
    }

    @Test
    void testCustomerAnalytics_BasicCalculation() {
        when(orderRepository.findByStatusNot(any())).thenReturn(Collections.emptyList());
        when(orderRepository.findByCreatedAtBetweenAndStatusNot(any(), any(), any())).thenReturn(Collections.emptyList());
        when(cartRepository.count()).thenReturn(10L);
        when(cartRepository.countCartsWithItems()).thenReturn(5L);
        when(orderRepository.countTotalOrdersBetween(any(), any())).thenReturn(2L);

        CustomerAnalyticsResponse response = analyticsService.getCustomerAnalytics("30D", null, null);
        assertNotNull(response);
        assertNotNull(response.getLtvTiers());
        assertEquals(3, response.getLtvTiers().size());
        assertNotNull(response.getConversionFunnel());
    }
}
