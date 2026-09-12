# SareeKart v3.0 Module 1: Customer Returns & Exchanges Specification & Test Mining Report

## Executive Summary & Survey Metadata
- **Survey Date**: 2026-09-11
- **Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main`
- **Specification Source**: `ORIGINAL_REQUEST.md` (§ Follow-up — 2026-09-11T10:04:03Z), database schema backups (`backups/sareekart_db_latest.sql`), authoritative reference Flyway migrations (`~/SareeKart/backend/backend/src/main/resources/db/migration/`), Spring Boot 3 backend codebase, and Vite/React frontend build pipeline.
- **Disk Health Verification**: `/Users/chaitanyachaitu/scripts/check_disk_health.sh` -> **34.4% Free Space (78.5 GiB available)** [PASS: Target >= 30%].
- **Backend Baseline Test Status**: `./mvnw test` -> **65 tests run, 0 failures, 0 errors, 0 skipped** (11.1s execution time).
- **Frontend Build Status**: `cd frontend && npm run build` -> **0 errors, all chunks <= 227.4 kB** [PASS: Target < 500 kB].

---

## 1. Database Schema & Flyway Migration Specification

### 1.1 Existing Migration Architecture & Version Numbering
1. **Repository Observation**:
   - In `Downloads/SareeKart-main/backend/backend/src/main/resources/`, there was initially no `db/migration/` directory because the local development profile utilizes Hibernate `ddl-auto: update` pointing to MySQL on port 3306 (`sareekart_db`).
   - In the authoritative reference repository (`~/SareeKart/backend/backend/src/main/resources/db/migration/`), Flyway migrations are tracked sequentially from `V1` to `V16`:
     * `V1__create_initial_schema.sql` through `V15__review_schema_update.sql`
     * `V16__order_shipment_tracking_and_returns.sql` added basic tracking and raw columns (`courier_name`, `tracking_number`, `return_status`, `return_reason`, `return_comment`, `return_requested_at`, `return_resolved_at`, `return_admin_notes`) directly on the `orders` table.
2. **Architectural Evolution in v3.0**:
   - The new v3.0 Returns & Exchanges specification upgrades returns from denormalized single-string columns on `orders` to a **dedicated first-class domain table `return_requests`**.
   - This provides multi-image condition defect uploads, distinct return vs. exchange item SKUs, granular reverse-pickup logistics tracking (courier partner + reverse AWB), refund mode selection (`ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE`), and multi-state admin moderation audit history.
   - **Migration Versioning Strategy**:
     * Primary target: `V17__create_return_requests_table.sql` in `backend/backend/src/main/resources/db/migration/` to seamlessly append to the existing V1-V16 sequence.
     * Standalone fallback: If a fresh Flyway baseline is seeded for `Downloads/SareeKart-main`, the script is equally self-contained and idempotent as `V1__create_return_requests_table.sql`.

### 1.2 Flyway DDL Specification: `V17__create_return_requests_table.sql`
The script below satisfies MySQL 8.0+ syntax in production and H2 in-memory mode in test suites:

```sql
-- V17: Create dedicated customer return and exchange requests table
-- SareeKart v3.0 Module 1: Self-Service Customer Returns & Exchanges

CREATE TABLE return_requests (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(30) NOT NULL DEFAULT 'RETURN',
    reason VARCHAR(50) NOT NULL,
    comments TEXT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    images TEXT NULL,
    refund_amount DECIMAL(10,2) NULL,
    refund_mode VARCHAR(50) NULL,
    exchange_sku VARCHAR(100) NULL,
    reverse_courier VARCHAR(100) NULL,
    reverse_tracking_number VARCHAR(100) NULL,
    admin_notes TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_return_requests_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE RESTRICT,
    CONSTRAINT fk_return_requests_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uk_return_requests_order UNIQUE (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance and query indexes
CREATE INDEX idx_return_requests_user ON return_requests (user_id);
CREATE INDEX idx_return_requests_order ON return_requests (order_id);
CREATE INDEX idx_return_requests_status ON return_requests (status);
CREATE INDEX idx_return_requests_created_at ON return_requests (created_at);
```

### 1.3 Column & Constraint Design Rationale
- **`order_id` & `CONSTRAINT uk_return_requests_order UNIQUE (order_id)`**:
  Enforces the business rule: **one return/exchange claim per order**. Prevents race conditions or accidental double-submissions from the customer UI.
- **`user_id` & `CONSTRAINT fk_return_requests_user`**:
  Directly binds the return claim to the authenticated customer, enabling rapid ownership checks without joining back to `orders`.
- **`type`**: `VARCHAR(30)` defaulting to `'RETURN'`. Allowed domain values: `RETURN` (refund) and `EXCHANGE` (swap saree drape).
- **`reason`**: `VARCHAR(50)`. Domain taxonomy:
  `COLOR_MISMATCH`, `ZARI_DEFECT`, `FABRIC_FEEL`, `INCORRECT_ITEM`, `SIZE_MISMATCH`, `OTHER`.
- **`status`**: `VARCHAR(30)` defaulting to `'PENDING'`. State machine progression:
  `PENDING` -> `APPROVED` -> `PICKUP_SCHEDULED` -> `COMPLETED`, or `PENDING`/`APPROVED` -> `REJECTED`.
- **`images`**: `TEXT` (or `JSON` in MySQL 8). Stored as a JSON-serialized string of photo URLs (e.g. `["/uploads/return-photos/defect-1.jpg", "/uploads/return-photos/defect-2.jpg"]`) for seamless multi-image defect audits and H2 test compatibility.
- **`refund_amount`**: `DECIMAL(10,2)` matching `orders.total_amount`.
- **`refund_mode`**: `VARCHAR(50)` (`ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE`).
- **`reverse_courier` & `reverse_tracking_number`**: Assigned by staff (`OWNER`, `MANAGER`, `ADMIN`) when scheduling doorstep pickup via BlueDart Reverse Logistics, Delhivery, DTDC, or India Post.
- **`admin_notes`**: Required when status transitions to `REJECTED` (providing the customer explanation), and optional for internal staff notes during pickup scheduling.

---

## 2. Backend Domain & Test Infrastructure Specification

### 2.1 Backend Architecture & Test Conventions
- **Framework & Libraries**: Spring Boot 3.5.15, Spring Data JPA, Hibernate ORM 6.6, Spring Security 6.
- **Testing Tools**: JUnit 5 (Jupiter), Mockito 5 (`@ExtendWith(MockitoExtension.class)`), MockMvc (`standaloneSetup`).
- **In-Memory H2 Configuration (`src/test/resources/application-test.yaml`)**:
  ```yaml
  spring:
    datasource:
      url: jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
      username: sa
      password:
      driver-class-name: org.h2.Driver
    jpa:
      hibernate:
        ddl-auto: create-drop
      properties:
        hibernate:
          dialect: org.hibernate.dialect.H2Dialect
  ```
- **Error Handling & Response Envelope**:
  All API responses use `ApiResponse<T>`:
  - Success: `{"success": true, "message": "...", "data": {...}}`
  - Client errors: `BadRequestException` handled by `GlobalExceptionHandler` returning HTTP 400 `{"success": false, "message": "..."}`
  - Not found: `ResourceNotFoundException` returning HTTP 404
  - Unauthorized: Spring Security access denial returning HTTP 403 `{"success": false, "message": "Not authorised to perform this action"}`.

### 2.2 JPA Entity Specification (`com.sareekart.entity.ReturnRequest`)
```java
package com.sareekart.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "return_requests", uniqueConstraints = {
    @UniqueConstraint(name = "uk_return_requests_order", columnNames = {"order_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequest {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_PICKUP_SCHEDULED = "PICKUP_SCHEDULED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_COMPLETED = "COMPLETED";

    public static final String TYPE_RETURN = "RETURN";
    public static final String TYPE_EXCHANGE = "EXCHANGE";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String type = TYPE_RETURN;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String comments;

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = STATUS_PENDING;

    @Column(columnDefinition = "TEXT")
    private String images; // JSON-serialized list of uploaded photo URLs

    @Column(precision = 10, scale = 2)
    private BigDecimal refundAmount;

    @Column(length = 50)
    private String refundMode; // ORIGINAL_PAYMENT, STORE_CREDIT, EXCHANGE_DRAPE

    @Column(length = 100)
    private String exchangeSku;

    @Column(length = 100)
    private String reverseCourier;

    @Column(length = 100)
    private String reverseTrackingNumber;

    @Column(columnDefinition = "TEXT")
    private String adminNotes;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

### 2.3 Repository Interface Specification (`com.sareekart.repository.ReturnRequestRepository`)
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

### 2.4 Service Interface Specification (`com.sareekart.service.ReturnService`)
```java
package com.sareekart.service;

import com.sareekart.dto.request.ReturnRequestDto;
import com.sareekart.dto.request.ReturnStatusUpdateDto;
import com.sareekart.dto.response.ReturnResponseDto;
import com.sareekart.entity.User;

import java.util.List;

public interface ReturnService {
    ReturnResponseDto createReturnRequest(ReturnRequestDto requestDto, User customer);
    List<ReturnResponseDto> getMyReturnRequests(User customer);
    ReturnResponseDto getReturnRequestByOrderId(Long orderId, User customer);
    List<ReturnResponseDto> getAllReturnsForAdmin(String statusFilter);
    ReturnResponseDto updateReturnStatus(Long returnId, ReturnStatusUpdateDto updateDto, User staffUser);
}
```

---

## 3. Comprehensive Test Plan: `ReturnServiceImplTest.java`

The unit and integration test suite `ReturnServiceImplTest.java` must be co-located at `backend/backend/src/test/java/com/sareekart/service/impl/ReturnServiceImplTest.java` and execute with `@ExtendWith(MockitoExtension.class)`.

### Test Case Matrix

| # | Test Method Name | Scenario & Preconditions | Execution & Trigger | Expected Assertions & Outcomes |
|---|-------------------|--------------------------|---------------------|--------------------------------|
| **T1** | `testCreateReturn_DeliveredOrderWithin7Days_Success` | Customer owns order #100. Status is `OrderStatus.DELIVERED`. `updatedAt` / delivery timestamp is 3 days ago (<= 7 days). No prior return exists. | Customer submits valid return request (`COLOR_MISMATCH`, `ORIGINAL_PAYMENT`, 2 images). | Return request saved with status `PENDING`, correct orderId, userId, amount. Response returned with `success: true`. |
| **T2** | `testCreateReturn_NonDeliveredOrder_ThrowsBadRequestException` | Order #101 is in `OrderStatus.SHIPPED` (or `PENDING`, `CONFIRMED`, `CANCELLED`). | Customer attempts to submit return request. | `BadRequestException` is thrown: *"Order must be DELIVERED to request a return or exchange"*. No record saved. |
| **T3** | `testCreateReturn_DeliveredOrderOlderThan7Days_ThrowsBadRequestException` | Order #102 is `DELIVERED`, but delivery timestamp (`updatedAt`) was 9 days ago (> 7 days cutoff). | Customer attempts to submit return request. | `BadRequestException` is thrown: *"Return window has expired (7 days cutoff from delivery)"*. No record saved. |
| **T4** | `testCreateReturn_DuplicateSubmissionOnSameOrder_ThrowsBadRequestException` | Return request already exists in repository for Order #103. | Customer attempts to submit a second return request on Order #103. | `BadRequestException` is thrown: *"A return or exchange request already exists for this order"*. |
| **T5** | `testCustomerAccessControl_CannotAccessAnotherCustomersReturnClaim` | Return request #50 belongs to User #10 (Customer A). User #20 (Customer B) requests return details for Order #50. | Customer B calls `getReturnRequestByOrderId(orderId, customerB)`. | Throws `BadRequestException` or `AccessDeniedException`: *"You are not authorized to view this return claim"*. |
| **T6** | `testStaffRole_CanSchedulePickupWithCourierAndAWB` | Return request #50 in `APPROVED` status. Staff member has `Role.MANAGER` (or `OWNER`/`ADMIN`). | Staff calls `updateReturnStatus(50L, updateDto, manager)` with `status: "PICKUP_SCHEDULED"`, courier `"BlueDart Reverse Logistics"`, AWB `"BDR-89214"`. | Status changes to `PICKUP_SCHEDULED`, reverse courier and AWB are saved, audit timestamps updated. |
| **T7** | `testStaffRole_CanApproveReturnRequest` | Return request #50 in `PENDING` status. Staff member has `Role.ADMIN`. | Staff calls `updateReturnStatus(50L, updateDto, admin)` with `status: "APPROVED"`. | Status updated to `APPROVED`. |
| **T8** | `testStaffRole_CanRejectReturnWithMandatoryReason` | Return request #50 in `PENDING` status. Staff member has `Role.OWNER`. | Staff calls `updateReturnStatus(50L, updateDto, owner)` with `status: "REJECTED"` and `adminNotes: "Defect not present on dispatch inspection"`. | Status updated to `REJECTED`, `adminNotes` recorded. |
| **T9** | `testStaffRole_RejectReturnWithoutReason_ThrowsBadRequestException` | Return request #50 in `PENDING` status. Staff member attempts to reject without providing notes. | Staff calls `updateReturnStatus` with status `REJECTED` and empty/null reason. | `BadRequestException` thrown: *"Mandatory explanation note required when rejecting a return claim"*. |
| **T10** | `testCustomerRole_CannotUpdateReturnStatus_Forbidden` | Return request #50 exists. User has `Role.CUSTOMER`. | Customer attempts to call `updateReturnStatus`. | Throws `AccessDeniedException` or `SecurityException`: *"Not authorised to perform this action"*. |

---

## 4. Frontend Architecture & Build Constraints

### 4.1 Vite & Build Pipeline
- **Bundler**: Vite 8.0.16 with `@tailwindcss/vite` 4.3.1 and `@vitejs/plugin-react` 6.0.1.
- **Node & Dependencies**: Node.js, React 19.2.6, React Router DOM 7.17.0, Redux Toolkit 2.12.0, Lucide React 1.18.0.
- **Build Command**: `cd frontend && npm run build`.
- **Chunk Size Budget**:
  * Constraint: **All chunks must strictly remain under 500 kB**.
  * `vite.config.js` enforces `chunkSizeWarningLimit: 480`.
  * Configured manual chunks:
    - `vendor-react` (React, ReactDOM, React Router): **227.44 kB**
    - `vendor-framer-motion` (Framer Motion): **132.83 kB**
    - `vendor-lucide` (Lucide Icons): **29.58 kB**
    - `vendor-redux` (Redux Toolkit): **21.39 kB**
    - Application chunks: All individual page chunks range from **0.39 kB to 48.30 kB**.
  * **Design Rules for Returns Components**:
    - Use pure Tailwind CSS for modals, buttons, and steppers.
    - Leverage existing `lucide-react` icons (`RotateCcw`, `UploadCloud`, `CheckCircle2`, `Truck`, `AlertCircle`, `X`, `Eye`).
    - Zero external modal, drag-and-drop, or charting packages to maintain the strict < 500 kB budget.

### 4.2 Component Architecture & Integration Points
1. **Customer Modal (`ReturnRequestModal.jsx`)**:
   - Integrated into `frontend/src/pages/MyOrders.jsx`.
   - Renders a "Return / Exchange" button on delivered order cards.
   - Evaluates the 7-day post-delivery cutoff:
     ```javascript
     const isEligible = order.status === 'DELIVERED' && 
       ((new Date() - new Date(order.updatedAt || order.createdAt)) / (1000 * 60 * 60 * 24)) <= 7;
     ```
   - If not eligible, displays a disabled button with a clear tooltip (e.g. *"Returns only available within 7 days of delivery"* or *"Order must be delivered"*).
   - Multi-step form with condition photo uploader (up to 3 photos), reason taxonomy selector, return vs. exchange toggle, refund preference, and comments.
2. **Admin Console (`ManageReturns.jsx`)**:
   - Registered at route `/admin/returns` in `frontend/src/routes/AppRouter.jsx`.
   - Added to `ADMIN_NAV` under the Commerce group in `frontend/src/pages/Admin/AdminDashboard.jsx`.
   - Status tabs: `ALL`, `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`.
   - Inspection Drawer to view high-resolution defect photos alongside order items.
   - 1-Click Action Controls: Approve, Schedule Pickup (Courier name + AWB number input modal), Complete Refund, Reject (with mandatory reason modal).
3. **Photo Upload API (`/api/returns/upload-photo`)**:
   - Stores defect photos in `uploads/return-photos/`.
   - Publicly served via `/uploads/**` static handler configured in `StaticResourceConfig.java`.

---

## 5. Storage Optimization & System Health
- **Disk Space Target**: At least 30% free disk space on `/System/Volumes/Data` (~70+ GiB available).
- **Verification Script**: `/Users/chaitanyachaitu/scripts/check_disk_health.sh`.
- **Pre-execution Verification Result**:
  - Mount Point: `/System/Volumes/Data`
  - Total Storage: `228.3 GiB`
  - Used Storage: `114.3 GiB (50.1%)`
  - Available Free Space: `78.5 GiB (34.4%)`
  - Status: `[PASS] Healthy Storage Headroom (34.4% >= 30%)`.
- **Hygiene Discipline**: Clean transient test artifacts (`frontend/test-results/`) after Playwright runs; keep model weights pruned.

---

## 6. Features Discovered & Edge Cases

### Features Discovered
| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | Storefront | 7-Day Eligibility Check | Evaluates whether delivered order was delivered within 7 calendar days | `order.status`, `order.updatedAt` | Boolean (`isEligible`), tooltip text | Disabled button with explanatory tooltip | `ORIGINAL_REQUEST.md` R1 |
| 2 | Storefront | Doorstep Return Modal | Self-service modal for customer return/exchange submission | Order ID, return type, reason, comments, photos, refund mode | API request to `POST /api/returns` | Form validation errors | `ORIGINAL_REQUEST.md` R1 |
| 3 | Storefront | Defect Photo Upload | Uploads up to 3 defect photos (JPG, PNG, WebP) to `uploads/return-photos/` | `MultipartFile` file | Static file URL `/uploads/return-photos/...` | HTTP 400 on format mismatch or > 15MB | `ORIGINAL_REQUEST.md` R3 & `PhotoUploadController` |
| 4 | Storefront | Order Card Telemetry | Displays real-time return status pill and milestones on `MyOrders.jsx` | Order return status & tracking | Status pill, tracking milestone modal | N/A | `ORIGINAL_REQUEST.md` R5 |
| 5 | Admin Console | Moderation Dashboard | `/admin/returns` console for staff with status filter tabs | Status filter pill (`PENDING`, `APPROVED`, etc.) | Paginated/filtered claims list | HTTP 403 for unauthorized users | `ORIGINAL_REQUEST.md` R4 |
| 6 | Admin Console | Reverse Logistics Assignment | Assigns courier name and reverse AWB number to schedule pickup | Return ID, courier name, tracking number | Status `PICKUP_SCHEDULED`, updated claim | HTTP 400 on missing AWB | `ORIGINAL_REQUEST.md` R4 |
| 7 | Admin Console | Rejection Moderation | Rejects return claim with mandatory explanation note | Return ID, rejection note | Status `REJECTED`, admin note saved | HTTP 400 on blank rejection reason | `ORIGINAL_REQUEST.md` R4 |
| 8 | Backend Domain | Duplicate Prevention | Prevents submitting multiple return claims on the same order | `orderId` | Return claim entity | HTTP 400 `BadRequestException` | Database `UNIQUE(order_id)` & Service validation |
| 9 | Security | Role-Based Access Control | Enforces customer ownership on claims, restricts admin endpoints to staff | JWT token, user roles | HTTP 200 / 201 | HTTP 403 `{"success":false,"message":"Not authorised..."}` | `SecurityConfig.java` & `GlobalExceptionHandler` |
| 10 | Static Asset Serving | Return Photo Hosting | Maps `/uploads/**` to local directory `uploads/` | HTTP GET `/uploads/return-photos/...` | Image byte stream | HTTP 404 | `StaticResourceConfig.java` |

### Edge Cases
| # | Feature | Input | Observed / Expected Behavior |
|---|---------|-------|------------------------------|
| E1 | 7-Day Cutoff Gate | Delivered order exactly 7 days 0 hours old | Return request is eligible and succeeds. |
| E2 | 7-Day Cutoff Gate | Delivered order 7 days 1 minute old | Return request rejected with HTTP 400 "Return window has expired". |
| E3 | Status Gate | Order status `SHIPPED` or `PENDING` | UI shows disabled button; API returns HTTP 400 "Order must be DELIVERED". |
| E4 | Duplicate Submission | Re-submitting return on order with existing PENDING claim | Unique constraint / service check returns HTTP 400 duplicate error. |
| E5 | Cross-Customer Access | Customer B queries return claim of Customer A | API returns HTTP 403 / 404 access denied error; no leak of customer data. |
| E6 | Missing Rejection Note | Staff attempts to reject return without entering a reason | HTTP 400 "Mandatory explanation note required when rejecting a return claim". |
| E7 | Defect Photo Format | Customer uploads `.exe` or `.pdf` file | HTTP 400 "Unsupported image format. Please upload JPG, PNG, or WebP". |
| E8 | Defect Photo Limit | Customer uploads 4 photos (limit is 3) | Client validation flags max 3 files limit before submission. |
| E9 | Exchange SKU Validation | Return type `EXCHANGE` selected with no target SKU | Client flags required exchange saree selection; API validates exchange SKU. |
| E10 | Courier AWB Assignment | Staff schedules pickup with blank AWB number | Form validation prompts for required reverse tracking number. |

---

## 7. Verification Commands & Execution Checklists
To independently verify the environment, test suite, and build constraints:

```bash
# 1. Verify storage headroom (target >= 30% free)
/Users/chaitanyachaitu/scripts/check_disk_health.sh

# 2. Run backend regression baseline
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw test

# 3. Run target ReturnServiceImplTest (once created)
./mvnw test -Dtest=ReturnServiceImplTest

# 4. Verify frontend production build and chunk budget (< 500 kB)
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
npm run build
```
