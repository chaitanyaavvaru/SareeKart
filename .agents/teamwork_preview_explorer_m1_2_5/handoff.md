# Handoff Report: Milestone 1 Backend Service Layer & Business Logic

**Agent**: Explorer 2 (`teamwork_preview_explorer_m1_2_5`)  
**Task**: Milestone 1 Backend Service Layer & Business Logic Exploration  
**Date**: 2026-09-11  
**Target File**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2_5/m1_service_spec.md`  

---

## 1. Observation

1. **Order Entity & Delivery Timestamp**:
   - Inspected `backend/backend/src/main/java/com/sareekart/entity/Order.java` (lines 1–71).
   - Observed that `Order.java` currently contains `status` (`OrderStatus`), `createdAt`, and `updatedAt`, but does *not* contain a `deliveredAt` column.
   - Inspected `backend/backend/src/main/java/com/sareekart/service/impl/OrderServiceImpl.java` (lines 172–193). When `updateOrderStatus` is called with `status = "DELIVERED"`, it sets `order.setStatus(OrderStatus.DELIVERED)` and `order.setPaymentStatus("COMPLETED")`. `updatedAt` is updated by JPA auditing.
2. **Notification Architecture**:
   - Inspected `backend/backend/src/main/java/com/sareekart/service/NotificationEventService.java` (lines 1–111) and `com/sareekart/entity/Notification.java` (lines 1–56).
   - Observed methods `notifyOrderPlaced`, `notifyOrderDispatched`, `notifyOrderDelivered`, `notifyLowStock`, `notifyStockTransferRequested`, and `notifyReviewSubmitted`.
   - Each notification persists a `Notification` entity specifying `userId` (for customer notifications), `targetRole` (e.g. `"MANAGER"`, `"OWNER"`, `"ADMIN"`), `title`, `message`, `type`, and `linkUrl`.
   - Inspected `backend/backend/src/main/java/com/sareekart/controller/ReviewController.java` (lines 84–88). Notification dispatches are safely wrapped in try-catch blocks to prevent notification failures from rolling back core transactions.
3. **Role-Based Access Control & Exception Handling**:
   - Inspected `backend/backend/src/main/java/com/sareekart/entity/Role.java` (`CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN`) and `User.java` (lines 57–59: `getAuthorities()` formats roles as `"ROLE_" + role.name()`).
   - Inspected `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java` (lines 28–33, 106–111).
   - Observed that `BadRequestException` maps to HTTP 400 with `ApiResponse.error(ex.getMessage())`, and `AccessDeniedException` maps to HTTP 403 with `ApiResponse.error("Not authorised to perform this action")`.
4. **Baseline Test Execution**:
   - Baseline backend test suite runs 65 tests in 10.9s with 0 failures, 0 errors, and 0 skipped.
   - Storage check indicates healthy storage headroom (34.4% free space, 78.5 GiB available).

---

## 2. Logic Chain

1. **Delivery Window Calculation (Referencing Observation 1)**:
   - Because `deliveredAt` may be newly introduced in M1 while legacy or seeded orders may have `deliveredAt == null`, `ReturnServiceImpl` must evaluate delivery time hierarchically:
     `order.getDeliveredAt() != null ? order.getDeliveredAt() : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getCreatedAt())`.
   - The 7-day cutoff boundary must be calculated using `LocalDateTime.now().isAfter(deliveryTime.plusDays(7))`. If true, `BadRequestException` is thrown.
2. **Order Ownership & Security Enforcement (Referencing Observation 1 & 3)**:
   - When a customer submits a return or queries an order's return telemetry, the system validates `order.getUser().getId().equals(currentUser.getId())`.
   - Any customer attempting to view or submit a return for another user's order must be rejected with `AccessDeniedException` (or `BadRequestException`), returning the standardized HTTP 403 or 400 response.
3. **Duplicate Claim Prevention**:
   - `returnRequestRepository.findByOrderId(order.getId())` is checked before creation. If present, throws `BadRequestException("A return or exchange request already exists for this order.")`. Combined with a unique database constraint `uk_return_requests_order`, duplicate claims are impossible.
4. **State Machine Transitions**:
   - Standard linear progression is `PENDING -> APPROVED -> PICKUP_SCHEDULED -> COMPLETED`.
   - `REJECTED` is allowed from `PENDING` or `APPROVED`, but requires non-blank `adminNotes`.
   - `PICKUP_SCHEDULED` requires non-blank `reverseCourier` and `reverseTrackingNumber`.
   - Terminal states (`COMPLETED`, `REJECTED`) reject any modification attempts with `BadRequestException`.
5. **Notification Integration (Referencing Observation 2)**:
   - Adding `notifyReturnSubmitted` and `notifyReturnStatusUpdated` to `NotificationEventService` enables in-app telemetry for customers (`/orders`) and staff (`/admin/returns`).
   - Wrapping notifications in `try-catch` within `ReturnServiceImpl` prevents notification persistence issues from disrupting transaction commits.

---

## 3. Caveats

1. **Storage of Defect Photos in Entity**:
   - If Explorer 1 defines `ReturnRequest` using `@ElementCollection List<String> images`, the service passes `List<String>` directly. If defined as a `TEXT` column storing a JSON-serialized array, the service can serialize/deserialize using Jackson `ObjectMapper`. The specification accommodates both approaches seamlessly.
2. **Order Entity Modification**:
   - Explorer 1 is expected to add `deliveredAt` to `Order.java` and `OrderServiceImpl`. `ReturnServiceImpl` is designed defensively to handle both `deliveredAt` presence and null-safe fallback to `updatedAt`/`createdAt`.
3. **Role Authorization in Tests**:
   - When testing `ReturnServiceImpl` in isolation without Spring Security filter chains, the service itself validates `user.getRole()` (`validateStaffRole`), throwing `AccessDeniedException` if a `CUSTOMER` attempts to update return status.

---

## 4. Conclusion

The specification for `ReturnService`, `ReturnServiceImpl`, DTOs (`ReturnCreateRequest`, `ReturnStatusUpdateRequest`, `ReturnResponse`), exception handling, and notification integration has been comprehensively compiled in:
`/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2_5/m1_service_spec.md`.

It provides:
- Complete Java definitions for all 3 DTOs with Bean Validation annotations.
- Full Java interface for `ReturnService` with all 6 required method signatures.
- Detailed step-by-step algorithms, transition matrix, and validation rules for `ReturnServiceImpl`.
- Exact notification methods for `NotificationEventService`.
- 15 specific test scenarios mapped to `ReturnServiceImplTest.java` for the Worker.

---

## 5. Verification Method

To independently verify the specification and its compatibility with the existing codebase:

1. **Verify Existing Tests Pass**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test
   ```
   *Expected*: Tests run: 65, Failures: 0, Errors: 0.

2. **Verify Specification File Integrity**:
   Inspect `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2_5/m1_service_spec.md` to ensure:
   - DTOs contain all required fields (`orderId`, `type`, `reason`, `refundMode`, `exchangeSku`, `images`, `reverseCourier`, `reverseTrackingNumber`, `adminNotes`).
   - State machine transition matrix covers all transitions, invalid jumps, and terminal states.
   - Notification dispatch methods match `NotificationEventService` conventions.
   - 15 test scenarios in Section 8 cover all 6 acceptance criteria scenarios plus edge cases.

3. **Invalidation Conditions**:
   - If the business requirements allow orders in non-`DELIVERED` status to be returned, this specification is invalidated.
   - If returns allow multiple submissions per order, the unique order check is invalidated.
