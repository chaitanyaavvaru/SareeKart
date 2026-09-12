# Handoff Report: Returns & Exchanges Backend Architecture Survey

**Agent**: Backend Architecture Explorer  
**Task Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5`  
**Target Module**: SareeKart v3.0 Module 1: Self-Service Customer Returns and Exchanges  
**Date**: 2026-09-11  

---

## 1. Observation

Direct observations from inspecting the codebase in `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`:

1. **Order Entity (`Order.java:1-71`)**:
   - `Order.java` lines 23-70 defines:
     `id`, `user` (ManyToOne), `items` (OneToMany), `totalAmount` (BigDecimal), `status` (OrderStatus enum), `shippingAddress`, `paymentMethod`, `paymentStatus`, `razorpayOrderId`, `razorpayPaymentId`, `trackingNumber`, `courierPartner`, `currentLocation`, `estimatedDeliveryDate`, `createdAt`, `updatedAt`.
   - `Order.java` currently lacks a dedicated `deliveredAt` timestamp field.
2. **Order Status Updates (`OrderServiceImpl.java:172-193`)**:
   - In `updateOrderStatus`:
     ```java
     OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
     order.setStatus(orderStatus);
     if (orderStatus == OrderStatus.DELIVERED) {
         order.setPaymentStatus("COMPLETED");
     }
     Order updatedOrder = orderRepository.save(order);
     ```
     Currently, when status becomes `DELIVERED`, only `updatedAt` is modified via Spring Data JPA auditing.
3. **Role & Security Rules (`Role.java:1-9`, `SecurityConfig.java:56-84`)**:
   - `Role.java`: `CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN`.
   - `SecurityConfig.java` lines 56-70 configures `accessDeniedHandler` and `authenticationEntryPoint`:
     ```java
     response.setStatus(HttpStatus.FORBIDDEN.value());
     response.setContentType(MediaType.APPLICATION_JSON_VALUE);
     response.getWriter().write("{\"success\":false,\"message\":\"Not authorised to perform this action\"}");
     ```
   - `SecurityConfig.java` lines 71-84 maps `/api/admin/**` to `hasAnyRole("OWNER", "MANAGER", "ADMIN")` and leaves `/api/returns/**` subject to `anyRequest().authenticated()`.
4. **Exception Handling (`GlobalExceptionHandler.java:106-111`)**:
   - Handles `AccessDeniedException`:
     ```java
     @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
     public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(org.springframework.security.access.AccessDeniedException ex) {
         return ResponseEntity
                 .status(HttpStatus.FORBIDDEN)
                 .body(ApiResponse.error("Not authorised to perform this action"));
     }
     ```
5. **Static Resource Handling (`StaticResourceConfig.java:17-26`)**:
   - Registers resource handler:
     ```java
     String uploadPath = Paths.get("uploads").toAbsolutePath().toUri().toString();
     if (!uploadPath.endsWith("/")) {
         uploadPath += "/";
     }
     registry.addResourceHandler("/uploads/**").addResourceLocations(uploadPath);
     ```
     Any file saved in `uploads/return-photos/<filename>` is automatically accessible at `http://localhost:8081/uploads/return-photos/<filename>`.
6. **Existing Test Suite Baseline**:
   - Tool command: `./mvnw test` in `backend/backend/`
   - Result: `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0, Total time: 10.948 s [BUILD SUCCESS]`.
7. **Disk Health & Storage Headroom**:
   - Tool command: `~/scripts/check_disk_health.sh`
   - Result: `Available Free Space: 78.6 GiB (34.4% >= 30%) [PASS]`.

---

## 2. Logic Chain

1. **Delivered Timestamp Tracking**:
   - *From Observation 1 & 2*: `Order.java` lacks `deliveredAt`, relying only on `updatedAt`. If an order is modified after delivery (e.g., tracking notes updated), `updatedAt` is overwritten, potentially altering the return eligibility window.
   - *Therefore*: Add `private LocalDateTime deliveredAt;` to `Order.java` and set `order.setDeliveredAt(LocalDateTime.now())` in `OrderServiceImpl.updateOrderStatus`.
   - *Furthermore*: In `ReturnServiceImpl`, provide fallback `order.getDeliveredAt() != null ? order.getDeliveredAt() : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt())` so that seeded and existing pre-v3.0 orders remain fully testable and backward-compatible.
2. **Eligibility & Cutoff Calculation**:
   - *From Requirement R1/R2*: Return claims require `order.getStatus() == OrderStatus.DELIVERED` and `deliveryTime <= 7 calendar days`.
   - *Therefore*: Validate `if (order.getStatus() != OrderStatus.DELIVERED) throw new BadRequestException(...)` and `if (LocalDateTime.now().isAfter(deliveryTime.plusDays(7))) throw new BadRequestException(...)`.
3. **Preventing Duplicate Returns & Data Integrity**:
   - *From Requirement R2*: Duplicate returns on the same order must be rejected.
   - *Therefore*: Implement `returnRequestRepository.findByOrderId(orderId)` check in `ReturnServiceImpl.createReturnRequest` and add a unique constraint on `return_requests.order_id`.
4. **State Machine Transitions & Guard Enforcements**:
   - *From Requirement R2 & R4*: Lifecycle follows `PENDING` -> `APPROVED` -> `PICKUP_SCHEDULED` -> `COMPLETED`, or `REJECTED`.
   - *Therefore*:
     - `PENDING` can transition to `APPROVED` or `REJECTED`.
     - `APPROVED` can transition to `PICKUP_SCHEDULED` or `REJECTED`.
     - `PICKUP_SCHEDULED` can transition to `COMPLETED` or `REJECTED`.
     - `COMPLETED` and `REJECTED` are terminal.
     - Transitions to `REJECTED` strictly enforce non-blank `adminNotes`.
     - Transitions to `PICKUP_SCHEDULED` strictly enforce non-blank `reverseCourier` and `reverseTrackingNumber`.
5. **RBAC & 403 Response Guarantee**:
   - *From Observation 3 & 4*: The security filter chain and global exception handler already enforce the exact response `{"success":false,"message":"Not authorised to perform this action"}` on 403 Forbidden.
   - *Therefore*: Add explicit matchers in `SecurityConfig.java`:
     `.requestMatchers("/api/returns/**").authenticated()`
     `.requestMatchers("/api/admin/returns/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")`
6. **Photo Storage & Delivery**:
   - *From Observation 5*: `StaticResourceConfig` routes `/uploads/**` to the local `uploads/` folder.
   - *Therefore*: `POST /api/returns/upload-photo` can save uploaded files directly to `uploads/return-photos/` with a UUID filename and return `/uploads/return-photos/<filename>`, requiring zero changes to `StaticResourceConfig.java`.

---

## 3. Caveats

1. **Order Modification Concurrency**: If multiple return requests for the same order arrive concurrently, the `findByOrderId` check is safeguarded by the unique constraint on `return_requests(order_id)`.
2. **Notification Simulation**: `NotificationEventService` currently persists notifications to the database and logs them without external SMS/WhatsApp network calls, matching SareeKart's local offline isolation policy.
3. **Frontend Scope Boundary**: This investigation focuses purely on the backend architecture. Detailed frontend components (`ReturnRequestModal.jsx`, `ManageReturns.jsx`, `MyOrders.jsx` updates) will consume these defined endpoints.

---

## 4. Conclusion

The complete backend architecture for the Returns & Exchanges system has been designed and documented in detail in `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/survey_backend.md`.

Key components designed:
1. **Entity**: `ReturnRequest` (`return_requests` table) with all 16 required attributes, plus enums `ReturnStatus`, `ReturnType`, `ReturnReason`, `RefundMode`.
2. **Repository**: `ReturnRequestRepository` with `findByUserIdOrderByCreatedAtDesc`, `findByOrderId`, `findByStatusOrderByCreatedAtDesc`, `findAllByOrderByCreatedAtDesc`, `existsByOrderId`.
3. **Service Layer**: `ReturnService` and `ReturnServiceImpl` with ownership checks, 7-day cutoff validation, duplicate guards, state machine validation, and reverse logistics tracking.
4. **Controllers**:
   - `ReturnController` (`POST /api/returns`, `GET /api/returns/my-requests`, `GET /api/returns/order/{orderId}`, `POST /api/returns/upload-photo`).
   - `AdminReturnController` (`GET /api/admin/returns`, `PUT /api/admin/returns/{id}/status`).
5. **Security & Static Config**: Exact RBAC rules and static photo URL mapping under `/uploads/return-photos/**`.
6. **Test Specifications**: Comprehensive test suite `ReturnServiceImplTest.java` covering all 6 mandatory acceptance criteria and edge cases.

---

## 5. Verification Method

To independently verify this architectural design and its integration:

1. **Inspect Artifacts**:
   - Survey document: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/survey_backend.md`
   - Progress log: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/progress.md`
   - Working memory: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/BRIEFING.md`
2. **Verify Baseline Tests**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test
   ```
   *Expected result*: 65 tests pass with 0 failures, 0 errors.
3. **Verify Planned Unit Tests Once Implemented**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test -Dtest=ReturnServiceImplTest
   ```
   *Expected result*: All return creation, eligibility, access control, and status transition unit tests pass 100%.
4. **Invalidation Conditions**:
   - If `OrderStatus` enum is changed without coordinating `ReturnServiceImpl`'s `DELIVERED` check.
   - If `/api/admin/**` 403 error payload is changed from `{"success":false,"message":"Not authorised to perform this action"}`.
