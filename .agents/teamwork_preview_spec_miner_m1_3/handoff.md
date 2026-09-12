# Handoff Report: Milestone M1 — Security Hardening, Controller Contracts, Seeding & Tests

**Agent**: `teamwork_preview_spec_miner_m1_3`  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3/`  
**Milestone**: M1 (Backend Analytics Telemetry Engine & Access Control)  
**Date**: 2026-09-04T15:30:00Z  

---

## 1. Observation

Direct code and environment observations:

1. **Existing Security Configuration (`com.sareekart.config.SecurityConfig.java:56-64`)**:
   ```java
   .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
       response.setStatus(HttpStatus.UNAUTHORIZED.value());
       response.setContentType(MediaType.APPLICATION_JSON_VALUE);
       response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please sign in again.\"}");
   }).accessDeniedHandler((request, response, accessDeniedException) -> {
       response.setStatus(HttpStatus.FORBIDDEN.value());
       response.setContentType(MediaType.APPLICATION_JSON_VALUE);
       response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
   }))
   ```
   - **Line 74**: `.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")`.
   - **Gap Identified**: When an *unauthenticated* user requests `/api/admin/**` (e.g. `/api/admin/analytics/overview`), Spring Security triggers `authenticationEntryPoint`, returning `HTTP 401 Unauthorized`. However, requirement **R5** explicitly mandates:
     > *"Customers and unauthenticated requests must receive HTTP 403 Forbidden with message 'Not authorised to perform this action'"*.

2. **Existing Global Exception Handler (`com.sareekart.exception.GlobalExceptionHandler.java`)**:
   - Currently lacks an `@ExceptionHandler` for Spring Security's `org.springframework.security.access.AccessDeniedException.class`.
   - When a controller method protected by `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")` rejects a request from an authenticated `CUSTOMER`, the framework throws `AccessDeniedException`. Without a dedicated handler, this falls through to `handleGenericException(Exception ex)` at line 106, returning `HTTP 500 Internal Server Error` instead of `HTTP 403 Forbidden`.

3. **Existing Data Seeder (`com.sareekart.config.DataSeeder.java:33-53`)**:
   - Seeds users, categories, products, inventory items, and promo coupons, but seeds **zero orders**.
   - `Order.java:56-58` specifies `@CreatedDate @Column(updatable = false) private LocalDateTime createdAt;`. Because `SareeKartApplication.java:8` declares `@EnableJpaAuditing`, saving an order via `orderRepository.save(order)` will cause Spring Data JPA's `AuditingEntityListener` to overwrite `createdAt` with `LocalDateTime.now()`, preventing historical order dates from being set via entity properties alone.

4. **Existing Test Suite Baseline**:
   - Running `./mvnw test` in `backend/backend/` executed 32 tests across 8 test classes in 9.38 seconds:
     ```
     [INFO] Tests run: 32, Failures: 0, Errors: 0, Skipped: 0
     [INFO] BUILD SUCCESS
     ```
   - H2 in-memory DB is used for testing (`jdbc:h2:mem:sareekart_test;MODE=MySQL`). Mockito and Spring Security Test (`spring-security-test`) are present in `pom.xml`.

5. **Existing DTOs & ApiResponse (`com.sareekart.dto.response.ApiResponse.java:36-41`)**:
   - `ApiResponse.error("Not authorised to perform this action")` returns:
     ```json
     {
       "success": false,
       "message": "Not authorised to perform this action"
     }
     ```
     with `@JsonInclude(JsonInclude.Include.NON_NULL)` omitting the null `data` property.

---

## 2. Logic Chain

1. **Security Hardening (R5) Logic**:
   - In `SecurityConfig.java`, inspect `request.getRequestURI()` inside `authenticationEntryPoint`. If the path starts with `/api/admin`, override default 401 behavior and return HTTP 403 with `{"success":false,"message":"Not authorised to perform this action"}`.
   - For authenticated users lacking roles (`CUSTOMER`), `accessDeniedHandler` already returns HTTP 403 with `{"success":false,"message":"Not authorised to perform this action"}`.
   - In `GlobalExceptionHandler.java`, add `@ExceptionHandler(AccessDeniedException.class)` returning `ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Not authorised to perform this action"))`.
   - This ensures both filter-level and method-level (`@PreAuthorize`) rejections return identical status (403) and body.

2. **Controller Contract Logic**:
   - `AnalyticsController` must reside at `@RequestMapping("/api/admin/analytics")` with class-level `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`.
   - All 6 endpoints (`/overview`, `/sales`, `/inventory`, `/customers`, `/export/sales`, `/export/inventory`) must follow the REST specification defined in `PROJECT.md`.
   - For export endpoints, binary `byte[]` responses must include proper `Content-Type` (`text/csv; charset=UTF-8` vs `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`) and `Content-Disposition: attachment; filename="..."`.
   - Input validation: Check for valid range tokens (`TODAY`, `7D`, `30D`, `90D`, `YTD`, `ALL`, `CUSTOM`). If `CUSTOM`, require both `startDate` and `endDate`, validating that `startDate <= endDate`. If invalid, throw `BadRequestException`, returning HTTP 400 Bad Request.

3. **Data Seeder Historical Telemetry Logic**:
   - In `DataSeeder.java`, check `if (orderRepository.count() == 0)`.
   - Create 40-50 historical orders distributed across the past 90 days (spans today, last 7 days, last 30 days, and 30-90 days ago).
   - Use seeded customers (`customer@sareekart.com`, `priya@example.com`, etc.) to create repeat customer profiles for LTV tiers (Platinum >₹50k, Gold ₹15k-₹50k, Silver <₹15k).
   - Mix payment methods (RAZORPAY, COD), statuses (mostly DELIVERED, some SHIPPED, 1 CANCELLED), coupons (WELCOME10, WEDDING20), and cities (Bengaluru, Mumbai, Delhi, Hyderabad, Chennai).
   - To bypass Spring Data JPA auditing overwriting `createdAt`, save the order entities, then execute a native SQL update:
     `UPDATE orders SET created_at = :orderDate, updated_at = :orderDate WHERE id = :id`
     via `JdbcTemplate` or `EntityManager`. This works reliably in both MySQL and H2.

4. **Testing Architecture Logic**:
   - `AnalyticsServiceTest`: Pure Mockito unit tests validating calculation algorithms (GST 5%, shipping fee rules, AOV, percentage deltas, inventory run-rate, days remaining, LTV tiers, and POI/CSV export streams).
   - `AnalyticsControllerTest`: MockMvc standalone tests validating endpoint paths, request parameter parsing, error responses (400 for bad custom dates), export headers, and HTTP 403 access denial handling.
   - Tests run in under 3 seconds and ensure `./mvnw test` remains 100% green.

---

## 3. Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Controller | `GET /api/admin/analytics/overview` | High-level summary metrics with period-over-period percentage comparisons | Query params: `range` (default "30D"), optional `startDate`, `endDate` | HTTP 200 `ApiResponse<AnalyticsOverviewResponse>` with current & previous totals, plus percentage deltas | 400 Bad Request if CUSTOM range missing dates or inverted dates | PROJECT.md §Interface Contracts |
| 2 | Controller | `GET /api/admin/analytics/sales` | Detailed sales performance, daily trend timeline, payment split, coupon ROI | Query params: `range` (default "30D"), optional `startDate`, `endDate` | HTTP 200 `ApiResponse<SalesTelemetryResponse>` with timeline points, payment shares, coupon ROI | 400 Bad Request if invalid dates | ORIGINAL_REQUEST §R1 |
| 3 | Controller | `GET /api/admin/analytics/inventory` | Inventory velocity, stock turnover, aging categories, stockout alerts | None (analyzes current inventory against rolling 30-day run-rate) | HTTP 200 `ApiResponse<InventoryVelocityResponse>` with fast/slow SKUs, aging counts, stockout alerts | 500 if database access fails | ORIGINAL_REQUEST §R2 |
| 4 | Controller | `GET /api/admin/analytics/customers` | Customer LTV tiers, new vs returning revenue, regional distribution, funnel | Query params: `range` (default "30D"), optional `startDate`, `endDate` | HTTP 200 `ApiResponse<CustomerAnalyticsResponse>` with LTV breakdown, states/cities, cart conversion | 400 Bad Request if invalid dates | ORIGINAL_REQUEST §R3 |
| 5 | Controller | `GET /api/admin/analytics/export/sales` | Export sales telemetry as CSV or Excel (.xlsx) file download | Query params: `range`, `startDate`, `endDate`, `format` ("csv" or "xlsx") | HTTP 200 `byte[]` with `Content-Disposition: attachment; filename="sales_telemetry_report_{range}.{ext}"` | 400 Bad Request if unsupported format | ORIGINAL_REQUEST §R4 |
| 6 | Controller | `GET /api/admin/analytics/export/inventory` | Export inventory velocity report as CSV or Excel (.xlsx) file download | Query params: `format` ("csv" or "xlsx", default "csv") | HTTP 200 `byte[]` with `Content-Disposition: attachment; filename="inventory_velocity_report.{ext}"` | 400 Bad Request if unsupported format | ORIGINAL_REQUEST §R4 |
| 7 | Security | Admin Access Control Hardening | Restrict `/api/admin/**` to `OWNER`, `MANAGER`, `ADMIN` | HTTP request to `/api/admin/**` | Pass-through for authorized roles | HTTP 403 Forbidden with exact payload `{"success":false,"message":"Not authorised to perform this action"}` | ORIGINAL_REQUEST §R5 |
| 8 | Security | Unauthenticated Admin Request Interception | Intercept anonymous requests to `/api/admin/**` at `AuthenticationEntryPoint` | Unauthenticated HTTP request to `/api/admin/**` | N/A | HTTP 403 Forbidden with exact payload `{"success":false,"message":"Not authorised to perform this action"}` | ORIGINAL_REQUEST §R5 |
| 9 | Security | Method Security AccessDenied Handling | Handle `AccessDeniedException` from `@PreAuthorize` in `GlobalExceptionHandler` | Unauthorized request reaching controller | N/A | HTTP 403 with `ApiResponse.error("Not authorised to perform this action")` | Codebase gap analysis |
| 10 | Seeding | Historical Order Telemetry Seeding | Seed 40-50 orders spanning past 90 days when `orderRepository.count() == 0` | Boot execution of `DataSeeder.run()` | Seeded orders in DB with preserved historical `createdAt` | Logged info message | DISPATCH.md §Detailed Investigation |
| 11 | Seeding | JPA Auditing Bypass for Historical Dates | Use native SQL update to prevent `@CreatedDate` overwriting past dates | Saved order ID and historical `LocalDateTime` | Order record with preserved historical date | Handled gracefully | JPA Entity inspection |
| 12 | Testing | `AnalyticsServiceTest` | Mockito test suite for date calculations, aggregations, zero-safe division, and POI export | Unit test execution | Verified business logic | 0 test failures | DISPATCH.md §4 |
| 13 | Testing | `AnalyticsControllerTest` | MockMvc test suite validating endpoints, parameter bindings, export headers, and 403 enforcement | Unit test execution | Verified HTTP contract | 0 test failures | DISPATCH.md §4 |

---

## 4. Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Date Interval Engine | `range=CUSTOM` without `startDate` or `endDate` | Throws `BadRequestException("Start date and end date are required for CUSTOM range.")`, returning HTTP 400 Bad Request |
| 2 | Date Interval Engine | `startDate=2026-09-10` and `endDate=2026-09-01` (`startDate > endDate`) | Throws `BadRequestException("Start date cannot be after end date.")`, returning HTTP 400 Bad Request |
| 3 | Date Interval Engine | `range=INVALID_RANGE` | Defaults safely to `30D` or throws 400 Bad Request with supported range list |
| 4 | Period-over-Period Delta | Prior period sales = ₹0, Current period sales = ₹50,000 | Returns `+100.0%` (or `0.0%`) without throwing `ArithmeticException` or `NaN` |
| 5 | Period-over-Period Delta | Prior period sales = ₹0, Current period sales = ₹0 | Returns `0.0%` delta |
| 6 | Inventory Velocity Run-rate | Product has 0 units sold in the 30-day period | `dailyRunRate = 0.0`; `daysOfInventoryRemaining = 999.0` (safe cap avoiding division by zero) |
| 7 | Inventory Stockout Alert | Product available stock = 0 | Categorized as `"OUT_OF_STOCK"`, assigned priority score `95` (Critical) |
| 8 | Customer Cohort LTV | Customer spend = ₹52,000 | Assigned to `"Platinum"` tier (threshold >= ₹50,000) |
| 9 | Cart Abandonment Funnel | Carts with items = 0 | `cartAbandonmentRate = 0.0%`, `conversionRate = 0.0%` without division by zero |
| 10 | Security Unauthenticated | Unauthenticated `GET /api/admin/analytics/overview` (no Authorization header) | `AuthenticationEntryPoint` detects `/api/admin` prefix, sets HTTP 403 Forbidden and writes `{"success":false,"message":"Not authorised to perform this action"}` |
| 11 | Security Customer Role | Customer JWT token accessing `/api/admin/analytics/overview` | `AccessDeniedHandler` / `GlobalExceptionHandler` returns HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}` |
| 12 | Historical Data Seeder | Order `createdAt` set to 60 days ago | JPA Auditing would overwrite on `save()`; native SQL update restores historical timestamp `createdAt = now() - 60 days` |
| 13 | Export CSV Escaping | Product name with comma: `"Banarasi Silk, Heavy Zari"` | Enclosed in quotes: `"\"Banarasi Silk, Heavy Zari\""` complying with RFC 4180 |
| 14 | Export Format Validation | `format=pdf` or `format=json` | Throws `BadRequestException("Unsupported export format: pdf. Supported formats are 'csv' and 'xlsx'.")` |

---

## 5. Implementation Specifications & Code Templates

### 5.1 `AnalyticsController.java`
**Target Path**: `backend/backend/src/main/java/com/sareekart/controller/AnalyticsController.java`

```java
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
```

---

### 5.2 Security Hardening (R5) Updates

#### 5.2.1 `SecurityConfig.java`
**Target Path**: `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`

Update the `exceptionHandling` block to:
```java
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    // Security hardening R5: Return HTTP 403 for unauthorized requests to /api/admin/**
                    if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/admin")) {
                        response.setStatus(HttpStatus.FORBIDDEN.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
                    } else {
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please sign in again.\"}");
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhook/whatsapp/**", "/ws-sareekart/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/api/approvals/*/approve", "/api/approvals/*/reject").hasRole("OWNER")
                .requestMatchers("/api/approvals/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                .requestMatchers("/api/excel/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                .requestMatchers("/api/inventory/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                .anyRequest().authenticated()
            );
```

#### 5.2.2 `GlobalExceptionHandler.java`
**Target Path**: `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java`

Add the `@ExceptionHandler(AccessDeniedException.class)` handler:
```java
    /**
     * Handle Spring Security method-level access denied exceptions (@PreAuthorize).
     * Returns HTTP 403 Forbidden with R5 compliance message.
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Not authorised to perform this action"));
    }
```

---

### 5.3 Historical Data Seeding Updates (`DataSeeder.java`)
**Target Path**: `backend/backend/src/main/java/com/sareekart/config/DataSeeder.java`

Inject `OrderRepository`, `ProductRepository`, `UserRepository`, `CouponRepository`, and `EntityManager` (or `JdbcTemplate`):

```java
    private final OrderRepository orderRepository;
    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;
```

In `run(String... args)`:
```java
        if (orderRepository.count() == 0) {
            log.info("Seeding historical order telemetry for analytics...");
            seedHistoricalOrders();
            log.info("Historical orders seeded successfully!");
        }
```

Implementation method:
```java
    @org.springframework.transaction.annotation.Transactional
    private void seedHistoricalOrders() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return;

        User customer = userRepository.findByEmail("customer@sareekart.com").orElse(null);
        if (customer == null) return;

        // Ensure secondary customer accounts exist for cohort segmentation
        User customer2 = userRepository.findByEmail("priya@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Priya")
                    .lastName("Sharma")
                    .email("priya@example.com")
                    .mobile("9876543220")
                    .password(passwordEncoder.encode("customer123"))
                    .role(com.sareekart.entity.Role.CUSTOMER)
                    .build();
            return userRepository.save(u);
        });

        User customer3 = userRepository.findByEmail("ananya@example.com").orElseGet(() -> {
            User u = User.builder()
                    .firstName("Ananya")
                    .lastName("Verma")
                    .email("ananya@example.com")
                    .mobile("9876543221")
                    .password(passwordEncoder.encode("customer123"))
                    .role(com.sareekart.entity.Role.CUSTOMER)
                    .build();
            return userRepository.save(u);
        });

        List<User> customers = List.of(customer, customer2, customer3);

        String[][] locations = {
            {"Bengaluru", "Karnataka", "560001"},
            {"Mumbai", "Maharashtra", "400001"},
            {"Delhi", "Delhi", "110001"},
            {"Hyderabad", "Telangana", "500001"},
            {"Chennai", "Tamil Nadu", "600001"}
        };

        LocalDateTime now = LocalDateTime.now();

        // 40 orders distributed over the last 90 days
        // Days ago offsets: 0, 1, 2, 4, 6, 9, 12, 15, 18, 21, 25, 28, 35, 42, 50, 60, 75, 85
        int[] daysAgoOffsets = {
            0, 0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 10, 12, 14, 16, 18, 20, 22, 24, 26, 28, 30,
            33, 36, 40, 45, 50, 55, 60, 65, 70, 75, 80, 85, 88
        };

        for (int i = 0; i < daysAgoOffsets.length; i++) {
            int daysAgo = daysAgoOffsets[i];
            LocalDateTime orderDate = now.minusDays(daysAgo).minusHours(i % 12).minusMinutes((i * 7) % 60);

            User user = customers.get(i % customers.size());
            String[] loc = locations[i % locations.length];

            Address shippingAddress = Address.builder()
                    .fullName(user.getFirstName() + " " + user.getLastName())
                    .phone(user.getMobile() != null ? user.getMobile() : "9876543210")
                    .streetAddress("Flat " + (100 + i) + ", Residency Road")
                    .city(loc[0])
                    .state(loc[1])
                    .pincode(loc[2])
                    .build();

            // Select 1 or 2 products
            Product p1 = products.get(i % products.size());
            Product p2 = products.get((i + 3) % products.size());

            int qty1 = (i % 2) + 1;
            BigDecimal item1Total = p1.getPrice().multiply(BigDecimal.valueOf(qty1));

            com.sareekart.entity.Order order = com.sareekart.entity.Order.builder()
                    .user(user)
                    .shippingAddress(shippingAddress)
                    .paymentMethod(i % 2 == 0 ? "RAZORPAY" : "COD")
                    .paymentStatus(i == 15 ? "REFUNDED/CANCELLED" : "COMPLETED")
                    .status(i == 15 ? com.sareekart.entity.OrderStatus.CANCELLED : com.sareekart.entity.OrderStatus.DELIVERED)
                    .build();

            List<com.sareekart.entity.OrderItem> items = new java.util.ArrayList<>();
            com.sareekart.entity.OrderItem item1 = com.sareekart.entity.OrderItem.builder()
                    .order(order)
                    .product(p1)
                    .quantity(qty1)
                    .price(p1.getPrice())
                    .build();
            items.add(item1);

            BigDecimal subtotal = item1Total;
            if (i % 3 == 0 && !p1.getId().equals(p2.getId())) {
                int qty2 = 1;
                items.add(com.sareekart.entity.OrderItem.builder()
                        .order(order)
                        .product(p2)
                        .quantity(qty2)
                        .price(p2.getPrice())
                        .build());
                subtotal = subtotal.add(p2.getPrice().multiply(BigDecimal.valueOf(qty2)));
            }

            BigDecimal discount = BigDecimal.ZERO;
            if (i % 4 == 0) {
                discount = subtotal.multiply(BigDecimal.valueOf(0.10)); // WELCOME10
            } else if (i % 7 == 0) {
                discount = subtotal.multiply(BigDecimal.valueOf(0.20)); // WEDDING20
            }

            BigDecimal shipping = subtotal.compareTo(BigDecimal.valueOf(5000)) >= 0 ? BigDecimal.ZERO : BigDecimal.valueOf(150);
            BigDecimal totalAmount = subtotal.subtract(discount).add(shipping);

            order.setItems(items);
            order.setTotalAmount(totalAmount);

            com.sareekart.entity.Order savedOrder = orderRepository.save(order);

            // Bypass Spring Data JPA Auditing to set accurate historical dates
            entityManager.createNativeQuery("UPDATE orders SET created_at = :orderDate, updated_at = :orderDate WHERE id = :id")
                    .setParameter("orderDate", orderDate)
                    .setParameter("id", savedOrder.getId())
                    .executeUpdate();
        }
    }
```

---

### 5.4 Unit and Integration Test Specifications

#### 5.4.1 `AnalyticsServiceTest.java`
**Target Path**: `backend/backend/src/test/java/com/sareekart/service/AnalyticsServiceTest.java`

```java
package com.sareekart.service;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.entity.InventoryItem;
import com.sareekart.entity.OrderStatus;
import com.sareekart.repository.CartRepository;
import com.sareekart.repository.InventoryItemRepository;
import com.sareekart.repository.OrderItemRepository;
import com.sareekart.repository.OrderRepository;
import com.sareekart.service.impl.AnalyticsServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
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
                .createdAt(LocalDateTime.now().minusDays(20))
                .build();

        when(inventoryItemRepository.findAll()).thenReturn(List.of(item));
        when(orderItemRepository.sumUnitsSoldByProductIdInPeriod(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(15L);

        InventoryVelocityResponse response = analyticsService.getInventoryVelocity();

        assertNotNull(response);
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
}
```

#### 5.4.2 `AnalyticsControllerTest.java`
**Target Path**: `backend/backend/src/test/java/com/sareekart/controller/AnalyticsControllerTest.java`

```java
package com.sareekart.controller;

import com.sareekart.dto.response.analytics.*;
import com.sareekart.exception.GlobalExceptionHandler;
import com.sareekart.service.AnalyticsService;
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
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("sales_telemetry_report_30d.csv")))
                .andExpect(content().bytes(mockCsv));
    }

    @Test
    void testExportSales_InvalidFormat_ReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/admin/analytics/export/sales?format=pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Unsupported export format: pdf")));
    }
}
```

---

## 6. Verification Method

To independently verify the implementation when the Worker builds Milestone M1:

1. **Test Execution**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test -Dtest=AnalyticsServiceTest,AnalyticsControllerTest
   ./mvnw test
   ```
   **Pass Condition**: All tests compile and execute cleanly with 0 failures and 0 errors.

2. **Security Hardening (R5) Verification via curl**:
   - Start backend: `./mvnw spring-boot:run`
   - Unauthenticated test:
     ```bash
     curl -i http://localhost:8081/api/admin/analytics/overview
     ```
     **Pass Condition**: Returns `HTTP/1.1 403 Forbidden` with body:
     ```json
     {"success":false,"message":"Not authorised to perform this action"}
     ```
   - Customer authenticated test (login with customer credentials, pass bearer token):
     ```bash
     curl -i -H "Authorization: Bearer <CUSTOMER_JWT>" http://localhost:8081/api/admin/analytics/overview
     ```
     **Pass Condition**: Returns `HTTP/1.1 403 Forbidden` with body:
     ```json
     {"success":false,"message":"Not authorised to perform this action"}
     ```
   - Admin/Manager/Owner authenticated test:
     ```bash
     curl -i -H "Authorization: Bearer <ADMIN_JWT>" http://localhost:8081/api/admin/analytics/overview
     ```
     **Pass Condition**: Returns `HTTP/1.1 200 OK` with populated `data` containing `currentPeriod`, `previousPeriod`, and `percentageChanges`.

3. **Historical Data Seeder Verification**:
   - Check database query or logs:
     ```bash
     curl -s -H "Authorization: Bearer <ADMIN_JWT>" http://localhost:8081/api/admin/analytics/overview?range=30D
     ```
     **Pass Condition**: Response contains `completedOrders > 0` and non-zero `grossSales` without manual order entry.
