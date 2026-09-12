# Backend Architecture & Domain Telemetry Survey Report

**Agent**: `teamwork_preview_explorer_survey_backend`  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend`  
**Target Codebase**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`  
**Date**: 2026-09-04  

---

## 1. Observation

### 1.1 Project & Build Environment
- **Build System**: Apache Maven wrapper `./mvnw` with Spring Boot `3.5.15` and Java `17` (`pom.xml`, lines 7-31).
- **Test Suite Status**: Executing `./mvnw test` passes with zero failures and zero errors:
  ```
  [INFO] Results:
  [INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
  [INFO] BUILD SUCCESS
  [INFO] Total time:  9.599 s
  ```
- **Database Configuration**:
  - Production datasource (`application.yaml`, lines 5-19): MySQL database `sareekart_db` on `localhost:3306`, Hibernate `ddl-auto: update`, dialect `org.hibernate.dialect.MySQLDialect`.
  - Test datasource (`src/test/resources/application-test.yaml`, lines 1-16): H2 in-memory DB `jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE`, dialect `org.hibernate.dialect.H2Dialect`, Hibernate `ddl-auto: create-drop`.
- **Existing Dependencies**:
  - Apache POI `org.apache.poi:poi-ooxml:5.3.0` is already present in `pom.xml` (lines 137-141) and currently utilized by `ExcelProcessingService.java`.
  - Spring Security `spring-boot-starter-security` and JWT `io.jsonwebtoken:jjwt-api:0.12.6` (`pom.xml`, lines 78-102).

---

### 1.2 Domain Entities & Schemas

| Entity | Table Name | Key Fields & Types | Relationships & Notes |
|---|---|---|---|
| **Order** | `orders` | `id` (Long, PK)<br>`totalAmount` (BigDecimal 10,2)<br>`status` (OrderStatus enum)<br>`paymentMethod` (String)<br>`paymentStatus` (String)<br>`razorpayOrderId` (String)<br>`razorpayPaymentId` (String)<br>`createdAt` (LocalDateTime, @CreatedDate)<br>`updatedAt` (LocalDateTime, @LastModifiedDate) | `@ManyToOne User user` (`user_id`, nullable=false)<br>`@OneToMany List<OrderItem> items` (cascade=ALL, orphanRemoval=true)<br>`@Embedded Address shippingAddress` |
| **OrderItem** | `order_items` | `id` (Long, PK)<br>`quantity` (Integer)<br>`price` (BigDecimal 10,2) | `@ManyToOne Order order` (`order_id`)<br>`@ManyToOne(EAGER) Product product` (`product_id`) |
| **OrderStatus** | Enum | `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED` | Order cancellation reverts product stock in `OrderServiceImpl.java:209-214`. |
| **Address** | Embeddable | `fullName`, `phone`, `streetAddress`, `city`, `state`, `pincode` | Embedded inside `Order` as `shippingAddress`. Direct source for geographic demand analytics (city, state). |
| **Product** | `products` | `id` (Long, PK)<br>`name` (String)<br>`description` (TEXT)<br>`price` (BigDecimal 10,2)<br>`stockQuantity` (Integer)<br>`fabric`, `occasion`, `color` (String)<br>`active` (Boolean)<br>`createdAt`, `updatedAt` (LocalDateTime) | `@ManyToOne Category category`<br>`@ElementCollection List<String> images` (`product_images`) |
| **Category** | `categories` | `id` (Long, PK)<br>`name` (String, Unique)<br>`description` (String)<br>`imageUrl` (String) | `@OneToMany List<Product> products` |
| **InventoryItem** | `inventory_items` | `id` (Long, PK)<br>`sku` (String, Unique)<br>`productId` (Long)<br>`productName` (String)<br>`category` (String)<br>`warehouseCode` (String, default "WH-01")<br>`warehouseName` (String, default "Bengaluru Central Fulfillment Hub")<br>`binLocation` (String)<br>`onHand` (Integer)<br>`reserved` (Integer)<br>`available` (Integer)<br>`unitPrice` (BigDecimal 10,2)<br>`status` (String: "IN_STOCK", "LOW_STOCK", "OUT_OF_STOCK")<br>`updatedAt` (LocalDateTime) | Includes `recalculateStatus()` method (lines 69-78): `available = onHand - reserved`. Status is `OUT_OF_STOCK` if `<= 0`, `LOW_STOCK` if `<= 5`, else `IN_STOCK`. |
| **User** | `users` | `id` (Long, PK)<br>`firstName`, `lastName` (String)<br>`email` (String, Unique)<br>`mobile`, `password` (String)<br>`role` (Role enum)<br>`createdAt`, `updatedAt` (LocalDateTime) | Implements `UserDetails`. `getAuthorities()` returns `ROLE_` + `role.name()`. |
| **Role** | Enum | `CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN` | Authorities: `ROLE_CUSTOMER`, `ROLE_MANAGER`, `ROLE_OWNER`, `ROLE_ADMIN`. |
| **Coupon** | `coupons` | `id` (Long, PK)<br>`code` (String, Unique)<br>`discountPercent` (Double)<br>`discountAmount` (Double)<br>`minPurchaseAmount` (Double)<br>`usageLimit` (Integer)<br>`timesUsed` (Integer)<br>`active` (Boolean)<br>`isDeleted` (Boolean)<br>`expiryDate` (LocalDateTime) | Used in `OrderServiceImpl.java:80-87` to calculate order discount percentage. |
| **Cart & CartItem** | `carts`, `cart_items` | Cart: `id`, `user`, `items`, `createdAt`, `updatedAt`<br>CartItem: `id`, `cart`, `product`, `quantity` | Tracks customer cart contents and provides cart abandonment / conversion funnel telemetry. |

---

### 1.3 Existing Repositories & Method Capabilities

1. **`OrderRepository`** (`com.sareekart.repository.OrderRepository.java`, lines 1-19):
   - `findByUserIdOrderByCreatedAtDesc(Long userId)`
   - `@Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'CANCELLED'") BigDecimal sumTotalRevenue()`
   - `findTop5ByOrderByCreatedAtDesc()`
   - *Missing*: Date-range filtering, payment method breakdown, geographic state/city grouping, completed transactions counting.
2. **`ProductRepository`** (`com.sareekart.repository.ProductRepository.java`, lines 1-45):
   - `findByStockQuantityLessThan(Integer threshold)`
   - `findByActiveTrue(Pageable pageable)`, `findByCategoryIdAndActiveTrue(Long categoryId, Pageable pageable)`
   - `searchProducts(...)`, `findByFilters(...)`
3. **`InventoryItemRepository`** (`com.sareekart.repository.InventoryItemRepository.java`, lines 1-17):
   - `findBySku(String sku)`, `findByProductId(Long productId)`
   - `findByWarehouseCode(String warehouseCode)`, `findAllByOrderByUpdatedAtDesc()`
4. **`CouponRepository`** (`com.sareekart.repository.CouponRepository.java`, lines 1-16):
   - `findByCode(String code)`, `findByCodeAndIsDeletedFalse(String code)`, `findByIsDeletedFalse()`
5. **`CartRepository`** (`com.sareekart.repository.CartRepository.java`, lines 1-14):
   - `findByUserId(Long userId)`
6. **`UserRepository`** (`com.sareekart.repository.UserRepository.java`, lines 1-18):
   - `findByEmail(String email)`, `existsByEmail(String email)`, `countByRole(Role role)`
7. **`OrderItemRepository`**:
   - **Does not exist**. No repository currently manages `OrderItem` directly.

---

### 1.4 Existing Controllers & Admin Endpoints

- **`AdminController`** (`com.sareekart.controller.AdminController.java`, lines 1-34):
  - `@GetMapping("/dashboard")` -> returns `AdminDashboardResponse` (`totalProducts`, `totalOrders`, `totalCustomers`, `totalRevenue`, `lowStockProductsCount`, `recentOrders`).
  - `@GetMapping("/inventory/low-stock")` -> returns `List<ProductResponse>` with stock below threshold.
- **`InventoryController`** (`com.sareekart.controller.InventoryController.java`, lines 1-63):
  - `@GetMapping` -> returns list of `InventoryItem` sorted by updated date.
  - `@PostMapping("/adjust")` -> submits stock adjustment approval request.
- **`ExcelTransactionController`** (`com.sareekart.controller.ExcelTransactionController.java`, lines 1-86):
  - `@GetMapping("/templates/{type}")` -> downloads Excel `.xlsx` template.
  - `@PostMapping("/preview")` and `@PostMapping("/import")` -> parses and validates bulk Excel records.
- **`OrderController`** (`com.sareekart.controller.OrderController.java`, lines 57-70):
  - `@GetMapping("/admin/orders")` -> returns all orders.
  - `@PutMapping("/admin/orders/{id}/status")` -> updates order status.

---

### 1.5 Security Configuration & Access Control (R5)

1. **`SecurityConfig.java`** (lines 53-76):
   - Path-level rule:
     ```java
     .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
     ```
   - Access Denied Handler:
     ```java
     .accessDeniedHandler((request, response, accessDeniedException) -> {
         response.setStatus(HttpStatus.FORBIDDEN.value());
         response.setContentType(MediaType.APPLICATION_JSON_VALUE);
         response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
     })
     ```
   - Authentication Entry Point:
     ```java
     .authenticationEntryPoint((request, response, authException) -> {
         response.setStatus(HttpStatus.UNAUTHORIZED.value());
         response.setContentType(MediaType.APPLICATION_JSON_VALUE);
         response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please sign in again.\"}");
     })
     ```
2. **`GlobalExceptionHandler.java`** (lines 19-112):
   - Does **not** contain an explicit handler for `org.springframework.security.access.AccessDeniedException`.
   - Any method-level security exception (`@PreAuthorize`) passing to `@ExceptionHandler(Exception.class)` would return HTTP 500 (`"A server error occurred. Please try again later."`) instead of HTTP 403.
3. **E2E Test Expectation** (`frontend/tests/approval.spec.js`, lines 32-40):
   ```javascript
   const approvalRes = await request.get('http://localhost:8081/api/approvals/pending', {
     headers: { Authorization: `Bearer ${token}` }
   });
   expect(approvalRes.status()).toBe(403);
   const errBody = await approvalRes.json();
   expect(errBody.message).toBe('Not authorised to perform this action');
   ```

---

## 2. Logic Chain

### 2.1 Sales & Financial Telemetry Engine (R1)
1. **Configurable Time Intervals**:
   - `Order.createdAt` records creation timestamps.
   - Intervals (`TODAY`, `7D`, `30D`, `90D`, `YTD`, `ALL`, and `CUSTOM` with `startDate` and `endDate`) can be converted to a `[startDateTime, endDateTime]` window.
   - For period-over-period comparison, the prior comparative duration is calculated as `[startDateTime - duration, startDateTime]`.
2. **Gross Sales vs Net Revenue**:
   - In `OrderServiceImpl.java:76-90`, order creation computes:
     - `subtotal = sum(item.price * item.quantity)`
     - `discount = subtotal * (coupon.discountPercent / 100)`
     - `shippingFee = subtotal >= 5000 ? 0 : 150`
     - `totalAmount = subtotal - discount + shippingFee`
   - Therefore:
     - **Gross Sales** = `SUM(orderItem.price * orderItem.quantity)` for non-cancelled orders in period.
     - **Net Revenue** = `SUM(order.totalAmount)` for non-cancelled orders in period.
     - **Tax Amount** = 5% textile GST component (`netRevenue * 0.05 / 1.05` or calculated 5% GST on net order total).
     - **Shipping Amount** = Count of non-cancelled orders with `totalAmount < 5000` * ₹150.00.
     - **Completed Transactions** = `COUNT(order)` where `status != 'CANCELLED'`.
     - **Average Order Value (AOV)** = `Net Revenue / Completed Transactions` (or `0.00` if count == 0).
3. **Payment Method Distribution**:
   - `Order.paymentMethod` stores `RAZORPAY`, `COD`, `UPI`, etc.
   - Grouping `Order` by `paymentMethod` produces transaction count, revenue sum, and percentage breakdown.
4. **Coupon Utilization & ROI**:
   - `Coupon` entity tracks `code`, `timesUsed`, `discountPercent`, `discountAmount`, `usageLimit`, `active`.
   - Orders placed with coupons yield coupon campaign ROI: `(Total Revenue Generated) / (Total Discount Amount Given)`.
5. **Timeline Breakdown**:
   - Grouping orders by `DATE(createdAt)` yields daily revenue, transaction volume, and unit volume for frontend trend graphing.

---

### 2.2 Inventory Velocity & Stock Telemetry (R2)
1. **SKU Velocity (Fast vs Slow Moving)**:
   - Aggregating `OrderItem.quantity` grouped by `product_id` across confirmed/delivered orders within the interval ranks top-selling products (Fast-Moving) and zero/low-selling products (Slow-Moving).
2. **Daily Run-Rate & Days of Inventory Remaining**:
   - `Daily Run Rate` = `Units Sold in Period / Number of Days in Period`.
   - `InventoryItem.available` provides current available warehouse stock.
   - `Days of Inventory Remaining` = `availableStock / dailyRunRate` (or `999` if run-rate is 0).
3. **Inventory Aging Categorization**:
   - `InventoryItem.updatedAt` or product active duration allows bucketing items into `<30 days`, `30–90 days`, and `>90 days` with unit counts and inventory valuation (`available * unitPrice`).
4. **Stockout Alerts & Replenishment Priority Scoring**:
   - From `InventoryItemRepository`:
     - Items with `available <= 0` are flagged `CRITICAL` (Score 90–100).
     - Items with `available <= 5` or `daysRemaining < 7` are flagged `HIGH` (Score 70–89).
     - Items with `daysRemaining between 7 and 21` are flagged `MEDIUM` (Score 40–69).
     - Items with `daysRemaining > 21` are flagged `LOW` (Score < 40).
   - Grouped by `warehouseCode` ("WH-01") and `binLocation` for warehouse fulfillment operations.

---

### 2.3 Customer Cohorts & Geographic Analytics (R3)
1. **Customer Lifetime Value (LTV)**:
   - Grouping orders by `user_id` calculates total completed order count and total spend per customer.
   - Segmenting customers into tiers:
     - **Platinum / High LTV**: Spend > ₹50,000
     - **Gold / Mid LTV**: Spend between ₹15,000 and ₹50,000
     - **Silver / Starter LTV**: Spend < ₹15,000
   - Overall Average LTV = `Total Net Revenue / Total Ordering Customers`.
2. **New vs Returning Cohort & Repeat Purchase Rate**:
   - Customers with exactly 1 completed order = New Customers.
   - Customers with ≥ 2 completed orders = Returning Customers.
   - `Repeat Purchase Rate` = `(Returning Customers / Total Ordering Customers) * 100%`.
   - Revenue contribution breakdown between new and repeat buyers.
3. **Regional Demand Breakdown**:
   - `Order.shippingAddress.state` and `city` provide regional data.
   - Grouping by `state` and `city` yields top buying states and top buying cities by order count and revenue.
4. **Cart Abandonment & Funnel Ratios**:
   - `Cart` and `CartItem` record cart activity.
   - Funnel stages:
     1. Total Carts Created / Active
     2. Carts with Items
     3. Checkout Initiated (`OrderStatus.PENDING`)
     4. Order Completed (`CONFIRMED`, `SHIPPED`, `DELIVERED`)
   - `Cart Abandonment Rate` = `(Active Carts with Items without Completed Order) / (Total Active Carts with Items) * 100%`.
   - `Conversion Rate` = `(Completed Orders) / (Total Active Carts) * 100%`.

---

### 2.4 Interactive Report Export (R4)
1. **Excel Export (`.xlsx`)**:
   - Apache POI `poi-ooxml 5.3.0` is already in `pom.xml`.
   - Using `XSSFWorkbook`, generate formatted sheets for Sales Telemetry and Inventory Velocity with styled headers (bold, colored fill), formatted currency/number cells, and autosized columns.
2. **CSV Export (`.csv`)**:
   - Stream formatted comma-separated values with `Content-Type: text/csv` and `Content-Disposition: attachment; filename=...`.

---

### 2.5 Access Control & Authorization Hardening (R5)
1. **Role Enforcement**:
   - Endpoint prefix `/api/admin/analytics/**` is covered by `SecurityConfig.java:74`:
     `.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")`
2. **Enforcing HTTP 403 with `"Not authorised to perform this action"`**:
   - For authenticated customers:
     - `SecurityConfig.accessDeniedHandler` already sets status 403 and writes `{"success":false,"message":"Not authorised to perform this action"}`.
   - For unauthenticated requests:
     - Currently, `authenticationEntryPoint` returns 401.
     - Update `authenticationEntryPoint` to verify if `request.getRequestURI().startsWith("/api/admin")`. If true, set status 403 and write `{"success":false,"message":"Not authorised to perform this action"}`.
   - For method security (`@PreAuthorize`):
     - Add `@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)` to `GlobalExceptionHandler.java` returning HTTP 403 with `ApiResponse.error("Not authorised to perform this action")`.

---

## 3. Caveats
1. **Legacy Order Schema Columns**:
   - Existing `Order` records in `sareekart_db_latest.sql` do not have standalone columns for `tax_amount`, `shipping_amount`, and `discount_amount`.
   - Because Hibernate is configured with `ddl-auto: update`, new columns can be added to `Order.java` (`taxAmount`, `shippingAmount`, `discountAmount`, `subtotalAmount`, `couponCode`) without schema migration failures, while backward-compatible calculations handle legacy records where those columns are null.
2. **Data Seeding in Clean/Test Environments**:
   - `DataSeeder.java` seeds Users, Categories, Products, Inventory Items, and Coupons, but seeds 0 Orders.
   - Seeding a realistic set of orders spanning the last 90 days when `orderRepository.count() == 0` ensures that the Admin Analytics UI and Playwright tests immediately display active KPI cards and graphs.

---

## 4. Conclusion & Recommended Architecture

### 4.1 New Components to Implement

```
backend/src/main/java/com/sareekart/
├── controller/
│   └── AnalyticsController.java           # Endpoints under /api/admin/analytics/**
├── service/
│   ├── AnalyticsService.java              # Analytics interface
│   └── impl/
│       └── AnalyticsServiceImpl.java      # Aggregation, velocity, cohorts, Excel/CSV generation
├── repository/
│   ├── OrderItemRepository.java           # NEW: Queries for product velocity & unit sales
│   └── OrderRepository.java               # ENHANCED: Custom JPQL date-range & aggregation queries
├── dto/response/analytics/
│   ├── AnalyticsOverviewResponse.java     # KPI cards with % comparison
│   ├── SalesTelemetryResponse.java        # R1: gross, net, tax, shipping, AOV, timeline, payments, coupon ROI
│   ├── InventoryVelocityResponse.java     # R2: fast/slow SKUs, run-rates, days remaining, aging, alerts
│   └── CustomerAnalyticsResponse.java     # R3: LTV tiers, new vs returning, top states/cities, funnel
```

### 4.2 API Endpoint Contract Specifications

| HTTP Method | Endpoint | Query Parameters | Role Authorization | Response Payload |
|---|---|---|---|---|
| `GET` | `/api/admin/analytics/overview` | `range` (TODAY, 7D, 30D, 90D, YTD, ALL, CUSTOM), `startDate`, `endDate` | `OWNER`, `MANAGER`, `ADMIN` | `ApiResponse<AnalyticsOverviewResponse>` (KPIs + period-over-period % delta) |
| `GET` | `/api/admin/analytics/sales` | `range`, `startDate`, `endDate` | `OWNER`, `MANAGER`, `ADMIN` | `ApiResponse<SalesTelemetryResponse>` (Financial telemetry, timeline, payment distribution, coupon ROI) |
| `GET` | `/api/admin/analytics/inventory` | None | `OWNER`, `MANAGER`, `ADMIN` | `ApiResponse<InventoryVelocityResponse>` (Fast/slow SKUs, run-rate, days remaining, aging, warehouse alerts) |
| `GET` | `/api/admin/analytics/customers` | `range`, `startDate`, `endDate` | `OWNER`, `MANAGER`, `ADMIN` | `ApiResponse<CustomerAnalyticsResponse>` (LTV distribution, new vs repeat, top cities/states, funnel) |
| `GET` | `/api/admin/analytics/export/sales` | `range`, `startDate`, `endDate`, `format` (`xlsx` or `csv`) | `OWNER`, `MANAGER`, `ADMIN` | Binary stream (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` or `text/csv`) |
| `GET` | `/api/admin/analytics/export/inventory` | `format` (`xlsx` or `csv`) | `OWNER`, `MANAGER`, `ADMIN` | Binary stream (`application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` or `text/csv`) |

### 4.3 Security Adjustments (R5)
- **`SecurityConfig.java`**:
  - In `authenticationEntryPoint`: Check `request.getRequestURI().startsWith("/api/admin")` -> return HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.
  - In `accessDeniedHandler`: Already returns HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.
- **`GlobalExceptionHandler.java`**:
  - Add explicit `@ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)` returning HTTP 403 with `"Not authorised to perform this action"`.

---

## 5. Verification Method

### 5.1 Backend Compilation & Unit Tests
Run the Maven test suite in the backend directory:
```bash
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw clean test
```
*Expected Result*: Zero compile errors, zero test failures, zero errors.

### 5.2 Unit & Integration Tests to Add
1. `AnalyticsServiceTest.java`:
   - Unit tests for date range parsing (`7D`, `30D`, `90D`, `YTD`, `CUSTOM`).
   - Unit tests for financial aggregation formulas (Gross Sales, Net Revenue, AOV, Tax, Shipping).
   - Unit tests for SKU velocity ranking and Days of Inventory Remaining calculations.
   - Unit tests for Customer LTV tiering and repeat purchase rate calculation.
2. `AnalyticsControllerTest.java` (MockMvc):
   - Verify `GET /api/admin/analytics/overview` returns 200 OK with populated telemetry for admin/manager/owner.
   - Verify unauthenticated request returns HTTP 403 Forbidden with `message: "Not authorised to perform this action"`.
   - Verify customer token returns HTTP 403 Forbidden with `message: "Not authorised to perform this action"`.
   - Verify Excel export endpoint returns 200 OK with content type `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` and attachment header.

### 5.3 Invalidation Conditions
- Any endpoint returning HTTP 401 instead of HTTP 403 for unauthenticated access to `/api/admin/analytics/**`.
- Any endpoint returning HTTP 500 when an unauthorized role accesses a method secured by `@PreAuthorize`.
- Division by zero errors when computing AOV or Days of Inventory Remaining with 0 sales or 0 orders.
- Date range parsing failures on invalid or leap year inputs.
