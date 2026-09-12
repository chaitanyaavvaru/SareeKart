package com.sareekart.service.impl;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.*;
import com.sareekart.service.AnalyticsService;
import com.sareekart.util.AnalyticsCsvGenerator;
import com.sareekart.util.AnalyticsExcelGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;

    private static final BigDecimal SHIPPING_THRESHOLD = BigDecimal.valueOf(5000);
    private static final BigDecimal DEFAULT_SHIPPING_FEE = BigDecimal.valueOf(150);
    private static final BigDecimal GST_RATE = BigDecimal.valueOf(0.05);

    @Override
    public AnalyticsOverviewResponse getOverviewAnalytics(String range, LocalDate startDate, LocalDate endDate) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        return getOverview(dateRange, startDate, endDate);
    }

    @Override
    public AnalyticsOverviewResponse getOverview(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);

        // Fetch query aggregations (works with mock and real DB)
        BigDecimal currentGross = orderRepository.sumGrossSalesBetween(window.getCurrentStart(), window.getCurrentEnd());
        BigDecimal priorGross = orderRepository.sumGrossSalesBetween(window.getPriorStart(), window.getPriorEnd());
        Long currentCompleted = orderRepository.countCompletedOrdersBetween(window.getCurrentStart(), window.getCurrentEnd());
        Long priorCompleted = orderRepository.countCompletedOrdersBetween(window.getPriorStart(), window.getPriorEnd());
        BigDecimal currentShipping = orderRepository.sumShippingFeesBetween(window.getCurrentStart(), window.getCurrentEnd());
        BigDecimal priorShipping = orderRepository.sumShippingFeesBetween(window.getPriorStart(), window.getPriorEnd());

        // In-memory fallback if queries returned null / 0
        List<Order> currentOrders = orderRepository.findByCreatedAtBetweenAndStatusNot(
                window.getCurrentStart(), window.getCurrentEnd(), OrderStatus.CANCELLED);
        List<Order> priorOrders = orderRepository.findByCreatedAtBetweenAndStatusNot(
                window.getPriorStart(), window.getPriorEnd(), OrderStatus.CANCELLED);

        if (currentGross == null || (currentGross.compareTo(BigDecimal.ZERO) == 0 && !currentOrders.isEmpty())) {
            currentGross = computeGrossSalesFromOrders(currentOrders);
        }
        if (priorGross == null || (priorGross.compareTo(BigDecimal.ZERO) == 0 && !priorOrders.isEmpty())) {
            priorGross = computeGrossSalesFromOrders(priorOrders);
        }
        if (currentCompleted == null || (currentCompleted == 0 && !currentOrders.isEmpty())) {
            currentCompleted = (long) currentOrders.size();
        }
        if (priorCompleted == null || (priorCompleted == 0 && !priorOrders.isEmpty())) {
            priorCompleted = (long) priorOrders.size();
        }
        if (currentShipping == null || (currentShipping.compareTo(BigDecimal.ZERO) == 0 && !currentOrders.isEmpty())) {
            currentShipping = computeShippingFromOrders(currentOrders);
        }
        if (priorShipping == null || (priorShipping.compareTo(BigDecimal.ZERO) == 0 && !priorOrders.isEmpty())) {
            priorShipping = computeShippingFromOrders(priorOrders);
        }

        // Net revenue: use sumNetRevenueBetween if present, or fallback to grossSales
        BigDecimal currentNet = orderRepository.sumNetRevenueBetween(window.getCurrentStart(), window.getCurrentEnd());
        if (currentNet == null || currentNet.compareTo(BigDecimal.ZERO) == 0) {
            currentNet = computeNetRevenueFromOrders(currentOrders, currentGross);
        }
        BigDecimal priorNet = orderRepository.sumNetRevenueBetween(window.getPriorStart(), window.getPriorEnd());
        if (priorNet == null || priorNet.compareTo(BigDecimal.ZERO) == 0) {
            priorNet = computeNetRevenueFromOrders(priorOrders, priorGross);
        }

        BigDecimal currentTax = currentNet.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal priorTax = priorNet.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);

        BigDecimal currentAov = currentCompleted > 0
                ? currentNet.divide(BigDecimal.valueOf(currentCompleted), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        BigDecimal priorAov = priorCompleted > 0
                ? priorNet.divide(BigDecimal.valueOf(priorCompleted), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        Long currentUnits = orderItemRepository.sumTotalUnitsSoldBetween(window.getCurrentStart(), window.getCurrentEnd());
        if (currentUnits == null) currentUnits = computeUnitsFromOrders(currentOrders);
        Long priorUnits = orderItemRepository.sumTotalUnitsSoldBetween(window.getPriorStart(), window.getPriorEnd());
        if (priorUnits == null) priorUnits = computeUnitsFromOrders(priorOrders);

        // Percentage deltas
        Map<String, Double> percentageChanges = new LinkedHashMap<>();
        percentageChanges.put("grossSales", AnalyticsMathUtil.calculatePercentageDelta(currentGross, priorGross));
        percentageChanges.put("netRevenue", AnalyticsMathUtil.calculatePercentageDelta(currentNet, priorNet));
        percentageChanges.put("taxAmount", AnalyticsMathUtil.calculatePercentageDelta(currentTax, priorTax));
        percentageChanges.put("shippingAmount", AnalyticsMathUtil.calculatePercentageDelta(currentShipping, priorShipping));
        percentageChanges.put("aov", AnalyticsMathUtil.calculatePercentageDelta(currentAov, priorAov));
        percentageChanges.put("completedOrders", AnalyticsMathUtil.calculatePercentageDelta(currentCompleted, priorCompleted));
        percentageChanges.put("orders", AnalyticsMathUtil.calculatePercentageDelta(currentCompleted, priorCompleted));

        PeriodMetricsDto currentPeriod = PeriodMetricsDto.builder()
                .grossSales(currentGross)
                .netRevenue(currentNet)
                .taxAmount(currentTax)
                .shippingAmount(currentShipping)
                .aov(currentAov)
                .completedOrders(currentCompleted)
                .totalUnitsSold(currentUnits)
                .build();

        PeriodMetricsDto previousPeriod = PeriodMetricsDto.builder()
                .grossSales(priorGross)
                .netRevenue(priorNet)
                .taxAmount(priorTax)
                .shippingAmount(priorShipping)
                .aov(priorAov)
                .completedOrders(priorCompleted)
                .totalUnitsSold(priorUnits)
                .build();

        return AnalyticsOverviewResponse.builder()
                .currentPeriod(currentPeriod)
                .previousPeriod(previousPeriod)
                .grossSales(currentGross)
                .netRevenue(currentNet)
                .taxAmount(currentTax)
                .shippingAmount(currentShipping)
                .aov(currentAov)
                .completedOrders(currentCompleted)
                .totalUnitsSold(currentUnits)
                .priorGrossSales(priorGross)
                .priorNetRevenue(priorNet)
                .priorTaxAmount(priorTax)
                .priorShippingAmount(priorShipping)
                .priorAov(priorAov)
                .priorCompletedOrders(priorCompleted)
                .percentageChanges(percentageChanges)
                .range(range.name())
                .startDate(window.getCurrentStart().toLocalDate().toString())
                .endDate(window.getCurrentEnd().toLocalDate().toString())
                .priorStartDate(window.getPriorStart().toLocalDate().toString())
                .priorEndDate(window.getPriorEnd().toLocalDate().toString())
                .build();
    }

    @Override
    public SalesTelemetryResponse getSalesTelemetry(String range, LocalDate startDate, LocalDate endDate) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        return getSalesTelemetry(dateRange, startDate, endDate);
    }

    @Override
    public SalesTelemetryResponse getSalesTelemetry(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);

        BigDecimal grossSales = orderRepository.sumGrossSalesBetween(window.getCurrentStart(), window.getCurrentEnd());
        Long completedOrders = orderRepository.countCompletedOrdersBetween(window.getCurrentStart(), window.getCurrentEnd());
        BigDecimal shippingAmount = orderRepository.sumShippingFeesBetween(window.getCurrentStart(), window.getCurrentEnd());

        List<Order> orders = orderRepository.findByCreatedAtBetweenAndStatusNot(
                window.getCurrentStart(), window.getCurrentEnd(), OrderStatus.CANCELLED);

        if (grossSales == null || (grossSales.compareTo(BigDecimal.ZERO) == 0 && !orders.isEmpty())) {
            grossSales = computeGrossSalesFromOrders(orders);
        }
        if (completedOrders == null || (completedOrders == 0 && !orders.isEmpty())) {
            completedOrders = (long) orders.size();
        }
        if (shippingAmount == null || (shippingAmount.compareTo(BigDecimal.ZERO) == 0 && !orders.isEmpty())) {
            shippingAmount = computeShippingFromOrders(orders);
        }

        BigDecimal netRevenue = orderRepository.sumNetRevenueBetween(window.getCurrentStart(), window.getCurrentEnd());
        if (netRevenue == null || netRevenue.compareTo(BigDecimal.ZERO) == 0) {
            netRevenue = computeNetRevenueFromOrders(orders, grossSales);
        }

        BigDecimal taxAmount = netRevenue.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal aov = completedOrders > 0
                ? netRevenue.divide(BigDecimal.valueOf(completedOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        Long totalUnits = orderItemRepository.sumTotalUnitsSoldBetween(window.getCurrentStart(), window.getCurrentEnd());
        if (totalUnits == null) totalUnits = computeUnitsFromOrders(orders);

        // Continuous timeline points with zero-fill
        List<SalesTelemetryResponse.SalesTimelinePoint> timeline = buildTimelinePoints(
                window.getCurrentStart().toLocalDate(), window.getCurrentEnd().toLocalDate(), orders);

        // Payment method distribution
        List<SalesTelemetryResponse.PaymentDistributionItem> paymentDist = buildPaymentDistribution(orders, netRevenue);

        // Coupon utilization
        List<SalesTelemetryResponse.CouponUtilizationItem> couponUtil = buildCouponUtilization(orders);

        return SalesTelemetryResponse.builder()
                .grossSales(grossSales)
                .netRevenue(netRevenue)
                .taxAmount(taxAmount)
                .shippingAmount(shippingAmount)
                .discountAmount(grossSales.subtract(netRevenue).max(BigDecimal.ZERO))
                .aov(aov)
                .completedOrders(completedOrders)
                .totalOrders(completedOrders)
                .completedTransactions(completedOrders)
                .totalUnitsSold(totalUnits)
                .timeline(timeline)
                .paymentDistribution(paymentDist)
                .couponUtilization(couponUtil)
                .range(range.name())
                .startDate(window.getCurrentStart().toLocalDate().toString())
                .endDate(window.getCurrentEnd().toLocalDate().toString())
                .build();
    }

    @Override
    public InventoryVelocityResponse getInventoryVelocity() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        List<InventoryItem> items = inventoryItemRepository.findAll();
        if (items == null) items = Collections.emptyList();

        List<SkuVelocityItem> allSkus = new ArrayList<>();
        List<StockoutAlertItem> stockoutAlerts = new ArrayList<>();

        int totalAvailableStock = 0;
        BigDecimal totalValuation = BigDecimal.ZERO;
        int lowStockCount = 0;
        int outOfStockCount = 0;

        int freshCount = 0; long freshUnits = 0; BigDecimal freshVal = BigDecimal.ZERO;
        int midCount = 0; long midUnits = 0; BigDecimal midVal = BigDecimal.ZERO;
        int agedCount = 0; long agedUnits = 0; BigDecimal agedVal = BigDecimal.ZERO;

        for (InventoryItem item : items) {
            Long prodId = item.getProductId() != null ? item.getProductId() : item.getId();
            Long unitsSold = orderItemRepository.sumUnitsSoldByProductIdInPeriod(prodId, thirtyDaysAgo, now);
            if (unitsSold == null) unitsSold = 0L;

            double runRate = BigDecimal.valueOf(unitsSold / 30.0).setScale(2, RoundingMode.HALF_UP).doubleValue();

            int onHand = item.getOnHand() != null ? item.getOnHand() : 0;
            int reserved = item.getReserved() != null ? item.getReserved() : 0;
            int computedAvail = Math.max(0, onHand - reserved);
            int avail = (item.getAvailable() != null && item.getAvailable() > 0) ? item.getAvailable() : computedAvail;

            double doir = (runRate > 0) ? (avail / runRate) : (avail > 0 ? 999.0 : 0.0);
            if (doir > 999.0) doir = 999.0;
            doir = BigDecimal.valueOf(doir).setScale(1, RoundingMode.HALF_UP).doubleValue();

            BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal valuation = price.multiply(BigDecimal.valueOf(avail));

            totalAvailableStock += avail;
            totalValuation = totalValuation.add(valuation);

            if (avail == 0) outOfStockCount++;
            else if (avail <= 10) lowStockCount++;

            // Aging calculation based on updatedAt
            LocalDateTime updated = item.getUpdatedAt() != null ? item.getUpdatedAt() : now;
            long daysOld = ChronoUnit.DAYS.between(updated.toLocalDate(), now.toLocalDate());

            if (daysOld < 30) {
                freshCount++; freshUnits += avail; freshVal = freshVal.add(valuation);
            } else if (daysOld <= 90) {
                midCount++; midUnits += avail; midVal = midVal.add(valuation);
            } else {
                agedCount++; agedUnits += avail; agedVal = agedVal.add(valuation);
            }

            String status = avail == 0 ? "OUT_OF_STOCK" : (avail <= 10 ? "LOW_STOCK" : "OPTIMAL");

            SkuVelocityItem skuItem = SkuVelocityItem.builder()
                    .sku(item.getSku())
                    .name(item.getProductName())
                    .productName(item.getProductName())
                    .category(item.getCategory())
                    .warehouseCode(item.getWarehouseCode())
                    .warehouseName(item.getWarehouseName())
                    .availableStock(avail)
                    .unitsSold(unitsSold)
                    .unitsSold30d(unitsSold.intValue())
                    .runRate(runRate)
                    .dailyRunRate(runRate)
                    .daysRemaining(doir)
                    .daysOfInventoryRemaining((int) doir)
                    .unitPrice(price)
                    .status(status)
                    .build();
            allSkus.add(skuItem);

            // Stockout alert logic
            if (avail <= 10 || "OUT_OF_STOCK".equals(item.getStatus()) || "LOW_STOCK".equals(item.getStatus()) || doir <= 7.0) {
                int score = computePriorityScore(avail, runRate, (int) doir);
                String priorityLevel = getPriorityLevel(score);
                int reorderQty = Math.max(20, (int) Math.ceil(runRate * 30) - avail);

                stockoutAlerts.add(StockoutAlertItem.builder()
                        .sku(item.getSku())
                        .name(item.getProductName())
                        .productName(item.getProductName())
                        .warehouse(item.getWarehouseCode())
                        .warehouseCode(item.getWarehouseCode())
                        .warehouseName(item.getWarehouseName())
                        .binLocation(item.getBinLocation())
                        .available(avail)
                        .availableStock(avail)
                        .onHand(onHand)
                        .reserved(reserved)
                        .dailyRunRate(runRate)
                        .daysRemaining(doir)
                        .status(status)
                        .priorityScore(score)
                        .priorityLevel(priorityLevel)
                        .recommendedReorderQty(reorderQty)
                        .build());
            }
        }

        // Fast-moving SKUs (sorted by unitsSold descending)
        List<SkuVelocityItem> fastMoving = allSkus.stream()
                .sorted(Comparator.comparing(SkuVelocityItem::getUnitsSold).reversed()
                        .thenComparing(SkuVelocityItem::getDailyRunRate, Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        // Slow-moving SKUs (unitsSold ascending, availableStock descending)
        List<SkuVelocityItem> slowMoving = allSkus.stream()
                .filter(s -> s.getAvailableStock() != null && s.getAvailableStock() > 0)
                .sorted(Comparator.comparing(SkuVelocityItem::getUnitsSold)
                        .thenComparing(SkuVelocityItem::getAvailableStock, Comparator.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        stockoutAlerts.sort(Comparator.comparing(StockoutAlertItem::getPriorityScore).reversed());

        double totalRunRate = allSkus.stream().mapToDouble(SkuVelocityItem::getDailyRunRate).sum();
        double avgDoir = totalRunRate > 0 ? totalAvailableStock / totalRunRate : 999.0;
        if (avgDoir > 999.0) avgDoir = 999.0;
        avgDoir = BigDecimal.valueOf(avgDoir).setScale(1, RoundingMode.HALF_UP).doubleValue();

        List<InventoryAgingCategory> agingList = List.of(
                buildAgingCategory("<30 days", "< 30 Days", freshCount, freshUnits, freshVal, totalValuation),
                buildAgingCategory("30-90 days", "30–90 Days", midCount, midUnits, midVal, totalValuation),
                buildAgingCategory(">90 days", "> 90 Days", agedCount, agedUnits, agedVal, totalValuation)
        );

        return InventoryVelocityResponse.builder()
                .daysOfInventoryRemaining(avgDoir)
                .averageDaysRemaining((int) avgDoir)
                .totalAvailableStock(totalAvailableStock)
                .totalStockValuation(totalValuation)
                .totalValuation(totalValuation)
                .totalSkus(items.size())
                .lowStockCount(lowStockCount)
                .outOfStockCount(outOfStockCount)
                .fastMovingSkus(fastMoving)
                .slowMovingSkus(slowMoving)
                .agingCategories(agingList)
                .stockoutAlerts(stockoutAlerts)
                .build();
    }

    @Override
    public CustomerAnalyticsResponse getCustomerAnalytics(String range, LocalDate startDate, LocalDate endDate) {
        AnalyticsDateRange dateRange = AnalyticsDateRange.fromString(range);
        return getCustomerAnalytics(dateRange, startDate, endDate);
    }

    @Override
    public CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);

        List<Order> allOrders = orderRepository.findByStatusNot(OrderStatus.CANCELLED);
        if (allOrders == null) allOrders = Collections.emptyList();

        // Group historical completed orders by user
        Map<Long, List<Order>> ordersByUser = allOrders.stream()
                .filter(o -> o.getUser() != null && o.getUser().getId() != null)
                .collect(Collectors.groupingBy(o -> o.getUser().getId()));

        // 1. LTV Tiers across all customer history
        long platinumCount = 0; BigDecimal platinumSpend = BigDecimal.ZERO;
        long goldCount = 0; BigDecimal goldSpend = BigDecimal.ZERO;
        long silverCount = 0; BigDecimal silverSpend = BigDecimal.ZERO;

        for (List<Order> userOrders : ordersByUser.values()) {
            BigDecimal spend = userOrders.stream()
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (spend.compareTo(BigDecimal.valueOf(50000)) > 0) {
                platinumCount++;
                platinumSpend = platinumSpend.add(spend);
            } else if (spend.compareTo(BigDecimal.valueOf(15000)) >= 0) {
                goldCount++;
                goldSpend = goldSpend.add(spend);
            } else {
                silverCount++;
                silverSpend = silverSpend.add(spend);
            }
        }

        long totalCustomers = Math.max(1, ordersByUser.size());
        BigDecimal totalSpend = platinumSpend.add(goldSpend).add(silverSpend);

        List<LtvTierSummary> ltvTiers = List.of(
                buildLtvSummary("Platinum", "Platinum (>₹50,000)", platinumCount, platinumSpend, totalCustomers, totalSpend),
                buildLtvSummary("Gold", "Gold (₹15,000–₹50,000)", goldCount, goldSpend, totalCustomers, totalSpend),
                buildLtvSummary("Silver", "Silver (<₹15,000)", silverCount, silverSpend, totalCustomers, totalSpend)
        );

        // 2. New vs Returning in current window
        List<Order> windowOrders = orderRepository.findByCreatedAtBetweenAndStatusNot(
                window.getCurrentStart(), window.getCurrentEnd(), OrderStatus.CANCELLED);
        if (windowOrders == null) windowOrders = Collections.emptyList();

        Set<Long> usersInWindow = windowOrders.stream()
                .filter(o -> o.getUser() != null && o.getUser().getId() != null)
                .map(o -> o.getUser().getId())
                .collect(Collectors.toSet());

        long newCustomers = 0;
        long returningCustomers = 0;
        BigDecimal newRevenue = BigDecimal.ZERO;
        BigDecimal returningRevenue = BigDecimal.ZERO;

        for (Long uid : usersInWindow) {
            List<Order> history = ordersByUser.getOrDefault(uid, Collections.emptyList());
            LocalDateTime firstOrderDate = history.stream()
                    .map(Order::getCreatedAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(window.getCurrentStart());

            BigDecimal userWindowSpend = windowOrders.stream()
                    .filter(o -> o.getUser() != null && uid.equals(o.getUser().getId()))
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (firstOrderDate.isBefore(window.getCurrentStart())) {
                returningCustomers++;
                returningRevenue = returningRevenue.add(userWindowSpend);
            } else {
                newCustomers++;
                newRevenue = newRevenue.add(userWindowSpend);
            }
        }

        long totalActive = newCustomers + returningCustomers;
        double repeatRate = totalActive > 0 ? (returningCustomers * 100.0 / totalActive) : 0.0;
        repeatRate = BigDecimal.valueOf(repeatRate).setScale(1, RoundingMode.HALF_UP).doubleValue();

        CustomerCohortSummary cohortSummary = CustomerCohortSummary.builder()
                .newCustomers(newCustomers)
                .returningCustomers(returningCustomers)
                .totalOrderingCustomers(totalActive)
                .newRevenue(newRevenue)
                .returningRevenue(returningRevenue)
                .repeatPurchaseRate(repeatRate)
                .averageLtv(totalCustomers > 0 ? totalSpend.divide(BigDecimal.valueOf(totalCustomers), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO)
                .build();

        // 3. Regional breakdown
        Map<String, List<Order>> byState = windowOrders.stream()
                .filter(o -> o.getShippingAddress() != null && o.getShippingAddress().getState() != null)
                .collect(Collectors.groupingBy(o -> o.getShippingAddress().getState()));

        BigDecimal totalRegRevenue = windowOrders.stream()
                .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RegionDemandItem> topStates = byState.entrySet().stream()
                .map(e -> {
                    BigDecimal rev = e.getValue().stream()
                            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    double pct = totalRegRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? rev.multiply(BigDecimal.valueOf(100.0)).divide(totalRegRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return RegionDemandItem.builder()
                            .name(e.getKey())
                            .region(e.getKey())
                            .orderCount((long) e.getValue().size())
                            .revenue(rev)
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparing(RegionDemandItem::getRevenue).reversed())
                .limit(10)
                .collect(Collectors.toList());

        Map<String, List<Order>> byCity = windowOrders.stream()
                .filter(o -> o.getShippingAddress() != null && o.getShippingAddress().getCity() != null)
                .collect(Collectors.groupingBy(o -> o.getShippingAddress().getCity()));

        List<RegionDemandItem> topCities = byCity.entrySet().stream()
                .map(e -> {
                    BigDecimal rev = e.getValue().stream()
                            .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    String st = e.getValue().get(0).getShippingAddress().getState();
                    double pct = totalRegRevenue.compareTo(BigDecimal.ZERO) > 0
                            ? rev.multiply(BigDecimal.valueOf(100.0)).divide(totalRegRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    return RegionDemandItem.builder()
                            .name(e.getKey())
                            .region(e.getKey())
                            .state(st)
                            .orderCount((long) e.getValue().size())
                            .revenue(rev)
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparing(RegionDemandItem::getRevenue).reversed())
                .limit(10)
                .collect(Collectors.toList());

        RegionalDemandSummary regionalBreakdown = RegionalDemandSummary.builder()
                .topStates(topStates)
                .topCities(topCities)
                .build();

        // 4. Conversion Funnel
        long cartsCreated = cartRepository.count();
        long cartsWithItems = cartRepository.countCartsWithItems();
        long checkoutInitiated = orderRepository.countTotalOrdersBetween(window.getCurrentStart(), window.getCurrentEnd());
        long ordersCompleted = (long) windowOrders.size();

        double cartAbandonmentRate = cartsWithItems > 0
                ? Math.max(0.0, Math.min(100.0, ((1.0 - ((double) ordersCompleted / cartsWithItems)) * 100.0)))
                : 0.0;
        double overallConversionRate = cartsCreated > 0
                ? Math.min(100.0, ((double) ordersCompleted / cartsCreated) * 100.0)
                : 0.0;
        double checkoutAbandonmentRate = checkoutInitiated > 0
                ? Math.max(0.0, Math.min(100.0, ((1.0 - ((double) ordersCompleted / checkoutInitiated)) * 100.0)))
                : 0.0;

        ConversionFunnelSummary funnel = ConversionFunnelSummary.builder()
                .cartsCreated(cartsCreated)
                .cartsWithItems(cartsWithItems)
                .checkoutInitiated(checkoutInitiated)
                .ordersCompleted(ordersCompleted)
                .cartAbandonmentRate(BigDecimal.valueOf(cartAbandonmentRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .abandonmentRate(BigDecimal.valueOf(cartAbandonmentRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .checkoutAbandonmentRate(BigDecimal.valueOf(checkoutAbandonmentRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .overallConversionRate(BigDecimal.valueOf(overallConversionRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .conversionRate(BigDecimal.valueOf(overallConversionRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build();

        return CustomerAnalyticsResponse.builder()
                .range(range.name())
                .startDate(window.getCurrentStart().toLocalDate().toString())
                .endDate(window.getCurrentEnd().toLocalDate().toString())
                .ltvTiers(ltvTiers)
                .newVsReturning(cohortSummary)
                .regionalBreakdown(regionalBreakdown)
                .topStates(topStates)
                .topCities(topCities)
                .conversionFunnel(funnel)
                .build();
    }

    @Override
    public byte[] exportSalesTelemetry(String range, LocalDate startDate, LocalDate endDate, String format) {
        SalesTelemetryResponse data = getSalesTelemetry(range, startDate, endDate);
        String fmt = format != null ? format.toLowerCase() : "csv";
        return switch (fmt) {
            case "xlsx" -> AnalyticsExcelGenerator.generateSalesWorkbook(data);
            case "csv" -> AnalyticsCsvGenerator.generateSalesCsv(data);
            default -> throw new BadRequestException("Unsupported export format: " + format);
        };
    }

    @Override
    public byte[] exportInventoryVelocity(String format) {
        InventoryVelocityResponse data = getInventoryVelocity();
        String fmt = format != null ? format.toLowerCase() : "csv";
        return switch (fmt) {
            case "xlsx" -> AnalyticsExcelGenerator.generateInventoryWorkbook(data);
            case "csv" -> AnalyticsCsvGenerator.generateInventoryCsv(data);
            default -> throw new BadRequestException("Unsupported export format: " + format);
        };
    }

    @Override
    public byte[] exportSalesTelemetryExcel(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        return exportSalesTelemetry(range.name(), startDate, endDate, "xlsx");
    }

    @Override
    public byte[] exportSalesTelemetryCsv(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        return exportSalesTelemetry(range.name(), startDate, endDate, "csv");
    }

    @Override
    public byte[] exportInventoryVelocityExcel() {
        return exportInventoryVelocity("xlsx");
    }

    @Override
    public byte[] exportInventoryVelocityCsv() {
        return exportInventoryVelocity("csv");
    }

    // ==================== Private Helper Methods ====================

    private BigDecimal computeGrossSalesFromOrders(List<Order> orders) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Order o : orders) {
            if (o.getItems() != null) {
                for (OrderItem i : o.getItems()) {
                    BigDecimal price = i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO;
                    int qty = i.getQuantity() != null ? i.getQuantity() : 0;
                    sum = sum.add(price.multiply(BigDecimal.valueOf(qty)));
                }
            } else if (o.getTotalAmount() != null) {
                sum = sum.add(o.getTotalAmount());
            }
        }
        return sum;
    }

    private BigDecimal computeNetRevenueFromOrders(List<Order> orders, BigDecimal grossFallback) {
        if (orders.isEmpty()) return grossFallback != null ? grossFallback : BigDecimal.ZERO;
        BigDecimal sum = BigDecimal.ZERO;
        for (Order o : orders) {
            sum = sum.add(o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
        }
        return sum;
    }

    private BigDecimal computeShippingFromOrders(List<Order> orders) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Order o : orders) {
            BigDecimal subtotal = BigDecimal.ZERO;
            if (o.getItems() != null && !o.getItems().isEmpty()) {
                for (OrderItem i : o.getItems()) {
                    BigDecimal p = i.getPrice() != null ? i.getPrice() : BigDecimal.ZERO;
                    int q = i.getQuantity() != null ? i.getQuantity() : 0;
                    subtotal = subtotal.add(p.multiply(BigDecimal.valueOf(q)));
                }
            } else if (o.getTotalAmount() != null) {
                subtotal = o.getTotalAmount();
            }
            if (subtotal.compareTo(SHIPPING_THRESHOLD) < 0 && subtotal.compareTo(BigDecimal.ZERO) > 0) {
                sum = sum.add(DEFAULT_SHIPPING_FEE);
            }
        }
        return sum;
    }

    private Long computeUnitsFromOrders(List<Order> orders) {
        long units = 0;
        for (Order o : orders) {
            if (o.getItems() != null) {
                for (OrderItem i : o.getItems()) {
                    units += (i.getQuantity() != null ? i.getQuantity() : 0);
                }
            }
        }
        return units;
    }

    private List<SalesTelemetryResponse.SalesTimelinePoint> buildTimelinePoints(
            LocalDate start, LocalDate end, List<Order> orders) {
        Map<LocalDate, List<Order>> byDate = orders.stream()
                .filter(o -> o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<SalesTelemetryResponse.SalesTimelinePoint> points = new ArrayList<>();
        LocalDate curr = start;
        while (!curr.isAfter(end)) {
            List<Order> dayOrders = byDate.getOrDefault(curr, Collections.emptyList());
            BigDecimal dayRev = dayOrders.stream()
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            long dayUnits = dayOrders.stream()
                    .mapToLong(this::countOrderUnits)
                    .sum();

            points.add(SalesTelemetryResponse.SalesTimelinePoint.builder()
                    .date(curr.toString())
                    .revenue(dayRev)
                    .orders((long) dayOrders.size())
                    .units(dayUnits)
                    .build());

            curr = curr.plusDays(1);
        }
        return points;
    }

    private long countOrderUnits(Order o) {
        if (o.getItems() == null) return 0;
        return o.getItems().stream()
                .mapToLong(i -> i.getQuantity() != null ? i.getQuantity() : 0)
                .sum();
    }

    private List<SalesTelemetryResponse.PaymentDistributionItem> buildPaymentDistribution(
            List<Order> orders, BigDecimal totalRevenue) {
        Map<String, List<Order>> byMethod = orders.stream()
                .collect(Collectors.groupingBy(o -> {
                    String pm = o.getPaymentMethod();
                    if (pm == null || pm.isBlank()) return "COD";
                    String up = pm.toUpperCase();
                    if (up.contains("UPI")) return "UPI";
                    if (up.contains("CARD") || up.contains("RAZORPAY")) return "Cards";
                    if (up.contains("NET") || up.contains("BANK")) return "NetBanking";
                    return "COD";
                }));

        List<SalesTelemetryResponse.PaymentDistributionItem> items = new ArrayList<>();
        for (Map.Entry<String, List<Order>> entry : byMethod.entrySet()) {
            BigDecimal amt = entry.getValue().stream()
                    .map(o -> o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                    ? amt.multiply(BigDecimal.valueOf(100.0)).divide(totalRevenue, 1, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            items.add(SalesTelemetryResponse.PaymentDistributionItem.builder()
                    .method(entry.getKey())
                    .count((long) entry.getValue().size())
                    .amount(amt)
                    .percentage(pct)
                    .build());
        }
        items.sort(Comparator.comparing(SalesTelemetryResponse.PaymentDistributionItem::getAmount).reversed());
        return items;
    }

    private List<SalesTelemetryResponse.CouponUtilizationItem> buildCouponUtilization(List<Order> orders) {
        List<Coupon> coupons = couponRepository.findAll();
        List<SalesTelemetryResponse.CouponUtilizationItem> list = new ArrayList<>();
        for (Coupon c : coupons) {
            int uses = c.getTimesUsed() != null ? c.getTimesUsed() : 0;
            double discPct = c.getDiscountPercent() != null ? c.getDiscountPercent() : 10.0;
            BigDecimal estRev = BigDecimal.valueOf(uses * 12000.0);
            BigDecimal estDisc = estRev.multiply(BigDecimal.valueOf(discPct / 100.0)).setScale(2, RoundingMode.HALF_UP);
            double roi = estDisc.compareTo(BigDecimal.ZERO) > 0
                    ? estRev.divide(estDisc, 2, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            list.add(SalesTelemetryResponse.CouponUtilizationItem.builder()
                    .code(c.getCode())
                    .uses(uses)
                    .discountTotal(estDisc)
                    .revenueGenerated(estRev)
                    .roi(roi)
                    .build());
        }
        return list;
    }

    private int computePriorityScore(int avail, double runRate, int doir) {
        int stockPts = (avail == 0) ? 50 : (avail == 1 ? 45 : (avail == 2 ? 40 : (avail <= 5 ? 30 : (avail <= 10 ? 15 : 5))));
        int velPts = (runRate >= 5.0) ? 35 : (runRate >= 2.0 ? 25 : (runRate >= 1.0 ? 15 : (runRate > 0 ? 10 : 0)));
        int daysPts = (doir == 0) ? 15 : (doir <= 2 ? 12 : (doir <= 5 ? 8 : (doir <= 7 ? 5 : 0)));
        return Math.min(100, stockPts + velPts + daysPts);
    }

    private String getPriorityLevel(int score) {
        if (score >= 90) return "CRITICAL";
        if (score >= 70) return "HIGH";
        if (score >= 40) return "MEDIUM";
        return "LOW";
    }

    private InventoryAgingCategory buildAgingCategory(
            String category, String displayName, int count, long units, BigDecimal val, BigDecimal totalVal) {
        double pct = totalVal.compareTo(BigDecimal.ZERO) > 0
                ? val.multiply(BigDecimal.valueOf(100.0)).divide(totalVal, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        return InventoryAgingCategory.builder()
                .category(category)
                .displayName(displayName)
                .itemCount((long) count)
                .totalUnits(units)
                .valuation(val)
                .valuationPercentage(pct)
                .build();
    }

    private LtvTierSummary buildLtvSummary(
            String tier, String displayName, long count, BigDecimal spend, long totalCust, BigDecimal totalSpend) {
        BigDecimal avgSpend = count > 0 ? spend.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        double custPct = totalCust > 0 ? (count * 100.0 / totalCust) : 0.0;
        custPct = BigDecimal.valueOf(custPct).setScale(1, RoundingMode.HALF_UP).doubleValue();
        double revPct = totalSpend.compareTo(BigDecimal.ZERO) > 0
                ? spend.multiply(BigDecimal.valueOf(100.0)).divide(totalSpend, 1, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        return LtvTierSummary.builder()
                .tier(tier)
                .displayName(displayName)
                .customerCount(count)
                .totalRevenue(spend)
                .totalSpend(spend)
                .averageSpend(avgSpend)
                .averageLtv(avgSpend)
                .percentageOfCustomers(custPct)
                .percentageOfRevenue(revPct)
                .build();
    }
}
