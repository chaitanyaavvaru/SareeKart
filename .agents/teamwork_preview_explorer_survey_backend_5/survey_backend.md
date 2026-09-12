# SareeKart v3.0 Module 1: Returns & Exchanges Backend Architecture Survey

**Document Author**: Backend Architecture Explorer  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5`  
**Date**: 2026-09-11  
**Project**: SareeKart Handloom E-Commerce Platform  

---

## 1. Executive Summary

This architectural survey presents the comprehensive technical blueprint for **SareeKart v3.0 Module 1: Self-Service Customer Returns and Exchanges**. The module introduces a self-service customer returns flow with automated 7-day post-delivery eligibility validation, defect photo upload capabilities (up to 3 photos saved to the local filesystem and served statically), reverse-logistics tracking (courier partner and AWB number), and an administrative moderation workflow for privileged staff (`OWNER`, `MANAGER`, `ADMIN`).

The design adheres strictly to SareeKart's existing architectural standards: Spring Boot 3.5 (Java 17), Spring Data JPA with MySQL (and H2 for in-memory testing), Spring Security 6 with stateless JWT authentication, standard `ApiResponse<T>` response wrapping, and centralized exception handling with standardized HTTP 403 Forbidden payload:
`{"success":false,"message":"Not authorised to perform this action"}`.

---

## 2. Existing Codebase Inspection & Baseline Analysis

### 2.1 Order Domain (`Order.java`, `OrderStatus.java`, `OrderServiceImpl.java`)
- **Entity Location**: `backend/backend/src/main/java/com/sareekart/entity/Order.java`
- **Current Fields**:
  - `id` (Long, PK)
  - `user` (ManyToOne User)
  - `items` (OneToMany OrderItem)
  - `totalAmount` (BigDecimal, precision 10, scale 2)
  - `status` (OrderStatus enum)
  - `shippingAddress` (Embedded Address)
  - `paymentMethod`, `paymentStatus`, `razorpayOrderId`, `razorpayPaymentId`
  - `trackingNumber`, `courierPartner`, `currentLocation`, `estimatedDeliveryDate`
  - `createdAt` (LocalDateTime, `@CreatedDate`)
  - `updatedAt` (LocalDateTime, `@LastModifiedDate`)
- **Status Enum (`OrderStatus.java`)**: `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
- **Delivery Timestamp Finding**:
  - `Order.java` does *not* currently possess a dedicated `deliveredAt` column.
  - When an order transitions to `DELIVERED` via `OrderServiceImpl.updateOrderStatus(Long orderId, String status)`, `order.setStatus(OrderStatus.DELIVERED)` and `order.setPaymentStatus("COMPLETED")` are set, and JPA auditing updates `updatedAt`.
  - **Architectural Enhancement**:
    1. Add `private LocalDateTime deliveredAt;` to `Order.java`.
    2. In `OrderServiceImpl.updateOrderStatus`: When status changes to `DELIVERED`, record `order.setDeliveredAt(LocalDateTime.now())` if `deliveredAt` is null.
    3. In `ReturnServiceImpl`: To guarantee backwards compatibility with seeded or existing orders that have `deliveredAt == null`, compute the delivery reference as:
       ```java
       LocalDateTime deliveryTime = order.getDeliveredAt() != null 
               ? order.getDeliveredAt() 
               : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
       ```
    4. Expose `deliveredAt` in `OrderResponse.java` and map it in `OrderMapper.java`.

### 2.2 Identity & Role-Based Access Control (`User.java`, `Role.java`, `SecurityConfig.java`)
- **User Entity**: `backend/backend/src/main/java/com/sareekart/entity/User.java`
  - Implements `UserDetails`.
  - Roles defined in `com.sareekart.entity.Role`: `CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN`.
  - `getAuthorities()` formats roles with the Spring Security prefix: `ROLE_` + `role.name()`.
- **Security Configuration**: `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`
  - Access control rules currently in place:
    - `/api/auth/**`, `/uploads/**`, `/api/webhook/whatsapp/**`, `/ws-sareekart/**` are `permitAll()`.
    - `GET /api/products/**`, `GET /api/categories/**`, `GET /api/orders/track/**` are `permitAll()`.
    - `/api/admin/**` is restricted to `hasAnyRole("OWNER", "MANAGER", "ADMIN")`.
    - `anyRequest().authenticated()`.
  - Exception Handling:
    - `accessDeniedHandler` writes HTTP 403 with `{"success":false,"message":"Not authorised to perform this action"}`.
    - `authenticationEntryPoint` for `/api/admin` requests returns HTTP 403 with `{"success":false,"message":"Not authorised to perform this action"}`.
    - Unauthenticated requests to customer endpoints receive HTTP 401 with `{"success":false,"message":"Authentication required. Please sign in again."}`.

### 2.3 Response Conventions & Global Exception Handling
- **API Response Wrapper**: `backend/backend/src/main/java/com/sareekart/dto/response/ApiResponse.java`
  - Format: `{ "success": boolean, "message": string, "data": T, "timestamp": LocalDateTime }`
  - Helper factories: `ApiResponse.success(data)`, `ApiResponse.success(message, data)`, `ApiResponse.error(message)`.
- **Exception Handling**: `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java`
  - Handles `ResourceNotFoundException` -> HTTP 404 NOT FOUND.
  - Handles `BadRequestException` -> HTTP 400 BAD REQUEST.
  - Handles `AccessDeniedException` -> HTTP 403 FORBIDDEN with `ApiResponse.error("Not authorised to perform this action")`.
  - Handles `MethodArgumentNotValidException` -> HTTP 400 BAD REQUEST with field validation map.

### 2.4 Static Resource Handling & Defect Photo Uploads
- **Static Configuration**: `backend/backend/src/main/java/com/sareekart/config/StaticResourceConfig.java`
  - Maps URL pattern `/uploads/**` directly to the local directory `uploads/`.
  - Any file written to `uploads/return-photos/<filename>` is immediately and publicly accessible at `http://localhost:8081/uploads/return-photos/<filename>`.
- **Upload Pattern (`PhotoUploadController.java`)**:
  - Validates MIME types (`image/jpeg`, `image/jpg`, `image/png`, `image/webp`).
  - Limits file size (`spring.servlet.multipart.max-file-size: 10MB`).
  - Generates unique safe filenames via `UUID.randomUUID()`.
  - Creates directories via `Files.createDirectories(targetDir)`.

### 2.5 Baseline Test Suite Status
- Command `./mvnw test` executed in `backend/backend`.
- **Result**: Tests run: **65**, Failures: **0**, Errors: **0**, Skipped: **0**, Time: **10.948s** (BUILD SUCCESS).
- Baseline is 100% clean and operational.

---

## 3. Detailed Architecture Design for Returns & Exchanges

### 3.1 Domain Model & JPA Entity

#### 3.1.1 Entity: `ReturnRequest.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/entity/ReturnRequest.java`
- **Table Name**: `return_requests`
- **Field Specifications**:

| Field Name | JPA Annotations / Column Config | Java Type | Description |
|---|---|---|---|
| `id` | `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` | `Long` | Primary key |
| `orderId` | `@Column(name = "order_id", nullable = false)` | `Long` | Foreign key reference to `orders.id` |
| `userId` | `@Column(name = "user_id", nullable = false)` | `Long` | Foreign key reference to `users.id` (claiming customer) |
| `type` | `@Column(name = "type", nullable = false, length = 20)` | `String` | `RETURN` (Refund) or `EXCHANGE` (Replacement) |
| `reason` | `@Column(name = "reason", nullable = false, length = 50)` | `String` | Reason taxonomy: `COLOR_MISMATCH`, `ZARI_DEFECT`, `FABRIC_FEEL`, `INCORRECT_ITEM`, `SIZE_MISMATCH`, `OTHER` |
| `comments` | `@Column(name = "comments", columnDefinition = "TEXT")` | `String` | Detailed customer explanation |
| `status` | `@Column(name = "status", nullable = false, length = 30)` | `String` | Lifecycle status: `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED` |
| `images` | `@ElementCollection(fetch = FetchType.EAGER)`<br>`@CollectionTable(name = "return_request_images", joinColumns = @JoinColumn(name = "return_request_id"))`<br>`@Column(name = "image_url", length = 1000)`<br>`@OrderColumn(name = "image_order")` | `List<String>` | URLs to uploaded condition/defect photos (max 3) |
| `refundAmount` | `@Column(name = "refund_amount", precision = 10, scale = 2)` | `BigDecimal` | Monetary amount for refund (defaults to order total) |
| `refundMode` | `@Column(name = "refund_mode", length = 30)` | `String` | `ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE` |
| `exchangeSku` | `@Column(name = "exchange_sku", length = 100)` | `String` | Target SKU if `type == EXCHANGE` |
| `reverseCourier` | `@Column(name = "reverse_courier", length = 100)` | `String` | Reverse logistics courier (e.g., Blue Dart, Delhivery) |
| `reverseTrackingNumber` | `@Column(name = "reverse_tracking_number", length = 100)` | `String` | Reverse courier AWB tracking identifier |
| `adminNotes` | `@Column(name = "admin_notes", columnDefinition = "TEXT")` | `String` | Staff review notes; mandatory if status is `REJECTED` |
| `createdAt` | `@CreatedDate @Column(name = "created_at", updatable = false)` | `LocalDateTime` | Claim creation timestamp |
| `updatedAt` | `@LastModifiedDate @Column(name = "updated_at")` | `LocalDateTime` | Last status or detail modification timestamp |

#### 3.1.2 Enums & Constants
To guarantee compile-time safety while maintaining repository and serialization simplicity, define the following enums in `com.sareekart.entity`:

1. **`ReturnStatus`**:
   ```java
   package com.sareekart.entity;
   public enum ReturnStatus {
       PENDING,
       APPROVED,
       PICKUP_SCHEDULED,
       REJECTED,
       COMPLETED
   }
   ```
2. **`ReturnType`**:
   ```java
   package com.sareekart.entity;
   public enum ReturnType {
       RETURN,
       EXCHANGE
   }
   ```
3. **`ReturnReason`**:
   ```java
   package com.sareekart.entity;
   public enum ReturnReason {
       COLOR_MISMATCH,
       ZARI_DEFECT,
       FABRIC_FEEL,
       INCORRECT_ITEM,
       SIZE_MISMATCH,
       OTHER
   }
   ```
4. **`RefundMode`**:
   ```java
   package com.sareekart.entity;
   public enum RefundMode {
       ORIGINAL_PAYMENT,
       STORE_CREDIT,
       EXCHANGE_DRAPE
   }
   ```

---

### 3.2 Repository Layer (`ReturnRequestRepository.java`)
- **File Path**: `backend/backend/src/main/java/com/sareekart/repository/ReturnRequestRepository.java`
- **Interface Definition**:
```java
package com.sareekart.repository;

import com.sareekart.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<ReturnRequest> findByOrderId(Long orderId);

    List<ReturnRequest> findByStatusOrderByCreatedAtDesc(String status);

    List<ReturnRequest> findAllByOrderByCreatedAtDesc();

    boolean existsByOrderId(Long orderId);
}
```

---

### 3.3 Data Transfer Objects (DTOs)

#### 3.3.1 Request: `ReturnCreateRequest.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/dto/request/ReturnCreateRequest.java`
```java
package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnCreateRequest {

    @NotNull(message = "Order ID is mandatory")
    private Long orderId;

    @NotBlank(message = "Return type is mandatory (RETURN or EXCHANGE)")
    private String type;

    @NotBlank(message = "Reason is mandatory")
    private String reason;

    private String comments;

    @NotBlank(message = "Refund mode is mandatory (ORIGINAL_PAYMENT, STORE_CREDIT, EXCHANGE_DRAPE)")
    private String refundMode;

    private String exchangeSku;

    private BigDecimal refundAmount;

    @Size(max = 3, message = "Maximum 3 defect photos allowed")
    private List<String> images;
}
```

#### 3.3.2 Request: `ReturnStatusUpdateRequest.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/dto/request/ReturnStatusUpdateRequest.java`
```java
package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatusUpdateRequest {

    @NotBlank(message = "Target status is mandatory (APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED)")
    private String status;

    private String reverseCourier;

    private String reverseTrackingNumber;

    private String adminNotes;

    private BigDecimal refundAmount;
}
```

#### 3.3.3 Response: `ReturnResponse.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/dto/response/ReturnResponse.java`
```java
package com.sareekart.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResponse {

    private Long id;
    private Long orderId;
    private Long userId;
    private String customerEmail;
    private String customerName;
    private String customerMobile;
    private BigDecimal orderTotalAmount;
    private String type;
    private String reason;
    private String comments;
    private String status;
    private List<String> images;
    private BigDecimal refundAmount;
    private String refundMode;
    private String exchangeSku;
    private String reverseCourier;
    private String reverseTrackingNumber;
    private String adminNotes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime orderDeliveredAt;
    private Long daysSinceDelivery;
}
```

---

### 3.4 Service Layer Architecture (`ReturnService` & `ReturnServiceImpl`)

- **Interface**: `backend/backend/src/main/java/com/sareekart/service/ReturnService.java`
- **Implementation**: `backend/backend/src/main/java/com/sareekart/service/impl/ReturnServiceImpl.java`

#### 3.4.1 Business Rules & Enforcement Mechanics

1. **Order Ownership Enforcement**:
   - Order fetched via `orderRepository.findById(request.getOrderId())`.
   - If not found: `throw new ResourceNotFoundException("Order", "id", request.getOrderId())`.
   - Ownership guard: `if (order.getUser() == null || !order.getUser().getId().equals(userId))`
     `throw new BadRequestException("You are not authorized to request a return for this order.");`
2. **Delivered Status Enforcement**:
   - Status guard: `if (order.getStatus() != OrderStatus.DELIVERED)`
     `throw new BadRequestException("Only delivered orders are eligible for return or exchange. Current order status: " + order.getStatus());`
3. **7-Calendar-Day Eligibility Cutoff Enforcement**:
   - Retrieve delivery timestamp:
     ```java
     LocalDateTime deliveredTime = order.getDeliveredAt() != null 
             ? order.getDeliveredAt() 
             : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt());
     ```
   - Cutoff boundary validation:
     ```java
     LocalDateTime cutoff = deliveredTime.plusDays(7);
     if (LocalDateTime.now().isAfter(cutoff)) {
         throw new BadRequestException("Return window has expired. Orders are only eligible for return or exchange within 7 calendar days of delivery.");
     }
     ```
4. **Duplicate Return Prevention**:
   - Check existing return request for order:
     ```java
     if (returnRequestRepository.findByOrderId(order.getId()).isPresent()) {
         throw new BadRequestException("A return or exchange request has already been submitted for Order #" + order.getId() + ".");
     }
     ```
5. **State Machine Transition Rules**:
   - Valid lifecycle states: `PENDING` -> `APPROVED` -> `PICKUP_SCHEDULED` -> `COMPLETED`, or `REJECTED`.
   - Transition Matrix:

| Current Status | Allowed Target Statuses | Additional Validation Requirements |
|---|---|---|
| `PENDING` | `APPROVED`, `REJECTED` | If `REJECTED`: `adminNotes` is mandatory (non-blank). |
| `APPROVED` | `PICKUP_SCHEDULED`, `REJECTED` | If `PICKUP_SCHEDULED`: `reverseCourier` and `reverseTrackingNumber` must be non-blank.<br>If `REJECTED`: `adminNotes` is mandatory. |
| `PICKUP_SCHEDULED` | `COMPLETED`, `REJECTED` | If `COMPLETED`: finalizes refund; if `REJECTED`: `adminNotes` is mandatory. |
| `COMPLETED` | *None (Terminal)* | Throws `BadRequestException`: "Cannot alter status of an already completed return request." |
| `REJECTED` | *None (Terminal)* | Throws `BadRequestException`: "Cannot alter status of an already rejected return request." |

6. **Notification System Integration**:
   - On request submission (`createReturnRequest`):
     - Trigger `notificationEventService` to notify customer: `"Return claim #RR-" + id + " submitted for Order #" + orderId`.
     - Trigger `notificationEventService` to alert manager staff (`targetRole="MANAGER"`): `"New return claim pending review for Order #" + orderId`.
   - On status update (`updateReturnStatus`):
     - Notify customer of status change (`APPROVED`, `PICKUP_SCHEDULED` with courier/AWB, `COMPLETED` with refund details, or `REJECTED` with reason).

---

### 3.5 REST API Controllers

#### 3.5.1 Customer Controller: `ReturnController.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java`
- **Base Path**: `/api/returns`
- **Cross-Origin**: `@CrossOrigin(origins = "*")`
- **Endpoints**:

| Method | Path | Auth Principal | Request Body / Params | HTTP Status | Description |
|---|---|---|---|---|---|
| `POST` | `/api/returns` | `@AuthenticationPrincipal User user` | `@Valid @RequestBody ReturnCreateRequest request` | `201 CREATED` | Submits a new return/exchange claim |
| `GET` | `/api/returns/my-requests` | `@AuthenticationPrincipal User user` | None | `200 OK` | Retrieves all return claims submitted by authenticated user |
| `GET` | `/api/returns/order/{orderId}` | `@AuthenticationPrincipal User user` | `@PathVariable Long orderId` | `200 OK` | Retrieves return telemetry for a specific order owned by user |
| `POST` | `/api/returns/upload-photo` | `@AuthenticationPrincipal User user` | `@RequestParam("file") MultipartFile file` | `200 OK` | Uploads defect condition photo to `uploads/return-photos/` |

#### 3.5.2 Admin Controller: `AdminReturnController.java`
- **File Path**: `backend/backend/src/main/java/com/sareekart/controller/AdminReturnController.java`
- **Base Path**: `/api/admin/returns`
- **Security Guard**: `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`
- **Endpoints**:

| Method | Path | Auth Principal | Request Body / Params | HTTP Status | Description |
|---|---|---|---|---|---|
| `GET` | `/api/admin/returns` | `@AuthenticationPrincipal User adminUser` | `@RequestParam(required = false) String status` | `200 OK` | Retrieves filterable list of return claims (`ALL`, `PENDING`, `APPROVED`, etc.) |
| `PUT` | `/api/admin/returns/{id}/status` | `@AuthenticationPrincipal User adminUser` | `@PathVariable Long id`, `@Valid @RequestBody ReturnStatusUpdateRequest request` | `200 OK` | Updates status (Approve, Schedule Pickup, Complete, Reject) |

---

### 3.6 Security & RBAC Configuration Updates (`SecurityConfig.java`)

In `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`:
```java
// Inside filterChain(HttpSecurity http):
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .requestMatchers("/uploads/**").permitAll()
    .requestMatchers("/api/webhook/whatsapp/**", "/ws-sareekart/**").permitAll()
    .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/categories/**", "/api/orders/track/**").permitAll()
    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
    .requestMatchers("/api/approvals/*/approve", "/api/approvals/*/reject").hasRole("OWNER")
    .requestMatchers("/api/admin/inventory/transfer/*/approve", "/api/admin/inventory/transfer/*/reject").hasRole("OWNER")
    .requestMatchers("/api/approvals/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
    .requestMatchers("/api/excel/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
    .requestMatchers("/api/inventory/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
    // Returns & Exchanges RBAC rules:
    .requestMatchers("/api/admin/returns/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
    .requestMatchers("/api/returns/**").authenticated()
    .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
    .anyRequest().authenticated()
)
```

**Access Denied Verification**:
- Non-staff (e.g. `CUSTOMER` or unauthenticated) accessing `/api/admin/returns` receives HTTP 403:
  ```json
  {
    "success": false,
    "message": "Not authorised to perform this action"
  }
  ```
  This is guaranteed by `SecurityConfig`'s `accessDeniedHandler`, `authenticationEntryPoint`, and `GlobalExceptionHandler`'s `AccessDeniedException` handler.

---

### 3.7 Photo Upload & Storage Details (`uploads/return-photos/`)

- **Upload Directory**: `uploads/return-photos/` (created automatically if missing via `Files.createDirectories`).
- **File Validation**:
  - Null/empty check -> HTTP 400 "No file selected".
  - Allowed MIME types: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`.
  - Max size: 10 MB (enforced by Spring Servlet multipart configuration).
- **Naming Convention**:
  - `return-<uuid_12>.<extension>`, e.g., `return-a8f3b29c104e.jpg`.
- **Public URL**:
  - `/uploads/return-photos/<filename>`.
  - Automatically served by `StaticResourceConfig.java` without further code changes because `uploads/` is mapped to `/uploads/**`.

---

## 4. Edge Cases, Race Conditions & Mitigations

1. **Exact 7-Day Cutoff Boundary**:
   - *Condition*: Customer attempts submission exactly at 7 days 0 hours vs 7 days 1 hour.
   - *Mitigation*: Calculation uses `LocalDateTime.now().isAfter(deliveryTime.plusDays(7))`. Testing must verify that `deliveryTime.plusDays(7).minusMinutes(5)` succeeds, while `deliveryTime.plusDays(7).plusMinutes(5)` fails with HTTP 400.
2. **Orders with Legacy Null `deliveredAt`**:
   - *Condition*: Orders delivered before the `deliveredAt` column was added.
   - *Mitigation*: Hierarchical fallback: `order.getDeliveredAt() -> order.getUpdatedAt() -> order.getCreatedAt()`. Prevents `NullPointerException`.
3. **Concurrent Double-Submission**:
   - *Condition*: Customer clicks "Submit Return" twice simultaneously.
   - *Mitigation*: Service checks `returnRequestRepository.findByOrderId(orderId)`. Additionally, enforce a unique database constraint on `return_requests(order_id)`:
     `@Column(name = "order_id", nullable = false, unique = true)`.
4. **Invalid State Skipping**:
   - *Condition*: Staff attempts to jump from `PENDING` directly to `COMPLETED` without approval or pickup scheduling.
   - *Mitigation*: Enforce transition check in `ReturnServiceImpl`:
     Only allowed transitions are `PENDING -> APPROVED / REJECTED`, `APPROVED -> PICKUP_SCHEDULED / REJECTED`, `PICKUP_SCHEDULED -> COMPLETED / REJECTED`. Any skipping throws `BadRequestException`.
5. **Rejection Without Mandatory Notes**:
   - *Condition*: Staff rejects a claim without specifying why.
   - *Mitigation*: Enforce `if ("REJECTED".equalsIgnoreCase(status) && (adminNotes == null || adminNotes.trim().isEmpty())) throw new BadRequestException("Mandatory rejection reason must be provided in admin notes.");`.
6. **Pickup Scheduled Without Courier Tracking**:
   - *Condition*: Staff marks pickup scheduled without courier name or AWB.
   - *Mitigation*: Enforce `if ("PICKUP_SCHEDULED".equalsIgnoreCase(status) && (courier == null || awb == null)) throw new BadRequestException("Reverse courier and tracking number are required to schedule pickup.");`.
7. **Exchange Without Desired SKU**:
   - *Condition*: Customer selects "Exchange Saree" but leaves `exchangeSku` empty.
   - *Mitigation*: `if ("EXCHANGE".equalsIgnoreCase(type) && (exchangeSku == null || exchangeSku.trim().isEmpty())) throw new BadRequestException("Exchange SKU is required when selecting saree exchange.");`.
8. **Malicious Image Filenames / Path Traversal**:
   - *Condition*: Upload contains filenames like `../../etc/passwd`.
   - *Mitigation*: Ignore client-supplied filename; generate a purely random UUID filename (`UUID.randomUUID().toString()`) with validated extension.

---

## 5. Verification & Test Suite Plan

### 5.1 Unit & Integration Test Suite (`ReturnServiceImplTest.java`)
- **File Path**: `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java`
- **Framework**: JUnit 5 (`@ExtendWith(MockitoExtension.class)` or SpringBootTest)
- **Mandatory Acceptance Test Cases**:

1. `testCreateReturnRequest_Success_WhenDeliveredWithin7Days()`:
   - Order delivered 3 days ago. Return submitted with reason `COLOR_MISMATCH`.
   - Verifies `returnRequestRepository.save()` called, status is `PENDING`, refund amount matches order total.
2. `testCreateReturnRequest_Fails_WhenOrderNotDelivered()`:
   - Order in `SHIPPED` or `PENDING` status.
   - Verifies `BadRequestException` thrown with message indicating order is not delivered.
3. `testCreateReturnRequest_Fails_WhenOrderDeliveredMoreThan7DaysAgo()`:
   - Order delivered 8 days ago.
   - Verifies `BadRequestException` thrown with message indicating return window expired.
4. `testCreateReturnRequest_Fails_WhenDuplicateReturnSubmitted()`:
   - Existing return found in `returnRequestRepository.findByOrderId(orderId)`.
   - Verifies `BadRequestException` thrown: duplicate request rejected.
5. `testAccessControl_CustomerCannotAccessAnotherCustomersReturn()`:
   - Customer B attempts to retrieve return for Order owned by Customer A.
   - Verifies `BadRequestException` or 403 thrown.
6. `testAdminStatusUpdate_Approve_PickupScheduled_Complete()`:
   - Staff approves claim (`PENDING -> APPROVED`).
   - Staff schedules pickup (`APPROVED -> PICKUP_SCHEDULED`) with courier "Delhivery" and AWB "REV-98765".
   - Staff marks completed (`PICKUP_SCHEDULED -> COMPLETED`).
7. `testAdminStatusUpdate_RejectRequiresMandatoryNotes()`:
   - Staff attempts to reject without admin notes -> throws `BadRequestException`.
   - Staff rejects with notes "Saree shows wear and altered fall" -> succeeds, status is `REJECTED`.

### 5.2 Verification Commands
```bash
# 1. Run ReturnServiceImplTest specifically:
./mvnw test -Dtest=ReturnServiceImplTest

# 2. Run full backend test regression (all 65+ existing tests + new suite):
./mvnw test

# 3. Verify disk health headroom:
~/scripts/check_disk_health.sh
```

---

## 6. Implementation Checklist & Target File Locations

| Component | Target File Path | Action |
|---|---|---|
| Entity | `backend/backend/src/main/java/com/sareekart/entity/ReturnRequest.java` | Create |
| Enums | `backend/backend/src/main/java/com/sareekart/entity/ReturnStatus.java`<br>`backend/backend/src/main/java/com/sareekart/entity/ReturnType.java`<br>`backend/backend/src/main/java/com/sareekart/entity/ReturnReason.java`<br>`backend/backend/src/main/java/com/sareekart/entity/RefundMode.java` | Create |
| Order Enhancements | `backend/backend/src/main/java/com/sareekart/entity/Order.java`<br>`backend/backend/src/main/java/com/sareekart/dto/response/OrderResponse.java`<br>`backend/backend/src/main/java/com/sareekart/mapper/OrderMapper.java`<br>`backend/backend/src/main/java/com/sareekart/service/impl/OrderServiceImpl.java` | Modify (Add `deliveredAt`) |
| DTOs | `backend/backend/src/main/java/com/sareekart/dto/request/ReturnCreateRequest.java`<br>`backend/backend/src/main/java/com/sareekart/dto/request/ReturnStatusUpdateRequest.java`<br>`backend/backend/src/main/java/com/sareekart/dto/response/ReturnResponse.java` | Create |
| Repository | `backend/backend/src/main/java/com/sareekart/repository/ReturnRequestRepository.java` | Create |
| Service | `backend/backend/src/main/java/com/sareekart/service/ReturnService.java`<br>`backend/backend/src/main/java/com/sareekart/service/impl/ReturnServiceImpl.java` | Create |
| Controllers | `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java`<br>`backend/backend/src/main/java/com/sareekart/controller/AdminReturnController.java` | Create |
| Security Config | `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java` | Modify (Register RBAC rules) |
| Unit Tests | `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java` | Create |

---
*Report completed and verified against SareeKart codebase and specifications.*
