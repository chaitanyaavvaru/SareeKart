# Backend Domain Survey Report: SareeKart Operations & Customer Engagement Suite

## 1. Observation

### 1.1 Backend Directory Structure & Build System
- **Maven Project Location**: The active backend project resides in `backend/backend/` (not the root directory `backend/` or `SareeKart/backend/backend`).
  - `backend/backend/pom.xml` defines Spring Boot `3.5.15` and Java `17` (lines 8, 30).
  - Maven wrapper `./mvnw` is present in `backend/backend/mvnw`.
  - Server runs on port `8081` (`backend/backend/src/main/resources/application.yaml`, line 27).
  - Runtime database is local MySQL on port `3306` (`sareekart_db`, user `root`, pass `root123`), with Hibernate `ddl-auto: update` (lines 6–13).
  - Test profile uses H2 in-memory database (`backend/backend/src/test/resources/application-test.yaml`) with `jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE` and Hibernate `ddl-auto: create-drop` (lines 3, 10).

### 1.2 Flyway & Database Schema Management
- Grep search for `flyway` across the entire repository returned **0 matches**.
- `backend/backend/pom.xml` contains no Flyway or Liquibase dependency or plugin.
- `backend/backend/src/main/resources/` contains only `application.yaml` (no `db/migration` directory).
- Database schema updates are managed dynamically by Hibernate JPA (`ddl-auto: update` in production/runtime, `create-drop` in test) and initial entities/data are seeded programmatically via `DataSeeder.java` (`backend/backend/src/main/java/com/sareekart/config/DataSeeder.java`).

### 1.3 Existing Order & Tracking Architecture
- `backend/backend/src/main/java/com/sareekart/entity/Order.java`:
  - Contains tracking fields:
    ```java
    56:     private String trackingNumber;
    58:     private String courierPartner;
    60:     private String currentLocation;
    62:     private String estimatedDeliveryDate;
    ```
- `backend/backend/src/main/java/com/sareekart/entity/OrderStatus.java`:
  - Currently contains only 5 status values:
    ```java
    3: public enum OrderStatus {
    4:     PENDING,
    5:     CONFIRMED,
    6:     SHIPPED,
    7:     DELIVERED,
    8:     CANCELLED
    9: }
    ```
  - **Missing**: `PACKED` and `OUT_FOR_DELIVERY` required for full 5-stage milestone progression (`Ordered`, `Packed`, `Shipped`, `Out for Delivery`, `Delivered`).
- `backend/backend/src/main/java/com/sareekart/controller/OrderController.java`:
  - Already exposes `GET /api/orders/track` (lines 71–78) for public order tracking by `orderId` or `trackingNumber`.
  - Exposes `PUT /api/admin/orders/{id}/tracking` (lines 80–89) for manual tracking field updates.
  - Exposes `PUT /api/admin/orders/{id}/status` (lines 63–69) to transition order status.
- `backend/backend/src/main/java/com/sareekart/service/impl/OrderServiceImpl.java`:
  - In `createOrder` (lines 92–106), defaults `trackingNumber` to `"SK-BD-" + randomAwb`, `courierPartner` to `"BlueDart Express"`, and `currentLocation` to `"Kanchipuram Artisan Guild - Atelier Central Hub"`.
  - Invokes `notificationService.sendOrderPlacedNotification(savedOrder)` (line 133).
  - In `updateOrderStatus` (line 187), invokes `notificationService.sendOrderStatusUpdateNotification(updatedOrder)`.

### 1.4 Notification Infrastructure
- `backend/backend/src/main/java/com/sareekart/service/OrderNotificationService.java`:
  - Currently only delegates to `WhatsAppApiClient.sendTextMessage` with mock WhatsApp API payloads (lines 16–34).
  - **No `Notification` entity** exists anywhere in `backend/backend/src/main/java/com/sareekart/entity/`.
  - **No in-app notification persistence, badge counter, mark-as-read, or REST controller** exists.
  - **No simulated event audit log** recording delivery payloads for in-app, simulated SMS, and simulated email.

### 1.5 Maker-Checker & Inventory Architecture
- `backend/backend/src/main/java/com/sareekart/entity/ApprovalRequest.java`:
  - Implements the baseline Maker-Checker entity (lines 21–23: `STATUS_PENDING = "Pending owner approval"`, `STATUS_APPROVED = "APPROVED"`, `STATUS_REJECTED = "REJECTED"`).
  - Fields: `entityType`, `targetEntityId`, `action`, `previousValue`, `requestedValue`, `reason`, `status`, `requestedByUserId`, `requestedByEmail`, `reviewedByUserId`, `reviewedByEmail`, `reviewNote`, `version`.
- `backend/backend/src/main/java/com/sareekart/service/ApprovalService.java`:
  - Handles Maker-Checker submissions (`submitRequest`) and reviews (`approveRequest`, `rejectRequest`).
  - Supports `PRODUCT_PRICE`, `INVENTORY_STOCK`, `COUPON_DELETE`, `COUPON_CREATE` (lines 78–121).
  - Enforces checker permission: `owner.getRole() != Role.OWNER && owner.getRole() != Role.ADMIN` throws `SecurityException("Not authorised to perform this action")` (lines 65–67).
- `backend/backend/src/main/java/com/sareekart/controller/ApprovalController.java`:
  - Restricts approve/reject endpoints to `hasRole('OWNER')` (lines 77, 97).
- `backend/backend/src/main/java/com/sareekart/entity/InventoryItem.java`:
  - Has `sku` marked `@Column(nullable = false, unique = true)` (line 25).
  - Has `warehouseCode` defaulting to `"WH-01"` and `warehouseName` defaulting to `"Bengaluru Central Fulfillment Hub"` (lines 37, 41).
  - Does NOT currently model multi-warehouse stock balances for `WH-02 Mumbai West` or `WH-03 Delhi North`.
  - **No `StockTransfer` entity** exists.

### 1.6 Reviews & Moderation Architecture
- `backend/backend/src/main/java/com/sareekart/entity/Review.java`:
  - Only contains: `id`, `productId`, `userName`, `rating` (1–5), `comment`, `createdAt` (lines 18–32).
  - **Missing**:
    - `userId` (Long) to bind the review to a customer account.
    - `status` (String/Enum: `PENDING`, `APPROVED`, `REJECTED`, `FEATURED`).
    - `verifiedBuyer` (Boolean) to flag verified purchase testimonials.
    - Moderation fields (`moderatedBy`, `moderatedAt`, `moderationNote`).
- `backend/backend/src/main/java/com/sareekart/controller/ReviewController.java`:
  - `GET /api/products/{productId}/reviews` fetches all reviews directly without filtering by moderation approval (line 29).
  - `POST /api/products/{productId}/reviews` accepts a review but does NOT verify if the customer purchased the saree and does NOT set a moderation status (lines 37–59).
  - **No admin review moderation endpoints** exist (`GET /api/admin/reviews`, `PUT /api/admin/reviews/{id}/status`).

### 1.7 Artisan Heritage Architecture
- Grep search for `Artisan` in `backend/backend` returned 0 entity/repository/controller matches (only a prompt string in `AIChatbotService.java` and a default string in `OrderServiceImpl.java`).
- `frontend/src/pages/Artisans/ArtisansPage.jsx` contains client-side static mock clusters (`kanchi`, `banarasi`, `paithani`, `uppada`).
- **No `Artisan` entity, repository, service, or controller** exists in the backend.
- Catalog sarees in `Product.java` have no relational or foreign key link to artisan profiles.

### 1.8 Access Control & Security Architecture
- `backend/backend/src/main/java/com/sareekart/entity/Role.java`:
  - Contains: `CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN` (lines 3–8).
- `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`:
  - Lines 56–70 define `authenticationEntryPoint` and `accessDeniedHandler`:
    ```java
    56:             .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, authException) -> {
    57:                 if (request.getRequestURI() != null && request.getRequestURI().startsWith("/api/admin")) {
    58:                     response.setStatus(HttpStatus.FORBIDDEN.value());
    59:                     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    60:                     response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
    61:                 } else {
    62:                     response.setStatus(HttpStatus.UNAUTHORIZED.value());
    63:                     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    64:                     response.getWriter().write("{\"success\":false,\"message\":\"Authentication required. Please sign in again.\"}");
    65:                 }
    66:             }).accessDeniedHandler((request, response, accessDeniedException) -> {
    67:                 response.setStatus(HttpStatus.FORBIDDEN.value());
    68:                 response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    69:                 response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
    70:             }))
    ```
  - Line 80: `.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")`.
  - Line 74: `.requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/orders/track/**").permitAll()`.
- `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java`:
  - Lines 106–111 handle Spring Security `AccessDeniedException`:
    ```java
    106:     @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    107:     public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
    108:         return ResponseEntity
    109:                 .status(HttpStatus.FORBIDDEN)
    110:                 .body(ApiResponse.error("Not authorised to perform this action"));
    111:     }
    ```

### 1.9 Existing Backend Test Suite Execution
- Running `./mvnw test` inside `backend/backend`:
  - Command: `./mvnw test`
  - Result: **Tests run: 48, Failures: 0, Errors: 0, Skipped: 0. Total time: 11.108 s**.
  - All existing unit, integration, and security tests pass with 100% success rate.

---

## 2. Logic Chain

### 2.1 Database Migration Strategy
- **Observation 1.2**: Flyway is not present in POM and no migration scripts exist; runtime uses `spring.jpa.hibernate.ddl-auto: update`, and testing uses H2 with `create-drop`.
- **Reasoning**: Introducing new entities (`Notification`, `NotificationDispatchLog`, `StockTransfer`, `WarehouseStock`, `Artisan`) and adding columns to existing entities (`Review`, `OrderStatus`) will be automatically synchronized by Hibernate at runtime and test time without requiring Flyway migration scripts.
- **Deduction**: Data seeding in `DataSeeder.java` should be extended to populate initial artisan heritage profiles, regional warehouse stock balances, approved/pending reviews, and sample notifications on startup.

### 2.2 R1 (Notifications & Dispatch Telemetry) Logic
- **Observation 1.4**: Only WhatsApp mock messaging exists in `OrderNotificationService`; no in-app notification model or simulated dispatch audit log exists.
- **Reasoning**:
  1. A `Notification` entity must be created with `userId` (null for staff broadcasts), `recipientRole` (`CUSTOMER` or `STAFF`), `title`, `message`, `type` (`ORDER_PLACED`, `ORDER_DISPATCHED`, `ORDER_DELIVERED`, `LOW_STOCK_ALERT`, `APPROVAL_PENDING`), `referenceId`, `link`, `isRead` (boolean), `createdAt`.
  2. A `NotificationDispatchLog` entity must record simulated external payloads (channel `IN_APP`, `SIMULATED_SMS`, `SIMULATED_EMAIL`, destination, payload body, dispatchedAt) to satisfy §R1 without external telecom dependencies.
  3. A `NotificationService` will centralize triggering from business events:
     - `OrderServiceImpl.createOrder` -> notify customer order placed & notify staff new order.
     - `OrderServiceImpl.updateOrderStatus` / dispatch -> notify customer shipment dispatched with courier partner and tracking AWB, or order delivered.
     - `InventoryItem.recalculateStatus` or stock deduction -> notify staff low-stock alert when available <= 5.
     - `ApprovalService.submitRequest` -> notify Owner/Manager pending approval request.
  4. A `NotificationController` at `/api/notifications` must expose:
     - `GET /api/notifications` (user/staff notifications)
     - `GET /api/notifications/unread-count` (navbar bell counter)
     - `PUT /api/notifications/{id}/read` (mark single)
     - `PUT /api/notifications/read-all` (mark all)
     - `GET /api/admin/notifications/dispatch-log` (staff audit log)

### 2.3 R2 (Multi-Warehouse Stock Transfers & Carrier Logistics) Logic
- **Observation 1.3 & 1.5**: `OrderStatus` lacks `PACKED` and `OUT_FOR_DELIVERY`; `ApprovalService` already has Maker-Checker logic for `INVENTORY_STOCK`; `InventoryItem` is currently single-warehouse (`WH-01`).
- **Reasoning**:
  1. `OrderStatus` enum should add `PACKED` and `OUT_FOR_DELIVERY` to support the full 5-stage milestone progression (`Ordered` -> `Packed` -> `Shipped` -> `Out for Delivery` -> `Delivered`).
  2. Model regional warehouses: `WH-01` (Bengaluru Central), `WH-02` (Mumbai West), `WH-03` (Delhi North).
  3. Implement `WarehouseStock` entity (or multi-warehouse table) holding stock per SKU per warehouse hub:
     - Hub balances initialized in `DataSeeder`.
  4. Implement `StockTransfer` entity:
     - `transferNumber` (e.g. `TRF-1001`), `sku`, `productName`, `sourceWarehouseCode`, `targetWarehouseCode`, `quantity`, `reason`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `COMPLETED`), `requestedByUserId`, `approvedByUserId`.
  5. Integrate with Maker-Checker protocol:
     - Manager submits stock transfer request -> status `PENDING` -> creates `ApprovalRequest(entityType="STOCK_TRANSFER", ...)`.
     - Owner approves in Approval Center -> approves `ApprovalRequest` -> moves stock: deducts from `sourceWarehouse`, adds to `targetWarehouse`, marks transfer `APPROVED`/`COMPLETED`.
     - Owner direct transfer -> executes immediately without waiting for approval.
  6. Carrier Logistics & Dispatch:
     - Admin order dispatch endpoint: `POST /api/admin/orders/{id}/fulfill` (or `PUT /api/admin/orders/{id}/tracking`) accepts courier partner (`Blue Dart`, `Delhivery`, `DTDC`, `India Post`), generates automated AWB (`SK-BD-...`, `SK-DL-...`, etc.), sets milestone to `SHIPPED` (or `PACKED`), sets origin warehouse location, and fires dispatch notification.

### 2.4 R3 (Verified Customer Reviews & Moderation Console) Logic
- **Observation 1.6**: `Review` entity only has `id`, `productId`, `userName`, `rating`, `comment`, `createdAt`; `ReviewController` lacks moderation, user association, and verified buyer checks.
- **Reasoning**:
  1. Extend `Review` entity with:
     - `userId` (Long)
     - `userEmail` (String)
     - `status` (String/Enum: `PENDING`, `APPROVED`, `REJECTED`, `FEATURED`, default `PENDING`)
     - `isVerifiedBuyer` (Boolean, default false)
     - `moderatedBy` (String)
     - `moderationNote` (String)
     - `updatedAt` (LocalDateTime)
  2. Verified Buyer Verification:
     - When review is submitted by authenticated customer (`@AuthenticationPrincipal User user`), check if user has placed an order containing the product:
     - Query: `OrderRepository.hasUserPurchasedProduct(userId, productId)`.
     - If true, mark `isVerifiedBuyer = true`; if false, `isVerifiedBuyer = false` (general registered customers can still contribute feedback).
  3. Public Product Reviews API:
     - `GET /api/products/{productId}/reviews`: return only `APPROVED` and `FEATURED` reviews.
     - Provide aggregate review analytics: `averageRating`, `totalReviews`, `ratingDistribution` (counts for 1 to 5 stars).
  4. Admin Review Moderation Console:
     - `GET /api/admin/reviews?status={PENDING|APPROVED|REJECTED|FEATURED|ALL}`: accessible to `OWNER`, `MANAGER`, `ADMIN`.
     - `PUT /api/admin/reviews/{id}/status`: update review status (`APPROVED`, `REJECTED`, `FEATURED`).

### 2.5 R4 (Artisan Heritage Storytelling Showcase) Logic
- **Observation 1.7**: No backend entity or API exists for artisans; frontend currently hardcodes static items.
- **Reasoning**:
  1. Create `Artisan` entity:
     - `id`, `slug` (unique), `name`, `leadArtisan`, `region`, `giTag`, `technique`, `heritage`, `bio`, `cooperativeName`, `activeLooms`, `imageUrl`.
  2. Traditional loom techniques from R4:
     - Kanchipuram Silk Guild (Kanchipuram, Tamil Nadu)
     - Varanasi Zari Cooperative (Varanasi, UP)
     - Patola Silk Double Ikat Guild (Patan, Gujarat)
     - Yeola Paithani Collective (Yeola, Maharashtra)
     - Chanderi Heritage Weavers (Chanderi, MP)
  3. Association with Catalog Products:
     - In `Product.java`: add `artisanSlug` or `artisanId` (or map by category/fabric), linking each handcrafted saree to its artisan creator.
  4. Public API:
     - `GET /api/artisans`: list all artisan heritage profiles.
     - `GET /api/artisans/{idOrSlug}`: detailed biography and cooperative info.
     - `GET /api/artisans/{idOrSlug}/products`: return catalog products produced by this artisan.
  5. SecurityConfig:
     - Permit all `GET /api/artisans/**`.

### 2.6 R5 (Access Control & RBAC) Logic
- **Observation 1.8**:
  - `SecurityConfig` already converts unauthenticated requests to `/api/admin/**` into 403 Forbidden with exact body: `{"success":false,"message":"Not authorised to perform this action"}`.
  - Spring Security `AccessDeniedHandler` and `GlobalExceptionHandler` also return 403 Forbidden with exact body: `{"success":false,"message":"Not authorised to perform this action"}`.
- **Reasoning**:
  - By placing all staff moderation and logistics endpoints under `/api/admin/**`:
    - `/api/admin/reviews/**` (Review Moderation)
    - `/api/admin/transfers/**` or `/api/admin/warehouses/**` (Warehouse Logistics)
    - `/api/admin/orders/**` (Courier fulfillment)
    - `/api/admin/notifications/dispatch-log` (Simulated dispatch audit log)
  - Unauthenticated requests and unauthorized customer requests are guaranteed to receive HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.
  - Public endpoints (`GET /api/products/**/reviews`, `GET /api/artisans/**`, `GET /api/orders/track/**`) are accessible without authentication.
  - Customer endpoints (`GET /api/notifications`, `PUT /api/notifications/**`, `POST /api/products/{productId}/reviews`) require authenticated user.

---

## 3. Caveats

1. **Absence of Flyway**:
   - The project does not use Flyway migration scripts. Schema evolution relies on Hibernate JPA's `ddl-auto: update` (MySQL) and `create-drop` (H2). While this is convenient and standard in the existing codebase, developers must not introduce conflicting Flyway dependencies that could alter this workflow.
2. **Unique SKU Constraint on `inventory_items`**:
   - In `InventoryItem.java`, `sku` has `unique = true`. Therefore, multi-warehouse stock balances should be stored in a dedicated `WarehouseStock` entity (with composite unique key on `warehouseCode` + `sku`), or `InventoryItem` can serve as the global aggregate while `WarehouseStock` holds per-hub balances (`WH-01`, `WH-02`, `WH-03`).
3. **DataSeeder In-Memory Dependency**:
   - The test suite boots Spring Boot with H2 and executes `DataSeeder.java`. When adding seed data for artisans, warehouses, and reviews, ensure mock references (categories, products) are resolved safely without foreign key conflicts in H2.
4. **WebSocket Real-Time Notifications**:
   - While Spring WebSocket dependency exists in `pom.xml`, the acceptance criteria specifically test the REST-based navbar notification bell widget (`/api/notifications`), active unread counter (`/api/notifications/unread-count`), and polling/event refresh. Supporting REST endpoints ensures complete offline determinism.

---

## 4. Conclusion & Actionable Blueprint

The backend architecture is clean, highly modular, and well-positioned for the Operations & Customer Engagement Suite. The delta required across all 5 requirements is scoped, concrete, and directly aligns with existing patterns:

| Req | Domain Area | New / Modified Backend Components | Actionable Scope |
|---|---|---|---|
| **R1** | Notifications | `Notification.java`, `NotificationDispatchLog.java`, `NotificationRepository.java`, `NotificationService.java`, `NotificationServiceImpl.java`, `NotificationController.java` | In-app notification center, unread counter, mark-as-read, simulated dispatch audit log (SMS/Email/InApp), triggered by order/dispatch/stock/approval events. |
| **R2** | Multi-Warehouse & Carrier Logistics | `OrderStatus.java` (add `PACKED`, `OUT_FOR_DELIVERY`), `WarehouseStock.java`, `StockTransfer.java`, `StockTransferRepository.java`, `StockTransferService.java`, `StockTransferController.java`, update `ApprovalService.java` | Hubs `WH-01`, `WH-02`, `WH-03`; Maker-Checker transfer workflow (Manager submits, Owner approves/rejects, Owner instant transfer); courier partner assignment (Blue Dart, Delhivery, DTDC, India Post) and AWB generation. |
| **R3** | Reviews & Moderation | Update `Review.java`, `ReviewRepository.java`, `ReviewService.java`, `ReviewServiceImpl.java`, `ReviewController.java`, `AdminReviewController.java` | Add `userId`, `status` (`PENDING`, `APPROVED`, `REJECTED`, `FEATURED`), `isVerifiedBuyer`; check order history for verified buyer badge; public approved reviews with rating distribution; `/api/admin/reviews` moderation console. |
| **R4** | Artisan Showcase | `Artisan.java`, `ArtisanRepository.java`, `ArtisanService.java`, `ArtisanServiceImpl.java`, `ArtisanController.java`, update `Product.java` & `DataSeeder.java` | 5 traditional artisan heritage profiles (Kanchipuram, Banarasi, Patola, Paithani, Chanderi) linked to catalog sarees; public endpoints `GET /api/artisans` and `GET /api/artisans/{slug}/products`. |
| **R5** | RBAC & Security | `SecurityConfig.java`, `GlobalExceptionHandler.java` | Public access for reviews and artisans; customer access for notifications and review submission; `/api/admin/**` restriction with exact 403 Forbidden `{"success":false,"message":"Not authorised to perform this action"}`. |

---

## 5. Verification Method

### 5.1 Independent Compilation & Test Verification
Run the backend test suite from `backend/backend`:
```bash
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw clean test
```
- **Success Criteria**: 0 compilation errors, all unit and integration tests passing with 100% pass rate.
- **Invalidation Condition**: Any test failure, compilation error, or H2 schema constraint violation.

### 5.2 Specific Backend Unit & Integration Tests to Implement
1. `NotificationServiceTest`:
   - Verify `notifyOrderPlaced`, `notifyOrderDispatched`, `notifyLowStockAlert`, and `notifyMakerCheckerPending` create notifications and dispatch log entries.
   - Verify unread counter and mark-as-read logic.
2. `StockTransferServiceTest`:
   - Verify Manager transfer submission creates pending `ApprovalRequest`.
   - Verify Owner approval deducts source warehouse stock and increments target warehouse stock.
   - Verify Owner direct execution immediately updates stock balances.
   - Verify non-owner rejection keeps stock unchanged.
3. `ReviewServiceTest` & `ReviewControllerTest`:
   - Verify verified buyer detection returns true when user has completed/non-cancelled order for product.
   - Verify unmoderated reviews default to `PENDING` and are excluded from public product reviews.
   - Verify admin moderation (`APPROVED`, `FEATURED`, `REJECTED`) updates status correctly.
   - Verify 403 Forbidden with exact message `{"success":false,"message":"Not authorised to perform this action"}` when unauthorized user accesses `/api/admin/reviews`.
4. `ArtisanServiceTest`:
   - Verify all 5 artisan clusters are retrieved with their linked catalog sarees.

### 5.3 Files to Inspect
- `backend/backend/src/main/resources/application.yaml`
- `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`
- `backend/backend/src/main/java/com/sareekart/config/DataSeeder.java`
- `backend/backend/src/main/java/com/sareekart/entity/Order.java`
- `backend/backend/src/main/java/com/sareekart/entity/ApprovalRequest.java`
