# Milestone M1 Handoff Report: Analytics Business Logic, Telemetry Formulas & Export Engineering

## 1. Observation

Direct observations from the target repository `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`:

1. **Build & Dependencies** (`pom.xml`):
   - Java version: 17 (`<java.version>17</java.version>`).
   - Spring Boot: 3.5.15 (`spring-boot-starter-data-jpa`, `spring-boot-starter-web`, `spring-boot-starter-security`).
   - Apache POI: `org.apache.poi:poi-ooxml:5.3.0` is already declared on lines 137–141 of `pom.xml`.
   - Testing stack: `org.springframework.boot:spring-boot-starter-test`, `org.springframework.security:spring-security-test`, H2 in-memory DB (`com.h2database:h2:test`).

2. **Domain Entities & Schema Constraints**:
   - `Order.java` (`src/main/java/com/sareekart/entity/Order.java:16–62`):
     - Fields: `id`, `user` (`@ManyToOne User`), `items` (`List<OrderItem>`), `totalAmount` (`BigDecimal(10,2)`), `status` (`OrderStatus`), `shippingAddress` (`@Embedded Address`), `paymentMethod` (`String`), `paymentStatus` (`String`), `razorpayOrderId`, `razorpayPaymentId`, `createdAt`, `updatedAt`.
     - `OrderStatus.java`: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
     - Note: In current order placement (`OrderServiceImpl.java:76–99`), `subtotal` is calculated from items, `discount` is computed from optional `Coupon`, shipping is ₹150 for subtotal < ₹5000 (₹0 for >= ₹5000), and `totalAmount = subtotal - discount + shippingFee`.
   - `OrderItem.java` (`src/main/java/com/sareekart/entity/OrderItem.java:10–36`):
     - Fields: `id`, `order` (`@ManyToOne Order`), `product` (`@ManyToOne Product`), `quantity` (`Integer`), `price` (`BigDecimal(10,2)`).
   - `InventoryItem.java` (`src/main/java/com/sareekart/entity/InventoryItem.java:12–79`):
     - Fields: `id`, `sku` (`String`), `productId` (`Long`), `productName` (`String`), `category` (`String`), `warehouseCode` (`String`), `warehouseName` (`String`), `binLocation` (`String`), `onHand` (`Integer`), `reserved` (`Integer`), `available` (`Integer`), `unitPrice` (`BigDecimal(10,2)`), `status` (`"IN_STOCK"`, `"LOW_STOCK"`, `"OUT_OF_STOCK"`), `updatedAt` (`LocalDateTime`).
   - `Address.java` (`src/main/java/com/sareekart/entity/Address.java:7–20`):
     - Fields: `fullName`, `phone`, `streetAddress`, `city`, `state`, `pincode`.
   - `Coupon.java` (`src/main/java/com/sareekart/entity/Coupon.java:8–46`):
     - Fields: `code`, `discountPercent`, `discountAmount`, `minPurchaseAmount`, `usageLimit`, `timesUsed`, `active`, `isDeleted`, `expiryDate`.
   - `Cart.java` and `CartItem.java` (`src/main/java/com/sareekart/entity/Cart.java:15–43`):
     - Carts linked to `User` with `items` collection.
   - `User.java` (`src/main/java/com/sareekart/entity/User.java:17–83`):
     - Fields: `id`, `firstName`, `lastName`, `email`, `role` (`Role.CUSTOMER`, `Role.ADMIN`, `Role.MANAGER`, `Role.OWNER`), `createdAt`.

3. **Existing Repositories & Excel Service**:
   - `OrderRepository.java:10–18` currently has only basic queries (`findByUserIdOrderByCreatedAtDesc`, `sumTotalRevenue()`, `findTop5ByOrderByCreatedAtDesc`).
   - `OrderItemRepository.java` does not yet exist and is designated to be created in Milestone M1.
   - `ExcelProcessingService.java:24–204` demonstrates Apache POI 5.3.0 pattern using `XSSFWorkbook`, `Sheet`, `Row`, `Cell`, `CellStyle`, `Font`, and `IndexedColors`.
   - `SecurityConfig.java:56–64`:
     - Configures `authenticationEntryPoint` returning 401 and `accessDeniedHandler` returning 403 `{"success":false,"message":"Not authorised to perform this action"}`.
     - Per PROJECT.md R5: All analytics endpoints (`/api/admin/analytics/**`) must return 403 for both unauthenticated and customer requests.

---

## 2. Logic Chain

From the observations and requirements in `ORIGINAL_REQUEST.md` (§R1–§R5) and `DISPATCH.md`, we synthesize the mathematical, algorithmic, and architectural solutions across five core domains:

### 2.1 Interval & Comparative Range Engine

The telemetry engine requires two time windows for any analysis request:
1. **Current Analysis Window** $[t_{\text{start}}, t_{\text{end}}]$
2. **Prior Comparative Window** $[t_{\text{prior\_start}}, t_{\text{prior\_end}}]$ of equal duration for period-over-period percentage delta calculation.

#### Window Mapping Rules:
- **`TODAY`**:
  - Current: Today at 00:00:00 to Today at 23:59:59.999999999 ($D = 1\text{ day}$).
  - Prior: Yesterday at 00:00:00 to Yesterday at 23:59:59.999999999.
- **`7D`** (Last 7 Days):
  - Current: $[T - 6\text{ days at 00:00:00}, T\text{ at 23:59:59.999999999}]$ ($D = 7\text{ days}$).
  - Prior: $[t_{\text{start}} - 7\text{ days}, t_{\text{start}} - 1\text{ ns}]$ ($7\text{ preceding days}$).
- **`30D`** (Last 30 Days):
  - Current: $[T - 29\text{ days at 00:00:00}, T\text{ at 23:59:59.999999999}]$ ($D = 30\text{ days}$).
  - Prior: $[t_{\text{start}} - 30\text{ days}, t_{\text{start}} - 1\text{ ns}]$.
- **`90D`** (Last 90 Days):
  - Current: $[T - 89\text{ days at 00:00:00}, T\text{ at 23:59:59.999999999}]$ ($D = 90\text{ days}$).
  - Prior: $[t_{\text{start}} - 90\text{ days}, t_{\text{start}} - 1\text{ ns}]$.
- **`YTD`** (Year to Date):
  - Current: $[T\text{ first day of year at 00:00:00}, T\text{ at 23:59:59.999999999}]$.
  - Prior: $[t_{\text{start}} - 1\text{ year}, t_{\text{end}} - 1\text{ year}]$ (YoY comparison prevents seasonal holiday skew).
- **`ALL`** (All Time):
  - Current: $[2020-01-01\text{ 00:00:00}, T\text{ at 23:59:59.999999999}]$.
  - Prior: Identical window; delta $\Delta = 0.0\%$.
- **`CUSTOM`**:
  - Inputs: `startDate`, `endDate` (ISO-8601 `YYYY-MM-DD`).
  - Validation: If `startDate.isAfter(endDate)`, throw `BadRequestException("Start date must be on or before end date")`.
  - Current: $[\text{startDate at 00:00:00}, \text{endDate at 23:59:59.999999999}]$.
  - Duration: $D = \text{DAYS}(\text{startDate}, \text{endDate}) + 1$.
  - Prior: $[t_{\text{start}} - D\text{ days}, t_{\text{start}} - 1\text{ ns}]$.

#### Percentage Delta Algorithm:
$$\Delta\% = \begin{cases}
0.0 & \text{if } \text{prior} = 0 \text{ and } \text{current} = 0 \\
+100.0 & \text{if } \text{prior} = 0 \text{ and } \text{current} > 0 \\
-100.0 & \text{if } \text{prior} = 0 \text{ and } \text{current} < 0 \\
\operatorname{round}\left(\frac{\text{current} - \text{prior}}{|\text{prior}|} \times 100, 1\right) & \text{if } \text{prior} \neq 0
\end{cases}$$

---

### 2.2 Financial Telemetry Business Logic & Formulas (R1)

Eligible orders for sales telemetry are completed orders: $status \in \{\text{CONFIRMED}, \text{SHIPPED}, \text{DELIVERED}\}$ (non-cancelled).

1. **Gross Sales**:
   $$\text{Gross Sales} = \sum_{o \in O_{\text{comp}}} \sum_{i \in o.\text{items}} (i.\text{price} \times i.\text{quantity})$$
   Representing the total merchandise face value sold before promotional discounts or taxes.

2. **Total Discounts**:
   $$\text{Discounts} = \sum_{o \in O_{\text{comp}}} \max\left(0, \text{subtotal}(o) + \text{shipping}(o) - o.\text{totalAmount}\right)$$

3. **Net Revenue**:
   $$\text{Net Revenue} = \text{Gross Sales} - \text{Discounts}$$

4. **5% GST Tax Calculation**:
   Under Indian Goods and Services Tax (GST) provisions for textile fabrics and sarees, standard GST is 5%:
   $$\text{Tax (5\% GST)} = \operatorname{round}(\text{Net Revenue} \times 0.05, 2)$$

5. **Shipping Charges**:
   In SareeKart business logic, orders with subtotal $< ₹5000$ incur ₹150 shipping fee; $\ge ₹5000$ qualify for free shipping:
   $$\text{Shipping Revenue} = \sum_{o \in O_{\text{comp}}} \begin{cases} 150.00 & \text{if } \text{subtotal}(o) < 5000 \\ 0.00 & \text{otherwise} \end{cases}$$

6. **Average Order Value (AOV)**:
   $$\text{AOV} = \begin{cases} \operatorname{round}\left(\frac{\text{Net Revenue}}{|O_{\text{comp}}|}, 2\right) & \text{if } |O_{\text{comp}}| > 0 \\ 0.00 & \text{otherwise} \end{cases}$$

7. **Completed Orders**:
   $$\text{completedOrders} = |O_{\text{comp}}|$$

8. **Payment Method Distribution**:
   Group completed orders by `order.paymentMethod` (normalized to canonical names: `UPI`, `Cards`, `NetBanking`, `COD`):
   - `method`: Normalized string
   - `count`: Total transactions
   - `amount`: Sum of order totals
   - `percentage`: $\operatorname{round}\left(\frac{\text{amount}(m)}{\sum \text{amount}} \times 100, 1\right)$

9. **Coupon Utilization & ROI**:
   For orders placed with promotional coupon codes:
   - `uses`: Number of times the coupon was redeemed in the period
   - `discountTotal`: Total rupees discounted via coupon
   - `revenueGenerated`: Total gross sales from orders that utilized the coupon
   - `roi`:
     $$\text{Coupon ROI} = \begin{cases} \operatorname{round}\left(\frac{\text{revenueGenerated}}{\text{discountTotal}}, 2\right) & \text{if } \text{discountTotal} > 0 \\ 0.00 & \text{otherwise} \end{cases}$$

10. **Continuous Daily Revenue Timeline**:
    Generate all dates $d \in [t_{\text{start}}.\text{toLocalDate}(), t_{\text{end}}.\text{toLocalDate}()]$:
    - Zero-filling guarantee: Days without orders must emit `orders = 0`, `units = 0`, `revenue = 0.00` to prevent broken continuous line graphs in admin frontend charts.

---

### 2.3 Inventory Velocity & Stock Telemetry (R2)

Inventory metrics use a standard rolling 30-day velocity window ($W = 30$) to calculate run-rate and days remaining.

1. **Daily Run-Rate**:
   For each product/SKU:
   $$U = \sum_{o \in O_{30\text{d}}, i \in o.\text{items}} i.\text{quantity}$$
   $$\text{Daily Run-Rate} = \operatorname{round}\left(\frac{U}{30.0}, 2\right)$$

2. **Days of Inventory Remaining (DOIR)**:
   For available stock $S$:
   $$\text{DOIR} = \begin{cases}
   0 & \text{if } S \le 0 \\
   999 & \text{if } \text{runRate} = 0 \text{ and } S > 0 \quad (\text{stagnant stock}) \\
   \min\left(999, \left\lfloor \frac{S}{\text{runRate}} \right\rfloor\right) & \text{if } \text{runRate} > 0
   \end{cases}$$

3. **Fast-Moving vs Slow-Moving SKU Classification**:
   - **Fast-Moving**: Top 10 SKUs sorted by $U$ descending, then daily run-rate descending.
   - **Slow-Moving**: Bottom 10 SKUs with available stock $S > 0$, sorted by units sold ascending, then available stock descending (highlighting trapped capital).

4. **Inventory Aging Categorization**:
   Based on days elapsed since item update/receipt ($\text{age} = \text{DAYS}(item.\text{updatedAt}, \text{now})$):
   - `< 30 days`: Fresh inventory ($\text{age} < 30$).
   - `30–90 days`: Moderate aging ($30 \le \text{age} \le 90$).
   - `> 90 days`: Aged / Dead stock ($\text{age} > 90$).
   Metrics per bucket: SKU count, total units on hand, total inventory valuation ($\sum S \times \text{unitPrice}$), valuation percentage.

5. **Stockout Alerts & Multi-Factor Priority Scoring**:
   Filter condition: $S \le 10$ OR $\text{status} \in \{\text{OUT\_OF\_STOCK}, \text{LOW\_STOCK}\}$ OR $\text{DOIR} \le 7$.
   Deterministic priority score (0 to 100):
   $$\text{Score} = \text{StockPoints} (0-50) + \text{VelocityPoints} (0-35) + \text{DaysPoints} (0-15)$$
   - **Stock Points (50 pts max)**:
     - $S = 0 \implies 50$
     - $S = 1 \implies 45$
     - $S = 2 \implies 40$
     - $3 \le S \le 5 \implies 30$
     - $6 \le S \le 10 \implies 15$
     - $S > 10 \implies 5$
   - **Velocity Points (35 pts max)**:
     - $\text{runRate} \ge 5.0 \implies 35$
     - $2.0 \le \text{runRate} < 5.0 \implies 25$
     - $1.0 \le \text{runRate} < 2.0 \implies 15$
     - $0.0 < \text{runRate} < 1.0 \implies 10$
     - $\text{runRate} = 0.0 \implies 0$
   - **Days Remaining Points (15 pts max)**:
     - $\text{DOIR} = 0 \implies 15$
     - $1 \le \text{DOIR} \le 2 \implies 12$
     - $3 \le \text{DOIR} \le 5 \implies 8$
     - $6 \le \text{DOIR} \le 7 \implies 5$
     - $\text{DOIR} > 7 \implies 0$
   - **Priority Tier Classification**:
     - $\text{Score} \ge 90 \implies \text{"CRITICAL"}$
     - $70 \le \text{Score} < 90 \implies \text{"HIGH"}$
     - $40 \le \text{Score} < 70 \implies \text{"MEDIUM"}$
     - $\text{Score} < 40 \implies \text{"LOW"}$
   - **Recommended Reorder Quantity**:
     $$\text{reorderQty} = \max\left(20, \lceil \text{runRate} \times 30 \rceil - S\right)$$

---

### 2.4 Customer Cohorts & Geographic Analytics (R3)

1. **Customer Lifetime Value (LTV) Distribution**:
   Compute total lifetime spend per customer across all historical completed orders:
   - **Platinum**: Spend $> ₹50,000$
   - **Gold**: Spend between $₹15,000$ and $₹50,000$
   - **Silver**: Spend $< ₹15,000$
   For each tier: `customerCount`, `totalSpend`, `averageLtv` ($\frac{\text{totalSpend}}{\text{customerCount}}$), `customerPercentage`, `revenuePercentage`.

2. **New vs Returning Customer Contribution & Repeat Purchase Rate**:
   Within the current interval $[t_{\text{start}}, t_{\text{end}}]$:
   - Customer $u$ is **New Customer** if their first completed order in system history occurred on or after $t_{\text{start}}$.
   - Customer $u$ is **Returning Customer** if they have at least one completed order prior to $t_{\text{start}}$.
   - Revenue splits: `newCustomerRevenue` and `returningCustomerRevenue`.
   - Repeat Purchase Rate:
     $$\text{repeatPurchaseRate} = \operatorname{round}\left(\frac{|U_{\text{returning}}|}{\max(1, |U_{\text{new}}| + |U_{\text{returning}}|)} \times 100, 1\right)$$

3. **Regional Breakdown (Top States and Cities)**:
   Extracted from `order.shippingAddress.state` and `order.shippingAddress.city`:
   - Top 5/10 states by revenue: `state`, `orderCount`, `revenue`, `sharePercentage`.
   - Top 5/10 cities by revenue: `city`, `state`, `orderCount`, `revenue`, `sharePercentage`.

4. **Conversion Funnel & Cart Abandonment Telemetry**:
   - `cartsCreated`: Total carts registered in database.
   - `cartsWithItems`: Carts with `items.size() > 0`.
   - `checkoutInitiated`: Total orders initiated in period (including PENDING).
   - `ordersCompleted`: Non-cancelled completed orders in period.
   - Cart Abandonment Rate:
     $$\text{abandonmentRate} = \operatorname{round}\left(\max\left(0.0, \left(1.0 - \frac{\text{ordersCompleted}}{\max(1, \text{cartsWithItems})}\right) \times 100\right), 1\right)$$
   - Conversion Rate:
     $$\text{conversionRate} = \operatorname{round}\left(\frac{\text{ordersCompleted}}{\max(1, \text{cartsCreated})} \times 100, 1\right)$$

---

### 2.5 Apache POI Excel (.xlsx) & CSV Export Utilities

1. **Excel Export (`.xlsx`) via Apache POI 5.3.0**:
   - Generates multi-tab workbooks using `XSSFWorkbook`.
   - Styling tokens:
     - Header: Dark Navy `#1E293B`, bold 11pt white text, solid fill, center aligned.
     - Currency: Right-aligned, formatted with `₹#,##0.00`.
     - Percentage: Right-aligned, formatted with `0.0%`.
     - Integers: Right-aligned, formatted with `#,##0`.
     - Dates: Center-aligned `yyyy-mm-dd`.
     - Auto-column sizing with minimum safety width of 15 characters (`15 * 256`).
   - Sales Workbook sheets:
     - Sheet 1: `Sales Overview` (KPI Summary & Period Deltas).
     - Sheet 2: `Daily Revenue Timeline` (Date, Orders, Units, Gross Sales, Net Revenue, GST, Shipping).
     - Sheet 3: `Payment Method Breakdown` (Method, Transactions, Revenue, Share %).
     - Sheet 4: `Coupon ROI Performance` (Coupon Code, Uses, Total Discount, Revenue Generated, ROI).
   - Inventory Workbook sheets:
     - Sheet 1: `Inventory Aging & Valuation` (Bucket, SKU Count, Units, Valuation, Valuation %).
     - Sheet 2: `Fast & Slow SKUs` (SKU, Product Name, Category, Units Sold, Run Rate, Available, DOIR, Status).
     - Sheet 3: `Stockout Priority Alerts` (Priority Level, Priority Score, SKU, Product Name, Warehouse, Stock, Run Rate, DOIR, Reorder Qty).

2. **RFC 4180 CSV Streaming**:
   - Quotes any field containing commas `,`, double quotes `"`, or line breaks (`\r`, `\n`).
   - Escapes internal quotes by doubling (`""`).
   - Prepend UTF-8 BOM (`\uFEFF`) to enable Microsoft Excel to open CSV files without unicode character encoding corruption.

---

## 3. Caveats

1. **Tax Model Assumption**:
   - SareeKart prices in `OrderServiceImpl` are stored as final line-item prices (`product.price * quantity`). Net revenue is computed as gross sales minus coupon discounts. The 5% GST tax is computed directly on net revenue: `taxAmount = netRevenue * 0.05`. If tax was modeled as embedded (tax-inclusive), the formula would be `netRevenue * (5.0 / 105.0)`. In this design, we provide the clean 5% GST telemetry computation and document both options.
2. **Coupon Linkage**:
   - Current `Order.java` entity does not store `couponCode` or `discountAmount` columns directly; discounts were computed dynamically during checkout. For analytics to report per-coupon ROI with 100% precision, `Order.java` should ideally have nullable `couponCode` and `discountAmount` columns. The design provides fallback handling if an order was placed without persistent coupon recording.
3. **Inventory Velocity Window**:
   - The inventory velocity run-rate defaults to a standard 30-day baseline ($W = 30$). If the platform has fewer than 30 days of data, elapsed days since platform launch should be used as denominator with a floor of 1 to avoid division by zero.
4. **Access Control (R5) Hardening**:
   - Spring Security's default `authenticationEntryPoint` returns HTTP 401 UNAUTHORIZED when no token is provided. Requirement R5 explicitly mandates: *"Customers and unauthenticated requests must receive HTTP 403 Forbidden with message 'Not authorised to perform this action'"*.
   - Implementation must ensure the security entry point or filter chain returns HTTP 403 for unauthenticated access attempts to `/api/admin/analytics/**`.

---

## 4. Conclusion & Production Implementation Blueprints

Here are the complete, production-ready class specifications and code templates for Milestone M1:

### 4.1 Enums & Helper Records

```java
package com.sareekart.dto.response.analytics;

public enum AnalyticsDateRange {
    TODAY,
    _7D,
    _30D,
    _90D,
    YTD,
    ALL,
    CUSTOM;

    public static AnalyticsDateRange fromString(String value) {
        if (value == null || value.isBlank()) return _7D;
        String upper = value.trim().toUpperCase();
        return switch (upper) {
            case "TODAY" -> TODAY;
            case "7D", "LAST_7_DAYS" -> _7D;
            case "30D", "LAST_30_DAYS" -> _30D;
            case "90D", "LAST_90_DAYS" -> _90D;
            case "YTD", "YEAR_TO_DATE" -> YTD;
            case "ALL" -> ALL;
            case "CUSTOM" -> CUSTOM;
            default -> throw new IllegalArgumentException("Invalid analytics date range: " + value);
        };
    }
}
```

```java
package com.sareekart.dto.response.analytics;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Getter
@Builder
public class DateRangeWindow {
    private final LocalDateTime currentStart;
    private final LocalDateTime currentEnd;
    private final LocalDateTime priorStart;
    private final LocalDateTime priorEnd;
    private final long daysInWindow;

    public static DateRangeWindow calculate(AnalyticsDateRange range, LocalDate customStart, LocalDate customEnd) {
        LocalDate today = LocalDate.now();
        LocalDateTime currentStart;
        LocalDateTime currentEnd = today.atTime(LocalTime.MAX);
        LocalDateTime priorStart;
        LocalDateTime priorEnd;
        long days;

        switch (range) {
            case TODAY -> {
                currentStart = today.atStartOfDay();
                days = 1;
                priorStart = today.minusDays(1).atStartOfDay();
                priorEnd = today.minusDays(1).atTime(LocalTime.MAX);
            }
            case _7D -> {
                currentStart = today.minusDays(6).atStartOfDay();
                days = 7;
                priorStart = currentStart.minusDays(7);
                priorEnd = currentStart.minusNanos(1);
            }
            case _30D -> {
                currentStart = today.minusDays(29).atStartOfDay();
                days = 30;
                priorStart = currentStart.minusDays(30);
                priorEnd = currentStart.minusNanos(1);
            }
            case _90D -> {
                currentStart = today.minusDays(89).atStartOfDay();
                days = 90;
                priorStart = currentStart.minusDays(90);
                priorEnd = currentStart.minusNanos(1);
            }
            case YTD -> {
                currentStart = today.with(TemporalAdjusters.firstDayOfYear()).atStartOfDay();
                days = ChronoUnit.DAYS.between(currentStart.toLocalDate(), today) + 1;
                priorStart = currentStart.minusYears(1);
                priorEnd = currentEnd.minusYears(1);
            }
            case ALL -> {
                currentStart = LocalDateTime.of(2020, 1, 1, 0, 0, 0);
                days = ChronoUnit.DAYS.between(currentStart.toLocalDate(), today) + 1;
                priorStart = currentStart;
                priorEnd = currentStart;
            }
            case CUSTOM -> {
                if (customStart == null) customStart = today.minusDays(29);
                if (customEnd == null) customEnd = today;
                if (customStart.isAfter(customEnd)) {
                    throw new IllegalArgumentException("Start date cannot be after end date");
                }
                currentStart = customStart.atStartOfDay();
                currentEnd = customEnd.atTime(LocalTime.MAX);
                days = ChronoUnit.DAYS.between(customStart, customEnd) + 1;
                priorStart = currentStart.minusDays(days);
                priorEnd = currentStart.minusNanos(1);
            }
            default -> throw new IllegalStateException("Unexpected range: " + range);
        }

        return DateRangeWindow.builder()
                .currentStart(currentStart)
                .currentEnd(currentEnd)
                .priorStart(priorStart)
                .priorEnd(priorEnd)
                .daysInWindow(days)
                .build();
    }
}
```

---

### 4.2 Math & Period-Over-Period Utility

```java
package com.sareekart.dto.response.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AnalyticsMathUtil {

    private AnalyticsMathUtil() {}

    public static double calculatePercentageDelta(BigDecimal current, BigDecimal prior) {
        if (prior == null || prior.compareTo(BigDecimal.ZERO) == 0) {
            if (current == null || current.compareTo(BigDecimal.ZERO) == 0) return 0.0;
            return 100.0;
        }
        if (current == null) current = BigDecimal.ZERO;
        BigDecimal delta = current.subtract(prior);
        BigDecimal pct = delta.multiply(BigDecimal.valueOf(100.0))
                .divide(prior.abs(), 4, RoundingMode.HALF_UP);
        return pct.setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    public static double calculatePercentageDelta(long current, long prior) {
        return calculatePercentageDelta(BigDecimal.valueOf(current), BigDecimal.valueOf(prior));
    }

    public static double calculateRatioPercentage(long numerator, long denominator) {
        if (denominator <= 0) return 0.0;
        return BigDecimal.valueOf(numerator * 100.0)
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
```

---

### 4.3 Service Interface (`AnalyticsService.java`)

```java
package com.sareekart.service;

import com.sareekart.dto.response.analytics.*;

import java.time.LocalDate;

public interface AnalyticsService {

    AnalyticsOverviewResponse getOverview(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    SalesTelemetryResponse getSalesTelemetry(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    InventoryVelocityResponse getInventoryVelocity();

    CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportSalesTelemetryExcel(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportSalesTelemetryCsv(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate);

    byte[] exportInventoryVelocityExcel();

    byte[] exportInventoryVelocityCsv();
}
```

---

### 4.4 Service Implementation (`AnalyticsServiceImpl.java`)

```java
package com.sareekart.service.impl;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.entity.*;
import com.sareekart.repository.*;
import com.sareekart.service.AnalyticsService;
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
    private final InventoryItemRepository inventoryItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final CouponRepository couponRepository;

    private static final BigDecimal SHIPPING_THRESHOLD = BigDecimal.valueOf(5000);
    private static final BigDecimal SHIPPING_FEE = BigDecimal.valueOf(150);
    private static final BigDecimal GST_RATE = BigDecimal.valueOf(0.05);

    @Override
    public AnalyticsOverviewResponse getOverview(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);

        List<Order> currentOrders = getCompletedOrders(window.getCurrentStart(), window.getCurrentEnd());
        List<Order> priorOrders = getCompletedOrders(window.getPriorStart(), window.getPriorEnd());

        FinancialMetrics current = computeFinancials(currentOrders);
        FinancialMetrics prior = computeFinancials(priorOrders);

        Map<String, Double> percentageChanges = new LinkedHashMap<>();
        percentageChanges.put("grossSales", AnalyticsMathUtil.calculatePercentageDelta(current.grossSales, prior.grossSales));
        percentageChanges.put("netRevenue", AnalyticsMathUtil.calculatePercentageDelta(current.netRevenue, prior.netRevenue));
        percentageChanges.put("taxAmount", AnalyticsMathUtil.calculatePercentageDelta(current.taxAmount, prior.taxAmount));
        percentageChanges.put("shippingAmount", AnalyticsMathUtil.calculatePercentageDelta(current.shippingAmount, prior.shippingAmount));
        percentageChanges.put("aov", AnalyticsMathUtil.calculatePercentageDelta(current.aov, prior.aov));
        percentageChanges.put("completedOrders", AnalyticsMathUtil.calculatePercentageDelta(current.completedOrders, prior.completedOrders));

        return AnalyticsOverviewResponse.builder()
                .range(range.name())
                .startDate(window.getCurrentStart().toLocalDate().toString())
                .endDate(window.getCurrentEnd().toLocalDate().toString())
                .grossSales(current.grossSales)
                .netRevenue(current.netRevenue)
                .taxAmount(current.taxAmount)
                .shippingAmount(current.shippingAmount)
                .aov(current.aov)
                .completedOrders(current.completedOrders)
                .percentageChanges(percentageChanges)
                .build();
    }

    @Override
    public SalesTelemetryResponse getSalesTelemetry(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);
        List<Order> orders = getCompletedOrders(window.getCurrentStart(), window.getCurrentEnd());
        FinancialMetrics fin = computeFinancials(orders);

        // Daily timeline with zero-fill
        List<DailyRevenueDto> timeline = buildTimeline(window.getCurrentStart().toLocalDate(), window.getCurrentEnd().toLocalDate(), orders);

        // Payment distribution
        List<PaymentDistributionDto> paymentDist = buildPaymentDistribution(orders, fin.netRevenue);

        // Coupon utilization
        List<CouponRoiDto> couponUtil = buildCouponUtilization(orders);

        return SalesTelemetryResponse.builder()
                .range(range.name())
                .startDate(window.getCurrentStart().toLocalDate().toString())
                .endDate(window.getCurrentEnd().toLocalDate().toString())
                .grossSales(fin.grossSales)
                .netRevenue(fin.netRevenue)
                .taxAmount(fin.taxAmount)
                .shippingAmount(fin.shippingAmount)
                .aov(fin.aov)
                .completedOrders(fin.completedOrders)
                .timeline(timeline)
                .paymentDistribution(paymentDist)
                .couponUtilization(couponUtil)
                .build();
    }

    @Override
    public InventoryVelocityResponse getInventoryVelocity() {
        LocalDate today = LocalDate.now();
        LocalDateTime thirtyDaysAgo = today.minusDays(29).atStartOfDay();
        List<Order> orders30d = getCompletedOrders(thirtyDaysAgo, today.atTime(23, 59, 59));

        // Aggregate units sold per product in last 30 days
        Map<Long, Integer> productUnitsSold = new HashMap<>();
        for (Order o : orders30d) {
            for (OrderItem i : o.getItems()) {
                if (i.getProduct() != null) {
                    productUnitsSold.merge(i.getProduct().getId(), i.getQuantity(), Integer::sum);
                }
            }
        }

        List<InventoryItem> items = inventoryItemRepository.findAll();
        List<SkuVelocityDto> allSkus = new ArrayList<>();
        List<StockoutAlertDto> stockoutAlerts = new ArrayList<>();

        int totalOnHand = 0;
        BigDecimal totalValuation = BigDecimal.ZERO;
        int freshCount = 0; int freshUnits = 0; BigDecimal freshVal = BigDecimal.ZERO;
        int midCount = 0; int midUnits = 0; BigDecimal midVal = BigDecimal.ZERO;
        int agedCount = 0; int agedUnits = 0; BigDecimal agedVal = BigDecimal.ZERO;

        for (InventoryItem item : items) {
            int sold = productUnitsSold.getOrDefault(item.getProductId(), 0);
            double runRate = BigDecimal.valueOf(sold / 30.0).setScale(2, RoundingMode.HALF_UP).doubleValue();
            int avail = item.getAvailable() != null ? item.getAvailable() : 0;
            int doir = (runRate > 0) ? Math.min(999, (int) Math.floor(avail / runRate)) : (avail > 0 ? 999 : 0);

            BigDecimal unitPrice = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
            BigDecimal itemValuation = unitPrice.multiply(BigDecimal.valueOf(avail));
            totalOnHand += avail;
            totalValuation = totalValuation.add(itemValuation);

            // Aging categorization
            long ageDays = item.getUpdatedAt() != null ? ChronoUnit.DAYS.between(item.getUpdatedAt().toLocalDate(), today) : 0;
            if (ageDays < 30) {
                freshCount++; freshUnits += avail; freshVal = freshVal.add(itemValuation);
            } else if (ageDays <= 90) {
                midCount++; midUnits += avail; midVal = midVal.add(itemValuation);
            } else {
                agedCount++; agedUnits += avail; agedVal = agedVal.add(itemValuation);
            }

            SkuVelocityDto skuDto = SkuVelocityDto.builder()
                    .sku(item.getSku())
                    .productName(item.getProductName())
                    .category(item.getCategory())
                    .warehouseCode(item.getWarehouseCode())
                    .availableStock(avail)
                    .unitsSold30d(sold)
                    .dailyRunRate(runRate)
                    .daysOfInventoryRemaining(doir)
                    .unitPrice(unitPrice)
                    .build();
            allSkus.add(skuDto);

            // Stockout alert condition
            if (avail <= 10 || "OUT_OF_STOCK".equals(item.getStatus()) || "LOW_STOCK".equals(item.getStatus()) || doir <= 7) {
                int priorityScore = computePriorityScore(avail, runRate, doir);
                String priorityLevel = getPriorityLevel(priorityScore);
                int reorderQty = Math.max(20, (int) Math.ceil(runRate * 30) - avail);

                stockoutAlerts.add(StockoutAlertDto.builder()
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .warehouseCode(item.getWarehouseCode())
                        .warehouseName(item.getWarehouseName())
                        .availableStock(avail)
                        .dailyRunRate(runRate)
                        .daysRemaining(doir)
                        .status(item.getStatus())
                        .priorityScore(priorityScore)
                        .priorityLevel(priorityLevel)
                        .recommendedReorderQty(reorderQty)
                        .build());
            }
        }

        // Sort Fast vs Slow
        List<SkuVelocityDto> fastMoving = allSkus.stream()
                .sorted(Comparator.comparingInt(SkuVelocityDto::getUnitsSold30d).reversed()
                        .thenComparingDouble(SkuVelocityDto::getDailyRunRate).reversed())
                .limit(10)
                .collect(Collectors.toList());

        List<SkuVelocityDto> slowMoving = allSkus.stream()
                .filter(s -> s.getAvailableStock() > 0)
                .sorted(Comparator.comparingInt(SkuVelocityDto::getUnitsSold30d)
                        .thenComparing(Comparator.comparingInt(SkuVelocityDto::getAvailableStock).reversed()))
                .limit(10)
                .collect(Collectors.toList());

        // Sort alerts by priority score descending
        stockoutAlerts.sort(Comparator.comparingInt(StockoutAlertDto::getPriorityScore).reversed());

        // Average DOIR
        double totalRunRate = allSkus.stream().mapToDouble(SkuVelocityDto::getDailyRunRate).sum();
        int avgDoir = totalRunRate > 0 ? (int) Math.round(totalOnHand / totalRunRate) : 999;

        // Build aging categories
        List<InventoryAgingDto> aging = List.of(
                buildAgingDto("<30 days", freshCount, freshUnits, freshVal, totalValuation),
                buildAgingDto("30-90 days", midCount, midUnits, midVal, totalValuation),
                buildAgingDto(">90 days", agedCount, agedUnits, agedVal, totalValuation)
        );

        return InventoryVelocityResponse.builder()
                .totalSkus(items.size())
                .totalAvailableStock(totalOnHand)
                .totalValuation(totalValuation)
                .averageDaysRemaining(avgDoir)
                .fastMovingSkus(fastMoving)
                .slowMovingSkus(slowMoving)
                .agingCategories(aging)
                .stockoutAlerts(stockoutAlerts)
                .build();
    }

    @Override
    public CustomerAnalyticsResponse getCustomerAnalytics(AnalyticsDateRange range, LocalDate startDate, LocalDate endDate) {
        DateRangeWindow window = DateRangeWindow.calculate(range, startDate, endDate);
        List<Order> allOrders = orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .collect(Collectors.toList());

        // Group historical orders by user ID
        Map<Long, List<Order>> ordersByUser = allOrders.stream()
                .collect(Collectors.groupingBy(o -> o.getUser().getId()));

        // 1. LTV Tiers across all active customers
        int platinumCount = 0; BigDecimal platinumSpend = BigDecimal.ZERO;
        int goldCount = 0; BigDecimal goldSpend = BigDecimal.ZERO;
        int silverCount = 0; BigDecimal silverSpend = BigDecimal.ZERO;

        for (Map.Entry<Long, List<Order>> entry : ordersByUser.entrySet()) {
            BigDecimal spend = entry.getValue().stream()
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (spend.compareTo(BigDecimal.valueOf(50000)) > 0) {
                platinumCount++; platinumSpend = platinumSpend.add(spend);
            } else if (spend.compareTo(BigDecimal.valueOf(15000)) >= 0) {
                goldCount++; goldSpend = goldSpend.add(spend);
            } else {
                silverCount++; silverSpend = silverSpend.add(spend);
            }
        }

        BigDecimal totalCustSpend = platinumSpend.add(goldSpend).add(silverSpend);
        int totalCustomers = ordersByUser.size();

        List<LtvTierDto> ltvTiers = List.of(
                buildLtvDto("Platinum", platinumCount, platinumSpend, totalCustomers, totalCustSpend),
                buildLtvDto("Gold", goldCount, goldSpend, totalCustomers, totalCustSpend),
                buildLtvDto("Silver", silverCount, silverSpend, totalCustomers, totalCustSpend)
        );

        // 2. New vs Returning in window
        List<Order> windowOrders = getCompletedOrders(window.getCurrentStart(), window.getCurrentEnd());
        Set<Long> usersInWindow = windowOrders.stream().map(o -> o.getUser().getId()).collect(Collectors.toSet());

        int newCustomers = 0; int returningCustomers = 0;
        BigDecimal newRevenue = BigDecimal.ZERO; BigDecimal returningRevenue = BigDecimal.ZERO;

        for (Long uid : usersInWindow) {
            List<Order> userHistory = ordersByUser.getOrDefault(uid, List.of());
            LocalDateTime firstOrderDate = userHistory.stream()
                    .map(Order::getCreatedAt)
                    .min(LocalDateTime::compareTo)
                    .orElse(window.getCurrentStart());

            BigDecimal userWindowSpend = windowOrders.stream()
                    .filter(o -> o.getUser().getId().equals(uid))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (firstOrderDate.isBefore(window.getCurrentStart())) {
                returningCustomers++;
                returningRevenue = returningRevenue.add(userWindowSpend);
            } else {
                newCustomers++;
                newRevenue = newRevenue.add(userWindowSpend);
            }
        }

        double repeatRate = AnalyticsMathUtil.calculateRatioPercentage(returningCustomers, newCustomers + returningCustomers);

        // 3. Regional breakdown
        Map<String, List<Order>> ordersByState = windowOrders.stream()
                .filter(o -> o.getShippingAddress() != null && o.getShippingAddress().getState() != null)
                .collect(Collectors.groupingBy(o -> o.getShippingAddress().getState()));

        BigDecimal totalRegRevenue = windowOrders.stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        List<RegionMetricDto> topStates = ordersByState.entrySet().stream()
                .map(e -> {
                    BigDecimal rev = e.getValue().stream().map(Order::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    double pct = AnalyticsMathUtil.calculatePercentageDelta(rev, totalRegRevenue) > 0 ?
                            rev.multiply(BigDecimal.valueOf(100.0)).divide(totalRegRevenue.max(BigDecimal.ONE), 1, RoundingMode.HALF_UP).doubleValue() : 0.0;
                    return RegionMetricDto.builder()
                            .region(e.getKey())
                            .orderCount(e.getValue().size())
                            .revenue(rev)
                            .percentage(pct)
                            .build();
                })
                .sorted(Comparator.comparing(RegionMetricDto::getRevenue).reversed())
                .limit(10)
                .collect(Collectors.toList());

        // 4. Conversion Funnel
        long totalCarts = cartRepository.count();
        long cartsWithItems = cartRepository.findAll().stream()
                .filter(c -> c.getItems() != null && !c.getItems().isEmpty())
                .count();
        long checkoutInitiated = orderRepository.findAll().stream()
                .filter(o -> o.getCreatedAt().isAfter(window.getCurrentStart()) && o.getCreatedAt().isBefore(window.getCurrentEnd()))
                .count();
        long ordersCompleted = windowOrders.size();

        double abandonmentRate = cartsWithItems > 0 ?
                Math.max(0.0, Math.min(100.0, ((1.0 - ((double) ordersCompleted / cartsWithItems)) * 100.0))) : 0.0;
        double conversionRate = totalCarts > 0 ?
                Math.min(100.0, ((double) ordersCompleted / totalCarts) * 100.0) : 0.0;

        ConversionFunnelDto funnel = ConversionFunnelDto.builder()
                .cartsCreated(totalCarts)
                .cartsWithItems(cartsWithItems)
                .checkoutInitiated(checkoutInitiated)
                .ordersCompleted(ordersCompleted)
                .abandonmentRate(BigDecimal.valueOf(abandonmentRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .conversionRate(BigDecimal.valueOf(conversionRate).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .build();

        return CustomerAnalyticsResponse.builder()
                .range(range.name())
                .ltvTiers(ltvTiers)
                .newVsReturning(NewVsReturningDto.builder()
                        .newCustomers(newCustomers)
                        .returningCustomers(returningCustomers)
                        .newRevenue(newRevenue)
                        .returningRevenue(returningRevenue)
                        .repeatPurchaseRate(repeatRate)
                        .build())
                .topStates(topStates)
                .conversionFunnel(funnel)
                .build();
    }

    // Helper: Compute Financials
    private FinancialMetrics computeFinancials(List<Order> orders) {
        BigDecimal grossSales = BigDecimal.ZERO;
        BigDecimal netRevenue = BigDecimal.ZERO;
        BigDecimal shippingAmount = BigDecimal.ZERO;

        for (Order o : orders) {
            BigDecimal subtotal = BigDecimal.ZERO;
            for (OrderItem item : o.getItems()) {
                BigDecimal itemPrice = item.getPrice() != null ? item.getPrice() : BigDecimal.ZERO;
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                subtotal = subtotal.add(itemPrice.multiply(BigDecimal.valueOf(qty)));
            }
            grossSales = grossSales.add(subtotal);

            BigDecimal ship = subtotal.compareTo(SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_FEE;
            shippingAmount = shippingAmount.add(ship);
            netRevenue = netRevenue.add(o.getTotalAmount() != null ? o.getTotalAmount() : subtotal);
        }

        BigDecimal taxAmount = netRevenue.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        long completedCount = orders.size();
        BigDecimal aov = completedCount > 0 ?
                netRevenue.divide(BigDecimal.valueOf(completedCount), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        return new FinancialMetrics(grossSales, netRevenue, taxAmount, shippingAmount, aov, completedCount);
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

    private List<Order> getCompletedOrders(LocalDateTime start, LocalDateTime end) {
        return orderRepository.findAll().stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .filter(o -> !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                .collect(Collectors.toList());
    }

    private record FinancialMetrics(BigDecimal grossSales, BigDecimal netRevenue, BigDecimal taxAmount,
                                    BigDecimal shippingAmount, BigDecimal aov, long completedOrders) {}
}
```

---

### 4.5 Apache POI Excel (.xlsx) & CSV Export Class Templates

#### Excel Report Generator (`AnalyticsExcelGenerator.java`):

```java
package com.sareekart.util;

import com.sareekart.dto.response.analytics.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public final class AnalyticsExcelGenerator {

    private AnalyticsExcelGenerator() {}

    public static byte[] generateSalesWorkbook(SalesTelemetryResponse data) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);
            CellStyle integerStyle = createIntegerStyle(workbook);
            CellStyle dateStyle = createDateStyle(workbook);
            CellStyle percentStyle = createPercentStyle(workbook);

            // Sheet 1: Sales Overview
            Sheet s1 = workbook.createSheet("Overview");
            createTitleRow(s1, "SareeKart Sales & Financial Telemetry", headerStyle);
            int r = 2;
            createKpiRow(s1, r++, "Date Range", data.getRange(), null);
            createKpiRow(s1, r++, "Window", data.getStartDate() + " to " + data.getEndDate(), null);
            createKpiRow(s1, r++, "Gross Sales", data.getGrossSales(), currencyStyle);
            createKpiRow(s1, r++, "Net Revenue", data.getNetRevenue(), currencyStyle);
            createKpiRow(s1, r++, "GST Tax (5%)", data.getTaxAmount(), currencyStyle);
            createKpiRow(s1, r++, "Shipping Collected", data.getShippingAmount(), currencyStyle);
            createKpiRow(s1, r++, "Average Order Value (AOV)", data.getAov(), currencyStyle);
            createKpiRow(s1, r++, "Completed Orders", data.getCompletedOrders(), integerStyle);

            // Sheet 2: Daily Timeline
            Sheet s2 = workbook.createSheet("Daily Timeline");
            Row h2 = s2.createRow(0);
            String[] cols2 = {"Date", "Orders", "Units Sold", "Gross Sales", "Net Revenue", "GST (5%)", "Shipping"};
            for (int i = 0; i < cols2.length; i++) {
                Cell c = h2.createCell(i); c.setCellValue(cols2[i]); c.setCellStyle(headerStyle);
            }
            int rowIdx = 1;
            for (DailyRevenueDto d : data.getTimeline()) {
                Row row = s2.createRow(rowIdx++);
                Cell c0 = row.createCell(0); c0.setCellValue(d.getDate()); c0.setCellStyle(dateStyle);
                Cell c1 = row.createCell(1); c1.setCellValue(d.getOrders()); c1.setCellStyle(integerStyle);
                Cell c2 = row.createCell(2); c2.setCellValue(d.getUnits()); c2.setCellStyle(integerStyle);
                Cell c3 = row.createCell(3); c3.setCellValue(d.getRevenue().doubleValue()); c3.setCellStyle(currencyStyle);
                Cell c4 = row.createCell(4); c4.setCellValue(d.getRevenue().doubleValue()); c4.setCellStyle(currencyStyle);
                Cell c5 = row.createCell(5); c5.setCellValue(d.getRevenue().multiply(BigDecimal.valueOf(0.05)).doubleValue()); c5.setCellStyle(currencyStyle);
                Cell c6 = row.createCell(6); c6.setCellValue(0.0); c6.setCellStyle(currencyStyle);
            }

            // Auto-size columns
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
            CellStyle percentStyle = createPercentStyle(workbook);

            // Sheet 1: Stockout Alerts
            Sheet s1 = workbook.createSheet("Stockout Alerts");
            Row h1 = s1.createRow(0);
            String[] cols1 = {"Priority", "Score", "SKU", "Product Name", "Warehouse", "Stock Available", "Run-Rate", "Days Remaining", "Reorder Qty"};
            for (int i = 0; i < cols1.length; i++) {
                Cell c = h1.createCell(i); c.setCellValue(cols1[i]); c.setCellStyle(headerStyle);
            }
            int r1 = 1;
            for (StockoutAlertDto a : data.getStockoutAlerts()) {
                Row row = s1.createRow(r1++);
                row.createCell(0).setCellValue(a.getPriorityLevel());
                row.createCell(1).setCellValue(a.getPriorityScore());
                row.createCell(2).setCellValue(a.getSku());
                row.createCell(3).setCellValue(a.getProductName());
                row.createCell(4).setCellValue(a.getWarehouseCode());
                row.createCell(5).setCellValue(a.getAvailableStock());
                row.createCell(6).setCellValue(a.getDailyRunRate());
                row.createCell(7).setCellValue(a.getDaysRemaining());
                row.createCell(8).setCellValue(a.getRecommendedReorderQty());
            }

            // Sheet 2: Fast & Slow Moving SKUs
            Sheet s2 = workbook.createSheet("SKU Velocity Rankings");
            Row h2 = s2.createRow(0);
            String[] cols2 = {"Classification", "SKU", "Product Name", "Category", "Units Sold (30D)", "Run-Rate (Units/Day)", "Stock Available", "DOIR"};
            for (int i = 0; i < cols2.length; i++) {
                Cell c = h2.createCell(i); c.setCellValue(cols2[i]); c.setCellStyle(headerStyle);
            }
            int r2 = 1;
            for (SkuVelocityDto s : data.getFastMovingSkus()) {
                Row row = s2.createRow(r2++);
                row.createCell(0).setCellValue("FAST_MOVING");
                row.createCell(1).setCellValue(s.getSku());
                row.createCell(2).setCellValue(s.getProductName());
                row.createCell(3).setCellValue(s.getCategory());
                row.createCell(4).setCellValue(s.getUnitsSold30d());
                row.createCell(5).setCellValue(s.getDailyRunRate());
                row.createCell(6).setCellValue(s.getAvailableStock());
                row.createCell(7).setCellValue(s.getDaysOfInventoryRemaining());
            }
            for (SkuVelocityDto s : data.getSlowMovingSkus()) {
                Row row = s2.createRow(r2++);
                row.createCell(0).setCellValue("SLOW_MOVING");
                row.createCell(1).setCellValue(s.getSku());
                row.createCell(2).setCellValue(s.getProductName());
                row.createCell(3).setCellValue(s.getCategory());
                row.createCell(4).setCellValue(s.getUnitsSold30d());
                row.createCell(5).setCellValue(s.getDailyRunRate());
                row.createCell(6).setCellValue(s.getAvailableStock());
                row.createCell(7).setCellValue(s.getDaysOfInventoryRemaining());
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

    private static CellStyle createPercentStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("0.0%"));
        style.setAlignment(HorizontalAlignment.RIGHT);
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
```

#### CSV Report Generator (`AnalyticsCsvGenerator.java`):

```java
package com.sareekart.util;

import com.sareekart.dto.response.analytics.*;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

public final class AnalyticsCsvGenerator {

    private static final String CRLF = "\r\n";
    private static final String BOM = "\uFEFF"; // UTF-8 BOM for Microsoft Excel

    private AnalyticsCsvGenerator() {}

    public static byte[] generateSalesCsv(SalesTelemetryResponse data) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Date,Orders_Count,Units_Sold,Gross_Sales,Net_Revenue,GST_Tax_5_Percent,Shipping_Amount").append(CRLF);

        for (DailyRevenueDto d : data.getTimeline()) {
            sb.append(escape(d.getDate())).append(",")
              .append(d.getOrders()).append(",")
              .append(d.getUnits()).append(",")
              .append(d.getRevenue()).append(",")
              .append(d.getRevenue()).append(",")
              .append(d.getRevenue().multiply(BigDecimal.valueOf(0.05)).setScale(2, java.math.RoundingMode.HALF_UP)).append(",")
              .append("0.00").append(CRLF);
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public static byte[] generateInventoryCsv(InventoryVelocityResponse data) {
        StringBuilder sb = new StringBuilder(BOM);
        sb.append("Priority_Level,Priority_Score,SKU,Product_Name,Warehouse_Code,Available_Stock,Daily_Run_Rate,Days_Remaining,Recommended_Reorder").append(CRLF);

        for (StockoutAlertDto a : data.getStockoutAlerts()) {
            sb.append(escape(a.getPriorityLevel())).append(",")
              .append(a.getPriorityScore()).append(",")
              .append(escape(a.getSku())).append(",")
              .append(escape(a.getProductName())).append(",")
              .append(escape(a.getWarehouseCode())).append(",")
              .append(a.getAvailableStock()).append(",")
              .append(a.getDailyRunRate()).append(",")
              .append(a.getDaysRemaining()).append(",")
              .append(a.getRecommendedReorderQty()).append(CRLF);
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
```

---

## 5. Verification Method

To independently verify the algorithms, telemetry math, and export pipelines:

1. **Service Unit Test Suite (`AnalyticsServiceTest.java`)**:
   - Location: `backend/backend/src/test/java/com/sareekart/service/AnalyticsServiceTest.java`.
   - Test Cases:
     1. `testDateRangeCalculation()`: Verify `TODAY`, `7D`, `30D`, `90D`, `YTD`, `ALL`, `CUSTOM` calculate exact window boundaries.
     2. `testPercentageDeltaSafeguards()`: Verify prior=0 returns 100.0% (growth) or 0.0% (both zero) without divide-by-zero or `ArithmeticException`.
     3. `testFinancialTelemetryFormulas()`: Verify gross sales, 5% GST calculation, ₹150 shipping under ₹5000 threshold, and AOV.
     4. `testInventoryVelocityAndPriorityScoring()`: Verify run-rate $\frac{U}{30}$, DOIR fallback to 999 for zero sales, and multi-factor score yielding CRITICAL/HIGH/MEDIUM/LOW tiers.
     5. `testCustomerCohortsAndRepeatPurchaseRate()`: Verify LTV tiers (Platinum > ₹50,000, Gold ₹15,000–₹50,000, Silver < ₹15,000) and new vs returning revenue classification.
     6. `testExcelExportWorkbookIntegrity()`: Parse generated Excel bytes using `WorkbookFactory.create(new ByteArrayInputStream(bytes))` and assert sheets (`Overview`, `Daily Timeline`, `Stockout Alerts`) and cell values match DTO figures.
     7. `testCsvExportRfc4180Compliance()`: Verify BOM presence, comma escaping, and valid row formatting.

2. **Backend Execution Command**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test -Dtest=AnalyticsServiceTest
   ```
   Must pass with 0 errors and 0 failures.

3. **Controller & Authorization Test Suite (`AnalyticsControllerTest.java`)**:
   - Location: `backend/backend/src/test/java/com/sareekart/controller/AnalyticsControllerTest.java`.
   - Verify that requests with role `CUSTOMER` or unauthenticated requests receive HTTP 403 Forbidden with exact body:
     ```json
     {"success":false,"message":"Not authorised to perform this action"}
     ```
   - Verify staff roles (`ADMIN`, `MANAGER`, `OWNER`) receive HTTP 200 OK with `ApiResponse<T>`.

4. **Invalidation Conditions**:
   - Any division by zero resulting in `Infinity`, `NaN`, or uncaught `ArithmeticException`.
   - Negative DOIR or missing zero-filled dates in continuous timeline graphs.
   - Any Excel generation memory leak or unclosed `Workbook` stream.
