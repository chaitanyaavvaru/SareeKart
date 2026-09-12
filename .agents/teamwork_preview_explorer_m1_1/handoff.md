# Milestone M1: Data Models, Query Engineering & Analytics DTO Architecture Handoff Report

**Agent**: `teamwork_preview_explorer_m1_1`  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_1`  
**Target Codebase**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`  
**Date**: 2026-09-04  

---

## 1. Observation

### 1.1 Codebase & Build Environment
1. **Framework & Runtime**:
   - `pom.xml` (lines 7-10, line 30): Spring Boot `3.5.15` running on Java `17`.
   - Verified local runtime with `./mvnw -v`:
     ```
     Apache Maven 3.9.16
     Java version: 17.0.18, vendor: Homebrew
     OS name: "mac os x", version: 15.7.9, arch: "aarch64"
     ```
   - Baseline test suite execution `./mvnw test` completed with 100% pass rate:
     ```
     Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
     BUILD SUCCESS (Total time: 9.710 s)
     ```
   - Hibernate Version: `Hibernate ORM core version 6.6.53.Final`.
   - Database environments:
     - Runtime (`application.yaml`, lines 5-19): MySQL 8 (`sareekart_db`), `hibernate.ddl-auto: update`, dialect `org.hibernate.dialect.MySQLDialect`.
     - Test (`src/test/resources/application-test.yaml`, lines 1-16): H2 in-memory `jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE`, dialect `org.hibernate.dialect.H2Dialect`, `hibernate.ddl-auto: create-drop`.

### 1.2 Entity Definitions & Schema Structure
1. **`Order`** (`com.sareekart.entity.Order.java`, lines 15-62):
   - Table: `orders`
   - Fields:
     - `Long id` (PK, GenerationType.IDENTITY)
     - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) User user`
     - `@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) List<OrderItem> items`
     - `BigDecimal totalAmount` (precision = 10, scale = 2, nullable = false)
     - `@Enumerated(EnumType.STRING) OrderStatus status` (nullable = false)
     - `@Embedded Address shippingAddress`
     - `String paymentMethod`
     - `String paymentStatus`
     - `String razorpayOrderId`, `String razorpayPaymentId`
     - `@CreatedDate LocalDateTime createdAt`
     - `@LastModifiedDate LocalDateTime updatedAt`
2. **`OrderItem`** (`com.sareekart.entity.OrderItem.java`, lines 9-36):
   - Table: `order_items`
   - Fields:
     - `Long id` (PK, GenerationType.IDENTITY)
     - `@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id", nullable = false) Order order`
     - `@ManyToOne(fetch = FetchType.EAGER) @JoinColumn(name = "product_id", nullable = false) Product product`
     - `Integer quantity` (nullable = false)
     - `BigDecimal price` (precision = 10, scale = 2, nullable = false)
3. **`OrderStatus`** (`com.sareekart.entity.OrderStatus.java`, lines 3-9):
   - Values: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
4. **`Address`** (`com.sareekart.entity.Address.java`, lines 6-20):
   - `@Embeddable` with fields: `fullName`, `phone`, `streetAddress`, `city`, `state`, `pincode`.
   - Accessible from `Order` via `o.shippingAddress.state` and `o.shippingAddress.city`.
5. **`Product`** (`com.sareekart.entity.Product.java`, lines 13-62):
   - Table: `products`
   - Fields: `id`, `name`, `description`, `price`, `stockQuantity`, `category`, `active`, `createdAt`, `updatedAt`.
   - Note: Does not maintain warehouse SKU strings directly (handled in `InventoryItem`).
6. **`InventoryItem`** (`com.sareekart.entity.InventoryItem.java`, lines 11-79):
   - Table: `inventory_items`
   - Fields: `id`, `sku` (unique), `productId`, `productName`, `category`, `warehouseCode` (default "WH-01"), `warehouseName`, `binLocation`, `onHand`, `reserved`, `available`, `unitPrice`, `status` ("IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK"), `updatedAt`.
   - Contains business method `recalculateStatus()`: `available = max(0, onHand - reserved)`.
7. **`User`** (`com.sareekart.entity.User.java`, lines 16-83):
   - Table: `users`
   - Fields: `id`, `firstName`, `lastName`, `email`, `mobile`, `password`, `role` (`Role.CUSTOMER`, `Role.MANAGER`, `Role.OWNER`, `Role.ADMIN`), `createdAt`, `updatedAt`.
8. **`Cart` & `CartItem`** (`com.sareekart.entity.Cart.java` & `CartItem.java`):
   - Table: `carts`, `cart_items`
   - `Cart` has `@OneToMany List<CartItem> items`, `user`, `createdAt`, `updatedAt`.
9. **`Coupon`** (`com.sareekart.entity.Coupon.java`, lines 8-46):
   - Table: `coupons`
   - Fields: `id`, `code` (unique), `discountPercent`, `discountAmount`, `minPurchaseAmount`, `usageLimit`, `timesUsed`, `active`, `isDeleted`, `expiryDate`.

### 1.3 Repository Landscape
1. `com.sareekart.repository.OrderItemRepository`:
   - **Does not exist**. Missing entirely from the codebase.
2. `com.sareekart.repository.OrderRepository`:
   - Contains only:
     - `findByUserIdOrderByCreatedAtDesc(Long userId)`
     - `@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'") BigDecimal sumTotalRevenue()`
     - `findTop5ByOrderByCreatedAtDesc()`
   - Lacks date-range filtering, timeline aggregation, payment method grouping, geographic grouping, customer cohort grouping, and status filtering.
3. `com.sareekart.repository.InventoryItemRepository`:
   - Contains basic lookups by SKU, product ID, and warehouse code, but lacks stock valuation aggregation and threshold alerts.
4. `com.sareekart.repository.CartRepository`:
   - Contains only `findByUserId(Long userId)`. Lacks cart counts for funnel conversion analysis.

---

## 2. Logic Chain

### 2.1 Aggregation & Filter Semantics
1. **Non-Cancelled vs Completed Orders**:
   - Order lifecycle: Upon placement (`OrderServiceImpl.java:94`), status is initialized to `PENDING`. Upon successful payment or verification (`PaymentServiceImpl.java:128`), status becomes `CONFIRMED`. Subsequently, orders move to `SHIPPED`, `DELIVERED`, or `CANCELLED`.
   - In sales telemetry, **Gross Sales** and **Net Revenue** must include all non-cancelled orders (`status != 'CANCELLED'`).
   - For conversion funnel and completed transaction metrics, completed orders are those in `status != 'CANCELLED'` (or `status IN ('CONFIRMED', 'SHIPPED', 'DELIVERED')`).
2. **Gross Sales vs Net Revenue Mathematical Derivation**:
   - In `OrderServiceImpl.java:76-90`:
     $$\text{subtotal} = \sum (\text{item.price} \times \text{item.quantity})$$
     $$\text{totalAmount} = \text{subtotal} - \text{discount} + \text{shippingFee}$$
   - Therefore:
     - **Gross Sales** is the sum of catalog item subtotal before discounts:
       $$\text{Gross Sales} = \sum_{\text{non-cancelled}} (\text{oi.price} \times \text{oi.quantity})$$
     - **Net Revenue** is the realized order total:
       $$\text{Net Revenue} = \sum_{\text{non-cancelled}} \text{o.totalAmount}$$
     - **Tax (5% GST)**: As specified in `PROJECT.md:21` (5% GST for textiles), tax is calculated as:
       $$\text{Tax Amount} = \text{Net Revenue} \times \frac{0.05}{1.05}$$
     - **Shipping Amount**: Orders with subtotal $< 5000$ incur ₹150 shipping fee:
       $$\text{Shipping Amount} = \text{Count}(\text{Orders with shipping fee applied}) \times 150.00$$
     - **Average Order Value (AOV)**:
       $$\text{AOV} = \begin{cases} \frac{\text{Net Revenue}}{\text{Completed Orders}}, & \text{if Completed Orders} > 0 \\ 0.00, & \text{otherwise} \end{cases}$$
3. **Cartesian Product Prevention in JPQL**:
   - If `Order o` is joined with `OrderItem oi` (`FROM Order o JOIN o.items oi`), summing `o.totalAmount` will duplicate `totalAmount` for each item in the order!
   - To avoid this data corruption:
     - Direct order amounts (`Net Revenue`, `Order Count`, `Payment Distribution`, `Regional Breakdown`) must be queried from `OrderRepository` without joins to item tables.
     - Product-level item sums (`Units Sold`, `Gross Sales`) must be queried from `OrderItemRepository` directly (`FROM OrderItem oi WHERE oi.order.createdAt BETWEEN ...`).
     - Daily timeline metrics can either be queried separately (daily revenue from `OrderRepository` + daily units from `OrderItemRepository`) or computed via Java stream iteration over `findByCreatedAtBetweenAndStatusNot`.

### 2.2 Date Functions & Dialect Portability
1. In MySQL and H2 (version 2.3.232), `FUNCTION('DATE', o.createdAt)` and `CAST(o.createdAt AS LocalDate)` are supported by Hibernate 6.6.
2. In Java 8 / 17 `AnalyticsService`, continuous timeline points (filling days with 0 sales) are required by frontend charts to prevent missing dates in graphs. Providing both the JPQL timeline query and the `findByCreatedAtBetweenAndStatusNot` query ensures optimal performance and deterministic mockability in unit tests.

### 2.3 Velocity, Aging & Cohort Rules
1. **Fast vs Slow Moving SKUs**:
   - Fast-Moving SKUs: Top products by total units sold in the selected interval (`ORDER BY SUM(oi.quantity) DESC`).
   - Slow-Moving SKUs: Products with zero or minimal units sold in the interval (`ORDER BY SUM(oi.quantity) ASC` or available products with 0 units).
   - Daily Run-Rate: $\text{Daily Run Rate} = \frac{\text{Units Sold}}{\text{Days in Period}}$.
   - Days of Inventory Remaining: $\frac{\text{Available Stock}}{\text{Daily Run Rate}}$ (fallback to `999.0` when run rate is 0).
2. **Inventory Aging Categorization**:
   - `< 30 Days`: Items updated within the last 30 days (`updatedAt >= now - 30 days`).
   - `30–90 Days`: Items updated between 30 and 90 days ago (`now - 90 days <= updatedAt < now - 30 days`).
   - `> 90 Days`: Items updated more than 90 days ago (`updatedAt < now - 90 days`).
   - Valuation: $\text{Available Units} \times \text{Unit Price}$.
3. **Customer LTV Tiers & Cohorts**:
   - Platinum: Spend $> ₹50,000$
   - Gold: Spend between $₹15,000$ and $₹50,000$
   - Silver: Spend $< ₹15,000$
   - Repeat Purchase Rate: $\frac{\text{Returning Customers (} \ge 2 \text{ orders)}}{\text{Total Ordering Customers}} \times 100\%$
4. **Conversion Funnel**:
   - Carts Created: `cartRepository.count()`
   - Carts with Items: `cartRepository.countCartsWithItems()`
   - Checkout Initiated: Total orders placed in period (including `PENDING`)
   - Orders Completed: Non-cancelled orders placed in period
   - Cart Abandonment Rate: $\frac{\text{Carts with Items} - \text{Orders Completed}}{\text{Carts with Items}} \times 100\%$

---

## 3. Caveats

1. **Legacy Order Schema Backwards Compatibility**:
   - Existing records in `sareekart_db_latest.sql` do not contain dedicated columns for `coupon_code` or `discount_amount`.
   - The query designs below safely query existing columns (`totalAmount`, `status`, `paymentMethod`, `shippingAddress`, `createdAt`).
   - If optional columns (`couponCode`, `discountAmount`) are added to `Order.java`, Hibernate `ddl-auto: update` adds them automatically; fallback coupon ROI logic can leverage `CouponRepository` (`timesUsed`, `discountPercent`).
2. **Null Safety in Embeddables**:
   - `o.shippingAddress` or `o.shippingAddress.state` could theoretically be null in test fixtures. JPQL queries must include `WHERE o.shippingAddress.state IS NOT NULL`.
3. **Division by Zero Protection**:
   - Percentage changes ($\Delta\%$), AOV, run-rates, repeat purchase rates, and abandonment rates must include explicit ternary guards (`denominator > 0 ? ... : 0.0`) to avoid `NaN` or `Infinity` in JSON payloads.

---

## 4. Conclusion & Complete Design Specifications

### 4.1 New Repository: `OrderItemRepository.java`
Target Location: `backend/backend/src/main/java/com/sareekart/repository/OrderItemRepository.java`

```java
package com.sareekart.repository;

import com.sareekart.entity.OrderItem;
import com.sareekart.repository.projection.DailyUnitsProjection;
import com.sareekart.repository.projection.ProductSalesProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByProductId(Long productId);

    /**
     * Aggregates units sold and total revenue grouped by product within a date range for non-cancelled orders.
     * Orders descending by units sold for fast-moving SKU identification.
     */
    @Query("SELECT oi.product.id AS productId, " +
           "oi.product.name AS productName, " +
           "COALESCE(SUM(oi.quantity), 0) AS totalUnitsSold, " +
           "COALESCE(SUM(oi.price * oi.quantity), 0) AS totalRevenue " +
           "FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesProjection> findProductSalesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregates units sold grouped by product for all-time non-cancelled orders.
     */
    @Query("SELECT oi.product.id AS productId, " +
           "oi.product.name AS productName, " +
           "COALESCE(SUM(oi.quantity), 0) AS totalUnitsSold, " +
           "COALESCE(SUM(oi.price * oi.quantity), 0) AS totalRevenue " +
           "FROM OrderItem oi " +
           "WHERE oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY oi.product.id, oi.product.name " +
           "ORDER BY SUM(oi.quantity) DESC")
    List<ProductSalesProjection> findProductSalesAllTime();

    /**
     * Sums total units sold within a date range for non-cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    Long sumTotalUnitsSoldBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Sums gross sales (item price * quantity before order-level discounts) in date range.
     */
    @Query("SELECT COALESCE(SUM(oi.price * oi.quantity), 0) FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED")
    BigDecimal sumGrossSalesBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Aggregates daily units sold for timeline chart merging.
     */
    @Query("SELECT CAST(oi.order.createdAt AS LocalDate) AS orderDate, " +
           "COALESCE(SUM(oi.quantity), 0) AS totalUnits " +
           "FROM OrderItem oi " +
           "WHERE oi.order.createdAt >= :startDate AND oi.order.createdAt <= :endDate " +
           "AND oi.order.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY CAST(oi.order.createdAt AS LocalDate) " +
           "ORDER BY CAST(oi.order.createdAt AS LocalDate) ASC")
    List<DailyUnitsProjection> findDailyUnitsSoldBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
```

---

### 4.2 Enhanced Repository: `OrderRepository.java`
Target Location: `backend/backend/src/main/java/com/sareekart/repository/OrderRepository.java`

```java
package com.sareekart.repository;

import com.sareekart.entity.Order;
import com.sareekart.entity.OrderStatus;
import com.sareekart.repository.projection.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'")
    BigDecimal sumTotalRevenue();

    List<Order> findTop5ByOrderByCreatedAtDesc();

    // ==================== Milestone M1 Analytics Telemetry Queries ====================

    /**
     * Net revenue sum (totalAmount) within [startDate, endDate] for non-cancelled orders.
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED")
    BigDecimal sumNetRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts completed / active non-cancelled transactions within date range.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED")
    long countNonCancelledOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts completed orders specifically in CONFIRMED, SHIPPED, or DELIVERED status.
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status IN (com.sareekart.entity.OrderStatus.CONFIRMED, " +
           "com.sareekart.entity.OrderStatus.SHIPPED, " +
           "com.sareekart.entity.OrderStatus.DELIVERED)")
    long countCompletedOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Counts total orders placed in period regardless of status (for checkout initiation).
     */
    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate")
    long countTotalOrdersBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Daily revenue and order volume grouping for trend graphs.
     */
    @Query("SELECT CAST(o.createdAt AS LocalDate) AS orderDate, " +
           "COUNT(o) AS orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) AS totalRevenue " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY CAST(o.createdAt AS LocalDate) " +
           "ORDER BY CAST(o.createdAt AS LocalDate) ASC")
    List<DailyRevenueProjection> findDailyRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Grouping orders by payment method (UPI, Cards, NetBanking, COD) with count and amount.
     */
    @Query("SELECT COALESCE(o.paymentMethod, 'UNKNOWN') AS paymentMethod, " +
           "COUNT(o) AS orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) AS totalAmount " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY o.paymentMethod " +
           "ORDER BY SUM(o.totalAmount) DESC")
    List<PaymentDistributionProjection> findPaymentMethodDistributionBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Regional demand breakdown grouped by shipping state.
     */
    @Query("SELECT o.shippingAddress.state AS state, " +
           "COUNT(o) AS orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) AS totalRevenue " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "AND o.shippingAddress.state IS NOT NULL " +
           "GROUP BY o.shippingAddress.state " +
           "ORDER BY SUM(o.totalAmount) DESC")
    List<StateDemandProjection> findStateDemandBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Regional demand breakdown grouped by shipping city and state.
     */
    @Query("SELECT o.shippingAddress.city AS city, " +
           "o.shippingAddress.state AS state, " +
           "COUNT(o) AS orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) AS totalRevenue " +
           "FROM Order o " +
           "WHERE o.createdAt >= :startDate AND o.createdAt <= :endDate " +
           "AND o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "AND o.shippingAddress.city IS NOT NULL " +
           "GROUP BY o.shippingAddress.city, o.shippingAddress.state " +
           "ORDER BY SUM(o.totalAmount) DESC")
    List<CityDemandProjection> findCityDemandBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Customer spend, order count, and first/last order dates for LTV tier segmentation.
     */
    @Query("SELECT o.user.id AS userId, " +
           "o.user.firstName AS firstName, " +
           "o.user.lastName AS lastName, " +
           "o.user.email AS email, " +
           "COUNT(o) AS orderCount, " +
           "COALESCE(SUM(o.totalAmount), 0) AS totalSpend, " +
           "MIN(o.createdAt) AS firstOrderDate, " +
           "MAX(o.createdAt) AS lastOrderDate " +
           "FROM Order o " +
           "WHERE o.status != com.sareekart.entity.OrderStatus.CANCELLED " +
           "GROUP BY o.user.id, o.user.firstName, o.user.lastName, o.user.email")
    List<CustomerSpendProjection> findCustomerSpendAllTime();

    /**
     * Fetches non-cancelled orders in date range for high-fidelity in-memory stream processing.
     */
    List<Order> findByCreatedAtBetweenAndStatusNot(
            LocalDateTime startDate,
            LocalDateTime endDate,
            OrderStatus status);
}
```

---

### 4.3 Projection Interfaces
Target Location: `backend/backend/src/main/java/com/sareekart/repository/projection/`

1. **`ProductSalesProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   public interface ProductSalesProjection {
       Long getProductId();
       String getProductName();
       Long getTotalUnitsSold();
       BigDecimal getTotalRevenue();
   }
   ```
2. **`DailyRevenueProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   public interface DailyRevenueProjection {
       Object getOrderDate();
       Long getOrderCount();
       BigDecimal getTotalRevenue();
   }
   ```
3. **`DailyUnitsProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   public interface DailyUnitsProjection {
       Object getOrderDate();
       Long getTotalUnits();
   }
   ```
4. **`PaymentDistributionProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   public interface PaymentDistributionProjection {
       String getPaymentMethod();
       Long getOrderCount();
       BigDecimal getTotalAmount();
   }
   ```
5. **`StateDemandProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   public interface StateDemandProjection {
       String getState();
       Long getOrderCount();
       BigDecimal getTotalRevenue();
   }
   ```
6. **`CityDemandProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   public interface CityDemandProjection {
       String getCity();
       String getState();
       Long getOrderCount();
       BigDecimal getTotalRevenue();
   }
   ```
7. **`CustomerSpendProjection.java`**:
   ```java
   package com.sareekart.repository.projection;
   import java.math.BigDecimal;
   import java.time.LocalDateTime;
   public interface CustomerSpendProjection {
       Long getUserId();
       String getFirstName();
       String getLastName();
       String getEmail();
       Long getOrderCount();
       BigDecimal getTotalSpend();
       LocalDateTime getFirstOrderDate();
       LocalDateTime getLastOrderDate();
   }
   ```

---

### 4.4 Repository Enhancements for `InventoryItemRepository` and `CartRepository`

1. **`InventoryItemRepository.java` additions**:
   ```java
   @Query("SELECT COALESCE(SUM(i.available), 0) FROM InventoryItem i")
   Integer sumTotalAvailableStock();

   @Query("SELECT COALESCE(SUM(i.available * i.unitPrice), 0) FROM InventoryItem i")
   BigDecimal sumTotalStockValuation();

   long countByStatus(String status);

   @Query("SELECT i FROM InventoryItem i WHERE i.available <= :threshold ORDER BY i.available ASC, i.updatedAt DESC")
   List<InventoryItem> findStockoutAlerts(@Param("threshold") Integer threshold);

   List<InventoryItem> findByUpdatedAtGreaterThanEqual(LocalDateTime thresholdDate);
   List<InventoryItem> findByUpdatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
   List<InventoryItem> findByUpdatedAtLessThan(LocalDateTime thresholdDate);
   ```

2. **`CartRepository.java` additions**:
   ```java
   @Query("SELECT COUNT(c) FROM Cart c WHERE SIZE(c.items) > 0")
   long countCartsWithItems();
   ```

---

### 4.5 Analytics Response DTO Definitions
Target Location: `backend/backend/src/main/java/com/sareekart/dto/response/analytics/`

#### 1. `AnalyticsOverviewResponse.java`
```java
package com.sareekart.dto.response.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalyticsOverviewResponse {

    // Current period totals
    private BigDecimal grossSales;
    private BigDecimal netRevenue;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal aov;
    private Long completedOrders;
    private Long totalUnitsSold;

    // Prior comparative period totals
    private BigDecimal priorGrossSales;
    private BigDecimal priorNetRevenue;
    private BigDecimal priorTaxAmount;
    private BigDecimal priorShippingAmount;
    private BigDecimal priorAov;
    private Long priorCompletedOrders;

    // Period-over-period percentage changes: "grossSales" -> +18.4, "aov" -> -3.2
    private Map<String, Double> percentageChanges;

    // Context
    private String range;
    private String startDate;
    private String endDate;
    private String priorStartDate;
    private String priorEndDate;
}
```

#### 2. `SalesTelemetryResponse.java`
```java
package com.sareekart.dto.response.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesTelemetryResponse {

    // Summary totals
    private BigDecimal grossSales;
    private BigDecimal netRevenue;
    private BigDecimal taxAmount;
    private BigDecimal shippingAmount;
    private BigDecimal discountAmount;
    private BigDecimal aov;
    private Long completedOrders;
    private Long totalUnitsSold;

    // Visual timeline points for charts
    private List<SalesTimelinePoint> timeline;

    // Payment method distribution
    private List<PaymentDistributionItem> paymentDistribution;

    // Coupon campaign utilization & ROI
    private List<CouponUtilizationItem> couponUtilization;

    // Interval context
    private String range;
    private String startDate;
    private String endDate;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SalesTimelinePoint {
        private String date;         // YYYY-MM-DD
        private BigDecimal revenue;
        private Long orders;
        private Long units;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentDistributionItem {
        private String method;       // UPI, CREDIT_CARD, NET_BANKING, COD
        private Long count;
        private BigDecimal amount;
        private Double percentage;   // e.g. 45.5
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CouponUtilizationItem {
        private String code;
        private Integer uses;
        private BigDecimal discountTotal;
        private BigDecimal revenueGenerated;
        private Double roi;          // revenueGenerated / discountTotal
    }
}
```

#### 3. `InventoryVelocityResponse.java`
```java
package com.sareekart.dto.response.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryVelocityResponse {

    private Double daysOfInventoryRemaining;
    private Integer totalAvailableStock;
    private BigDecimal totalStockValuation;
    private Integer lowStockCount;
    private Integer outOfStockCount;

    // Fast-moving & Slow-moving SKU rankings
    private List<SkuVelocityItem> fastMovingSkus;
    private List<SkuVelocityItem> slowMovingSkus;

    // Aging categories (<30d, 30-90d, >90d)
    private List<InventoryAgingCategory> agingCategories;

    // Stockout replenishment priority alerts
    private List<StockoutAlertItem> stockoutAlerts;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SkuVelocityItem {
        private String sku;
        private String name;
        private String category;
        private Long unitsSold;
        private Double runRate;               // unitsSold / daysInPeriod
        private Integer availableStock;
        private Double daysRemaining;         // availableStock / runRate
        private String status;                // FAST_MOVING or SLOW_MOVING
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class InventoryAgingCategory {
        private String category;              // LESS_THAN_30_DAYS, THIRTY_TO_NINETY_DAYS, MORE_THAN_90_DAYS
        private String displayName;           // "< 30 Days", "30–90 Days", "> 90 Days"
        private Long itemCount;
        private Long totalUnits;
        private BigDecimal valuation;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockoutAlertItem {
        private String sku;
        private String name;
        private String warehouse;             // warehouseCode or warehouseName
        private String binLocation;
        private Integer available;
        private Integer onHand;
        private Integer reserved;
        private Double daysRemaining;
        private String status;                // OUT_OF_STOCK, CRITICAL, LOW_STOCK
        private Integer priorityScore;        // 1 - 100 (Critical 90-100, High 70-89, Med 40-69, Low <40)
    }
}
```

#### 4. `CustomerAnalyticsResponse.java`
```java
package com.sareekart.dto.response.analytics;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAnalyticsResponse {

    // Customer LTV tiers
    private List<LtvTierSummary> ltvTiers;

    // New vs Returning customer cohort
    private CustomerCohortSummary newVsReturning;

    // Regional demand breakdown
    private RegionalDemandSummary regionalBreakdown;

    // Cart abandonment & conversion funnel
    private ConversionFunnelSummary conversionFunnel;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LtvTierSummary {
        private String tier;                  // PLATINUM, GOLD, SILVER
        private String displayName;           // "Platinum (>₹50,000)", etc.
        private Long customerCount;
        private BigDecimal totalRevenue;
        private BigDecimal averageSpend;
        private Double percentageOfCustomers;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerCohortSummary {
        private Long newCustomers;
        private Long returningCustomers;
        private Long totalOrderingCustomers;
        private BigDecimal newRevenue;
        private BigDecimal returningRevenue;
        private Double repeatPurchaseRate;    // (returningCustomers / totalOrderingCustomers) * 100
        private BigDecimal averageLtv;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegionalDemandSummary {
        private List<RegionDemandItem> topStates;
        private List<RegionDemandItem> topCities;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegionDemandItem {
        private String name;                  // State name or City name
        private String state;                 // For cities, parent state
        private Long orderCount;
        private BigDecimal revenue;
        private Double percentage;            // Percentage of total regional revenue
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ConversionFunnelSummary {
        private Long cartsCreated;
        private Long cartsWithItems;
        private Long checkoutInitiated;
        private Long ordersCompleted;
        private Double cartAbandonmentRate;   // ((cartsWithItems - ordersCompleted) / cartsWithItems) * 100
        private Double checkoutAbandonmentRate;// ((checkoutInitiated - ordersCompleted) / checkoutInitiated) * 100
        private Double overallConversionRate; // (ordersCompleted / cartsCreated) * 100
    }
}
```

---

## 5. Verification Method

### 5.1 Independent Code & Schema Verification
1. **Compile & Test Verification**:
   Execute the Spring Boot test build inside `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend`:
   ```bash
   ./mvnw test-compile
   ./mvnw test
   ```
   *Expected result*: Repository queries compile with 0 errors. Spring Data JPA repository bootstrapping succeeds in both MySQL and H2 dialect modes.

2. **Entity & Property Integrity Check**:
   Confirm every referenced entity property exists in the Java classes:
   - `OrderItem`: `order`, `product`, `quantity`, `price` (in `OrderItem.java`).
   - `Order`: `totalAmount`, `status`, `shippingAddress.state`, `shippingAddress.city`, `paymentMethod`, `createdAt`, `user` (in `Order.java`).
   - `Product`: `id`, `name`, `price`, `stockQuantity` (in `Product.java`).
   - `InventoryItem`: `sku`, `productName`, `available`, `unitPrice`, `status`, `updatedAt` (in `InventoryItem.java`).
   - `User`: `id`, `firstName`, `lastName`, `email` (in `User.java`).

3. **Invalidation Conditions**:
   - Query compilation fails if any alias or JPQL entity property name mismatches existing entity getters.
   - Any query returning cartesian duplicates (e.g. `SUM(o.totalAmount)` on joined item queries) invalidates revenue accuracy.
   - Any DTO field renaming that breaks the REST API contract defined in `PROJECT.md:41-64` invalidates frontend interoperability.
