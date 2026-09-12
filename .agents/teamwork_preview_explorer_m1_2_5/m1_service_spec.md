# SareeKart v3.0 Module 1: ReturnService & Business Logic Specification

**Milestone**: M1 (Backend Service Layer & Business Logic)  
**Author**: Explorer 2 (teamwork_preview_explorer_m1_2_5)  
**Target Audience**: Worker (Backend Implementer), Reviewer, Challenger, Auditor  
**Date**: 2026-09-11  

---

## 1. Executive Summary & Architectural Overview

This document specifies the complete business logic, service contract, Data Transfer Objects (DTOs), validation mechanics, state machine transitions, exception handling, and notification integration for **SareeKart v3.0 Module 1: Self-Service Customer Returns and Exchanges**.

### Architectural Positioning
- **Layer**: Service Layer (`com.sareekart.service`, `com.sareekart.service.impl`)
- **DTOs**: `com.sareekart.dto.request`, `com.sareekart.dto.response`
- **Dependencies**: `ReturnRequestRepository`, `OrderRepository`, `UserRepository`, `NotificationEventService`
- **Security & Authorization**: Spring Security 6, `User` entity (`Role.CUSTOMER`, `Role.MANAGER`, `Role.OWNER`, `Role.ADMIN`)
- **Design Pattern**: Interface-based service design (`ReturnService` interface + `ReturnServiceImpl` class), transactional boundary management (`@Transactional`), defensive input validation, standard `ApiResponse<T>` wrapping, and fail-safe asynchronous notification dispatch.

---

## 2. Data Transfer Objects (DTO) Specifications

### 2.1 `ReturnCreateRequest.java`
- **Target Path**: `backend/backend/src/main/java/com/sareekart/dto/request/ReturnCreateRequest.java`
- **Package**: `com.sareekart.dto.request`
- **Purpose**: Captures customer return or exchange submission from storefront modal (`ReturnRequestModal.jsx`).

```java
package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String type; // RETURN, EXCHANGE

    @NotBlank(message = "Reason is mandatory")
    private String reason; // COLOR_MISMATCH, ZARI_DEFECT, FABRIC_FEEL, INCORRECT_ITEM, SIZE_MISMATCH, OTHER

    private String comments;

    @NotBlank(message = "Refund mode is mandatory (ORIGINAL_PAYMENT, STORE_CREDIT, EXCHANGE_DRAPE)")
    private String refundMode;

    private String exchangeSku; // Mandatory if type == "EXCHANGE"

    private BigDecimal refundAmount; // Optional: defaults to order total if omitted

    @Size(max = 3, message = "Maximum 3 defect photos allowed")
    private List<String> images; // List of static URL strings, e.g. ["/uploads/return-photos/defect-uuid.jpg"]
}
```

### 2.2 `ReturnStatusUpdateRequest.java`
- **Target Path**: `backend/backend/src/main/java/com/sareekart/dto/request/ReturnStatusUpdateRequest.java`
- **Package**: `com.sareekart.dto.request`
- **Purpose**: Used by privileged staff (`OWNER`, `MANAGER`, `ADMIN`) in `/admin/returns` to moderate claims, schedule reverse pickup, complete refund, or reject claims.

```java
package com.sareekart.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnStatusUpdateRequest {

    @NotBlank(message = "Target status is mandatory (APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED)")
    private String status;

    private String reverseCourier; // Mandatory if status == "PICKUP_SCHEDULED"

    private String reverseTrackingNumber; // Mandatory if status == "PICKUP_SCHEDULED"

    private String adminNotes; // Mandatory if status == "REJECTED", optional otherwise

    private BigDecimal refundAmount; // Optional: allows staff to adjust approved refund amount
}
```

### 2.3 `ReturnResponse.java`
- **Target Path**: `backend/backend/src/main/java/com/sareekart/dto/response/ReturnResponse.java`
- **Package**: `com.sareekart.dto.response`
- **Purpose**: Consolidated return claim telemetry returned to customer order tracking drawers and admin moderation consoles.

```java
package com.sareekart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    
    // Customer profile telemetry
    private String customerEmail;
    private String customerName;
    private String customerMobile;
    
    // Financial telemetry
    private BigDecimal orderTotalAmount;
    private BigDecimal refundAmount;
    private String refundMode;
    
    // Claim taxonomy & details
    private String type; // RETURN, EXCHANGE
    private String reason;
    private String comments;
    private String status; // PENDING, APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED
    private String exchangeSku;
    private List<String> images;
    
    // Reverse logistics tracking
    private String reverseCourier;
    private String reverseTrackingNumber;
    
    // Staff audit notes
    private String adminNotes;
    
    // Timestamps & Eligibility metrics
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime orderDeliveredAt;
    private Long daysSinceDelivery;
}
```

---

## 3. `ReturnService` Interface Definition

- **Target Path**: `backend/backend/src/main/java/com/sareekart/service/ReturnService.java`
- **Package**: `com.sareekart.service`

```java
package com.sareekart.service;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;

import java.util.List;

public interface ReturnService {

    /**
     * Customer: Submit a self-service return or exchange request.
     *
     * Validates:
     * 1. Order ownership (order belongs to authenticated customer).
     * 2. Order status is DELIVERED.
     * 3. Delivery timestamp is within 7 calendar days.
     * 4. No duplicate return claim exists for this order.
     * 5. Exchange SKU presence if type is EXCHANGE.
     * 6. Up to 3 defect images maximum.
     *
     * Dispatches notification to customer and managers upon creation.
     */
    ReturnResponse createReturnRequest(ReturnCreateRequest request, User customer);

    /**
     * Customer: Retrieve all return/exchange claims submitted by authenticated customer.
     */
    List<ReturnResponse> getMyReturnRequests(User customer);

    /**
     * Customer/Staff: Retrieve return claim details for a specific order.
     * Enforces that the customer owns the order, unless requester is staff (OWNER, MANAGER, ADMIN).
     * Returns null if no return claim exists for the order (enabling UI conditional rendering).
     */
    ReturnResponse getReturnRequestByOrderId(Long orderId, User user);

    /**
     * Customer/Staff: Retrieve return claim by claim ID.
     * Enforces ownership for customers.
     */
    ReturnResponse getReturnRequestById(Long returnId, User user);

    /**
     * Staff (OWNER, MANAGER, ADMIN): Retrieve all return claims for admin console.
     * Supports optional status filtering: "ALL" (or null/empty), "PENDING", "APPROVED",
     * "PICKUP_SCHEDULED", "REJECTED", "COMPLETED".
     */
    List<ReturnResponse> getAllReturnsForAdmin(String statusFilter, User staffUser);

    /**
     * Staff (OWNER, MANAGER, ADMIN): Update return status and logistics details.
     *
     * Enforces:
     * 1. Staff role authorization (Role.CUSTOMER rejected with AccessDeniedException).
     * 2. Allowed lifecycle transitions:
     *    - PENDING -> APPROVED | REJECTED
     *    - APPROVED -> PICKUP_SCHEDULED | REJECTED
     *    - PICKUP_SCHEDULED -> COMPLETED | REJECTED
     * 3. Cannot transition out of terminal states (COMPLETED, REJECTED).
     * 4. If target status is PICKUP_SCHEDULED: reverseCourier and reverseTrackingNumber are mandatory.
     * 5. If target status is REJECTED: adminNotes is mandatory.
     *
     * Dispatches notification to customer on status progression.
     */
    ReturnResponse updateReturnStatus(Long returnId, ReturnStatusUpdateRequest request, User staffUser);
}
```

---

## 4. `ReturnServiceImpl` Comprehensive Specification & Business Rules

- **Target Path**: `backend/backend/src/main/java/com/sareekart/service/impl/ReturnServiceImpl.java`
- **Package**: `com.sareekart.service.impl`

### 4.1 Dependency Injection
`ReturnServiceImpl` requires the following Spring-managed components:
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnServiceImpl implements ReturnService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderRepository orderRepository;
    private final NotificationEventService notificationEventService;
    // Optional: ObjectMapper for JSON serialization of image lists if entity stores string
```

---

### 4.2 Business Rule 1: Order Ownership Enforcement
- **Requirement**: An order return can only be submitted or inspected by the user who placed the order (or by authorized staff).
- **Enforcement Logic**:
  ```java
  Order order = orderRepository.findById(orderId)
          .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

  if (order.getUser() == null || !order.getUser().getId().equals(customer.getId())) {
      throw new BadRequestException("You are not authorized to request a return for this order.");
  }
  ```
- **Claim Retrieval Ownership**:
  When a user queries `getReturnRequestByOrderId` or `getReturnRequestById`:
  ```java
  boolean isStaff = user.getRole() == Role.OWNER || 
                    user.getRole() == Role.MANAGER || 
                    user.getRole() == Role.ADMIN;

  if (!isStaff && !returnRequest.getUser().getId().equals(user.getId())) {
      throw new org.springframework.security.access.AccessDeniedException("Not authorised to perform this action");
  }
  ```

---

### 4.3 Business Rule 2: Order Status & 7-Day Cutoff Window Evaluation
- **Requirement**: Only orders in `OrderStatus.DELIVERED` are eligible. The delivery timestamp must be within `<= 7 calendar days` (7 * 24 hours = 168 hours).
- **Status Check**:
  ```java
  if (order.getStatus() != OrderStatus.DELIVERED) {
      throw new BadRequestException("Only delivered orders are eligible for return or exchange. Current order status: " + order.getStatus());
  }
  ```
- **Delivery Timestamp Evaluation Strategy**:
  `Order.java` may have a dedicated `deliveredAt` field (or will be added in M1), while existing seeded orders rely on `updatedAt` (or `createdAt` as a last fallback). The service must implement a hierarchical, NPE-safe resolver:
  ```java
  private LocalDateTime resolveDeliveryTimestamp(Order order) {
      if (order.getDeliveredAt() != null) {
          return order.getDeliveredAt();
      }
      if (order.getUpdatedAt() != null) {
          return order.getUpdatedAt();
      }
      if (order.getCreatedAt() != null) {
          return order.getCreatedAt();
      }
      return LocalDateTime.now();
  }
  ```
- **7-Day Cutoff Validation**:
  ```java
  LocalDateTime deliveryTime = resolveDeliveryTimestamp(order);
  LocalDateTime cutoff = deliveryTime.plusDays(7);

  if (LocalDateTime.now().isAfter(cutoff)) {
      throw new BadRequestException("Return window has expired. Orders are only eligible for return or exchange within 7 calendar days of delivery.");
  }
  ```
- **Edge Case Precision**:
  - Exactly 7 days 0 hours: `isAfter(cutoff)` evaluates to `false` -> **Allowed**.
  - 7 days + 1 minute: `isAfter(cutoff)` evaluates to `true` -> **Throws BadRequestException**.

---

### 4.4 Business Rule 3: Duplicate Return Request Prevention
- **Requirement**: Only one return or exchange claim can be active or recorded for a single order.
- **Enforcement Logic**:
  ```java
  if (returnRequestRepository.findByOrderId(order.getId()).isPresent()) {
      throw new BadRequestException("A return or exchange request has already been submitted for Order #" + order.getId() + ".");
  }
  ```
- **Concurrency & Database Layer**:
  In addition to the service-level check, `uk_return_requests_order` on `order_id` in `return_requests` guarantees that concurrent requests cannot violate uniqueness.

---

### 4.5 Business Rule 4: State Machine & Transition Rules

#### State Machine Transition Matrix
| Current Status | Allowed Target Status | Required Fields / Conditions | Prohibited Transitions |
|---|---|---|---|
| `PENDING` | `APPROVED` | None (admin notes optional) | Cannot jump to `PICKUP_SCHEDULED`, `COMPLETED` |
| `PENDING` | `REJECTED` | `adminNotes` is **mandatory** (non-blank) | Cannot transition to unapproved states |
| `APPROVED` | `PICKUP_SCHEDULED` | `reverseCourier` and `reverseTrackingNumber` are **mandatory** (non-blank) | Cannot jump directly to `COMPLETED` |
| `APPROVED` | `REJECTED` | `adminNotes` is **mandatory** (non-blank) | Cannot transition backwards to `PENDING` |
| `PICKUP_SCHEDULED` | `COMPLETED` | Finalizes refund amount (can be updated) | Cannot transition backwards to `PENDING` or `APPROVED` |
| `PICKUP_SCHEDULED` | `REJECTED` | `adminNotes` is **mandatory** | Cannot transition backwards |
| `COMPLETED` | *None* | **Terminal State**: throws `BadRequestException` | All transitions prohibited |
| `REJECTED` | *None* | **Terminal State**: throws `BadRequestException` | All transitions prohibited |

#### Transition Enforcement Implementation Snippet
```java
String currentStatus = returnRequest.getStatus();
String targetStatus = request.getStatus() != null ? request.getStatus().trim().toUpperCase() : "";

if (targetStatus.isEmpty()) {
    throw new BadRequestException("Target status is mandatory.");
}

// 1. Terminal state checks
if ("COMPLETED".equalsIgnoreCase(currentStatus)) {
    throw new BadRequestException("Cannot alter status of an already completed return request.");
}
if ("REJECTED".equalsIgnoreCase(currentStatus)) {
    throw new BadRequestException("Cannot alter status of an already rejected return request.");
}

// 2. Handle REJECTED transition
if ("REJECTED".equalsIgnoreCase(targetStatus)) {
    if (request.getAdminNotes() == null || request.getAdminNotes().trim().isEmpty()) {
        throw new BadRequestException("Mandatory explanation note required when rejecting a return claim.");
    }
    returnRequest.setStatus("REJECTED");
    returnRequest.setAdminNotes(request.getAdminNotes().trim());
    return;
}

// 3. Status-specific forward progressions
switch (currentStatus) {
    case "PENDING":
        if ("APPROVED".equalsIgnoreCase(targetStatus)) {
            returnRequest.setStatus("APPROVED");
            if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                returnRequest.setAdminNotes(request.getAdminNotes().trim());
            }
        } else {
            throw new BadRequestException("Invalid state transition from PENDING to " + targetStatus + ". Claim must first be APPROVED.");
        }
        break;

    case "APPROVED":
        if ("PICKUP_SCHEDULED".equalsIgnoreCase(targetStatus)) {
            if (request.getReverseCourier() == null || request.getReverseCourier().trim().isEmpty() ||
                request.getReverseTrackingNumber() == null || request.getReverseTrackingNumber().trim().isEmpty()) {
                throw new BadRequestException("Reverse courier and tracking number are required to schedule pickup.");
            }
            returnRequest.setStatus("PICKUP_SCHEDULED");
            returnRequest.setReverseCourier(request.getReverseCourier().trim());
            returnRequest.setReverseTrackingNumber(request.getReverseTrackingNumber().trim());
            if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                returnRequest.setAdminNotes(request.getAdminNotes().trim());
            }
        } else {
            throw new BadRequestException("Invalid state transition from APPROVED to " + targetStatus);
        }
        break;

    case "PICKUP_SCHEDULED":
        if ("COMPLETED".equalsIgnoreCase(targetStatus)) {
            returnRequest.setStatus("COMPLETED");
            if (request.getRefundAmount() != null) {
                returnRequest.setRefundAmount(request.getRefundAmount());
            }
            if (request.getAdminNotes() != null && !request.getAdminNotes().trim().isEmpty()) {
                returnRequest.setAdminNotes(request.getAdminNotes().trim());
            }
        } else {
            throw new BadRequestException("Invalid state transition from PICKUP_SCHEDULED to " + targetStatus);
        }
        break;

    default:
        throw new BadRequestException("Unknown current status: " + currentStatus);
}
```

---

### 4.6 Business Rule 5: Role-Based Access Control & Staff Moderation Validation
- **Requirement**: Only users with roles `OWNER`, `MANAGER`, or `ADMIN` may call `updateReturnStatus` or `getAllReturnsForAdmin`.
- **Validation Guard**:
  ```java
  private void validateStaffRole(User user) {
      if (user == null || user.getRole() == null || user.getRole() == Role.CUSTOMER) {
          throw new org.springframework.security.access.AccessDeniedException("Not authorised to perform this action");
      }
  }
  ```
- This defense-in-depth ensures that even if invoked directly or in tests without Spring Security filters, unauthorized callers receive an `AccessDeniedException` which maps to HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.

---

### 4.7 Mapping Helper (`toResponse`)
The service converts `ReturnRequest` entities to `ReturnResponse` DTOs:
```java
private ReturnResponse toResponse(ReturnRequest entity) {
    if (entity == null) {
        return null;
    }

    Order order = entity.getOrder();
    User user = entity.getUser();

    // Calculate delivery metrics if order is available
    LocalDateTime deliveredTime = order != null ? resolveDeliveryTimestamp(order) : null;
    Long daysSinceDelivery = null;
    if (deliveredTime != null) {
        daysSinceDelivery = java.time.temporal.ChronoUnit.DAYS.between(deliveredTime, LocalDateTime.now());
    }

    String customerName = null;
    String customerEmail = null;
    String customerMobile = null;
    if (user != null) {
        customerEmail = user.getEmail();
        customerMobile = user.getMobile();
        customerName = (user.getFirstName() + " " + (user.getLastName() != null ? user.getLastName() : "")).trim();
    }

    // Images handling
    List<String> imageList = parseImages(entity.getImages());

    return ReturnResponse.builder()
            .id(entity.getId())
            .orderId(order != null ? order.getId() : (entity.getOrderId() != null ? entity.getOrderId() : null))
            .userId(user != null ? user.getId() : (entity.getUserId() != null ? entity.getUserId() : null))
            .customerEmail(customerEmail)
            .customerName(customerName)
            .customerMobile(customerMobile)
            .orderTotalAmount(order != null ? order.getTotalAmount() : null)
            .type(entity.getType())
            .reason(entity.getReason())
            .comments(entity.getComments())
            .status(entity.getStatus())
            .images(imageList)
            .refundAmount(entity.getRefundAmount())
            .refundMode(entity.getRefundMode())
            .exchangeSku(entity.getExchangeSku())
            .reverseCourier(entity.getReverseCourier())
            .reverseTrackingNumber(entity.getReverseTrackingNumber())
            .adminNotes(entity.getAdminNotes())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .orderDeliveredAt(deliveredTime)
            .daysSinceDelivery(daysSinceDelivery)
            .build();
}
```

*Note on Images Storage*:
If `ReturnRequest` uses `@ElementCollection List<String> images`, `entity.getImages()` returns `List<String>` directly. If `ReturnRequest` stores `String images` as JSON text, `parseImages` can deserialize using `ObjectMapper` (falling back to an empty list or splitting on commas). Supporting both makes the implementation resilient.

---

## 5. Notification Integration via `NotificationEventService`

### 5.1 Enhancing `NotificationEventService.java`
`NotificationEventService` currently handles `ORDER_PLACED`, `SHIPMENT_DISPATCHED`, `ORDER_DELIVERED`, `LOW_STOCK`, `STOCK_TRANSFER`, and `REVIEW_SUBMITTED`. Add the following two dedicated methods to `com.sareekart.service.NotificationEventService`:

```java
    /**
     * Dispatches notifications when a customer submits a return or exchange claim.
     * 1. Informs customer that the request was registered and is under review.
     * 2. Alerts store managers in /admin/returns queue.
     */
    @Transactional
    public Notification notifyReturnSubmitted(ReturnRequest returnRequest, Order order, User customer) {
        // Customer notification
        Notification customerNotif = Notification.builder()
                .userId(customer.getId())
                .title("Return Request Submitted #" + returnRequest.getId())
                .message("Your " + returnRequest.getType().toLowerCase() + " request for Order #" + order.getId() + " has been received and is pending review.")
                .type("RETURN_REQUESTED")
                .linkUrl("/orders")
                .build();
        notificationRepository.save(customerNotif);

        // Staff notification
        Notification staffNotif = Notification.builder()
                .targetRole("MANAGER")
                .title("New Return Claim: Order #" + order.getId())
                .message(customer.getEmail() + " submitted a " + returnRequest.getType() + " claim for Order #" + order.getId() + " (" + returnRequest.getReason() + ").")
                .type("RETURN_REQUESTED")
                .linkUrl("/admin/returns")
                .build();
        notificationRepository.save(staffNotif);

        log.info("Dispatched return requested notifications for order #{}", order.getId());
        return customerNotif;
    }

    /**
     * Dispatches a notification to the customer when the return claim status changes.
     */
    @Transactional
    public Notification notifyReturnStatusUpdated(ReturnRequest returnRequest) {
        Long customerId = returnRequest.getUser() != null ? returnRequest.getUser().getId() : returnRequest.getUserId();
        Long orderId = returnRequest.getOrder() != null ? returnRequest.getOrder().getId() : returnRequest.getOrderId();
        String status = returnRequest.getStatus();

        String title;
        String message;
        String type;

        switch (status) {
            case "APPROVED":
                title = "Return Claim Approved: Order #" + orderId;
                message = "Your return request for Order #" + orderId + " has been approved. Doorstep pickup will be scheduled shortly.";
                type = "RETURN_APPROVED";
                break;
            case "PICKUP_SCHEDULED":
                title = "Reverse Pickup Scheduled: Order #" + orderId;
                message = "Reverse pickup scheduled via " + returnRequest.getReverseCourier() + " (AWB: " + returnRequest.getReverseTrackingNumber() + ") for Order #" + orderId + ".";
                type = "RETURN_PICKUP_SCHEDULED";
                break;
            case "COMPLETED":
                title = "Return Completed: Order #" + orderId;
                message = "Your return for Order #" + orderId + " is completed. Refund of ₹" + returnRequest.getRefundAmount() + " processed via " + returnRequest.getRefundMode() + ".";
                type = "RETURN_COMPLETED";
                break;
            case "REJECTED":
                title = "Return Request Rejected: Order #" + orderId;
                message = "Your return request for Order #" + orderId + " was rejected. Reason: " + returnRequest.getAdminNotes();
                type = "RETURN_REJECTED";
                break;
            default:
                title = "Return Status Updated: Order #" + orderId;
                message = "Your return claim for Order #" + orderId + " has been updated to " + status + ".";
                type = "RETURN_STATUS_UPDATED";
                break;
        }

        Notification notif = Notification.builder()
                .userId(customerId)
                .title(title)
                .message(message)
                .type(type)
                .linkUrl("/orders")
                .build();

        log.info("Dispatched return status notification ({}) for return claim #{}", status, returnRequest.getId());
        return notificationRepository.save(notif);
    }
```

### 5.2 Resilient Try-Catch Pattern in Service
In `ReturnServiceImpl`, call notification triggers inside `try-catch` blocks:
```java
try {
    notificationEventService.notifyReturnSubmitted(savedClaim, order, customer);
} catch (Exception e) {
    log.warn("Failed to dispatch return submission notification: {}", e.getMessage());
}
```
This guarantees that an error in notification generation will never cause a rollback of the primary return request transaction.

---

## 6. Exception Handling Strategy & Standardized Responses

SareeKart's `GlobalExceptionHandler` already defines standard handlers for:

| Exception Class | HTTP Status | Response Payload Format |
|---|---|---|
| `BadRequestException` | **400 Bad Request** | `{"success":false,"message":"<error message>"}` |
| `ResourceNotFoundException` | **404 Not Found** | `{"success":false,"message":"<Resource> not found with <field>: '<value>'"}` |
| `AccessDeniedException` | **403 Forbidden** | `{"success":false,"message":"Not authorised to perform this action"}` |
| `MethodArgumentNotValidException` | **400 Bad Request** | `{"success":false,"message":"Validation failed","data":{...field errors...}}` |

### Exception Rules in ReturnService:
1. **Invalid Order Status / Expired Window / Duplicate Claim / Missing Courier Info / Missing Rejection Reason**:
   - Always throw `com.sareekart.exception.BadRequestException` with a descriptive message.
2. **Missing Order / Missing ReturnRequest**:
   - Always throw `com.sareekart.exception.ResourceNotFoundException`.
3. **Customer attempting to modify status / access another customer's claim**:
   - Throw `org.springframework.security.access.AccessDeniedException("Not authorised to perform this action")` (or `BadRequestException` on cross-customer creation).

---

## 7. Step-by-Step Implementation Guide for the Worker

The Worker should follow this exact sequence:

1. **Step 1: Create Request and Response DTOs**
   - Create `ReturnCreateRequest.java` in `com.sareekart.dto.request`
   - Create `ReturnStatusUpdateRequest.java` in `com.sareekart.dto.request`
   - Create `ReturnResponse.java` in `com.sareekart.dto.response`

2. **Step 2: Create `ReturnService` Interface**
   - Create `ReturnService.java` in `com.sareekart.service` with all 6 signatures described in Section 3.

3. **Step 3: Update `NotificationEventService.java`**
   - Add `notifyReturnSubmitted` and `notifyReturnStatusUpdated` to `com.sareekart.service.NotificationEventService`.

4. **Step 4: Implement `ReturnServiceImpl.java`**
   - Create `ReturnServiceImpl.java` in `com.sareekart.service.impl`.
   - Implement `createReturnRequest`, `getMyReturnRequests`, `getReturnRequestByOrderId`, `getReturnRequestById`, `getAllReturnsForAdmin`, `updateReturnStatus`.
   - Implement private helper methods: `resolveDeliveryTimestamp(Order)`, `validateStaffRole(User)`, `toResponse(ReturnRequest)`.

5. **Step 5: Co-locate and Run Unit Tests**
   - Co-locate unit tests in `src/test/java/com/sareekart/service/ReturnServiceImplTest.java`.
   - Run `./mvnw test -Dtest=ReturnServiceImplTest`.
   - Run full regression `./mvnw test`.

---

## 8. Unit Test Plan: `ReturnServiceImplTest.java`

Co-located in `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java`.
Uses JUnit 5 and Mockito (`@ExtendWith(MockitoExtension.class)`).

### Test Case Matrix

```java
@ExtendWith(MockitoExtension.class)
public class ReturnServiceImplTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private NotificationEventService notificationEventService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customer;
    private User anotherCustomer;
    private User manager;
    private User admin;
    private Order deliveredOrder;
```

#### Test Scenarios to Implement:

1. **`testCreateReturnRequest_Success_WhenDeliveredWithin7Days`**:
   - Order delivered 3 days ago (`deliveredAt = now().minusDays(3)`).
   - Mock `orderRepository.findById(100L)` returns `Optional.of(deliveredOrder)`.
   - Mock `returnRequestRepository.findByOrderId(100L)` returns `Optional.empty()`.
   - Execute `returnService.createReturnRequest(req, customer)`.
   - Verify: `returnRequestRepository.save(any())` called, status is `PENDING`, notification dispatched.

2. **`testCreateReturnRequest_Fails_WhenOrderNotDelivered`**:
   - Order status is `OrderStatus.SHIPPED`.
   - Assert `assertThrows(BadRequestException.class, () -> returnService.createReturnRequest(req, customer))`.
   - Verify error message contains `"DELIVERED"` or `"delivered"`.
   - Verify `returnRequestRepository.save` is NEVER called.

3. **`testCreateReturnRequest_Fails_WhenDeliveredMoreThan7DaysAgo`**:
   - Order delivered 9 days ago (`deliveredAt = now().minusDays(9)`).
   - Assert `BadRequestException` thrown with `"expired"` or `"7 days"`.

4. **`testCreateReturnRequest_Fails_WhenDuplicateRequestExists`**:
   - Mock `returnRequestRepository.findByOrderId(100L)` returns `Optional.of(existingReturn)`.
   - Assert `BadRequestException` thrown with `"already"` or `"duplicate"`.

5. **`testCreateReturnRequest_Fails_WhenOrderDoesNotBelongToCustomer`**:
   - Order belongs to `anotherCustomer` (ID 200). Customer (ID 100) submits return.
   - Assert `BadRequestException` or `AccessDeniedException` thrown.

6. **`testCreateReturnRequest_ExchangeRequiresSku`**:
   - Type is `EXCHANGE` but `exchangeSku` is null or blank.
   - Assert `BadRequestException` thrown with `"Exchange SKU"`.

7. **`testStaff_CanApprovePendingReturn`**:
   - Return in `PENDING` status. Manager calls `updateReturnStatus(1L, updateReq, manager)` with status `APPROVED`.
   - Verify status becomes `APPROVED`, notification dispatched.

8. **`testStaff_CanSchedulePickup_WithCourierAndAwb`**:
   - Return in `APPROVED` status. Admin calls `updateReturnStatus` with status `PICKUP_SCHEDULED`, courier `Delhivery`, AWB `DLH-89214`.
   - Verify status becomes `PICKUP_SCHEDULED`, courier and tracking number saved.

9. **`testStaff_SchedulePickup_FailsWithoutCourierOrAwb`**:
   - Status `APPROVED` -> target `PICKUP_SCHEDULED`, but courier or AWB missing.
   - Assert `BadRequestException` thrown.

10. **`testStaff_CanRejectReturn_WithMandatoryReason`**:
    - Status `PENDING` -> target `REJECTED`, `adminNotes = "Item altered by customer"`.
    - Verify status becomes `REJECTED`, admin notes saved.

11. **`testStaff_RejectReturn_FailsWithoutReason`**:
    - Status `PENDING` -> target `REJECTED`, `adminNotes = null`.
    - Assert `BadRequestException` thrown with `"Mandatory explanation note required"`.

12. **`testStaff_CannotTransitionFromCompletedOrRejected`**:
    - Status `COMPLETED` -> target `APPROVED`.
    - Assert `BadRequestException` thrown.

13. **`testCustomer_CannotCallUpdateReturnStatus_Forbidden`**:
    - Customer calls `updateReturnStatus`.
    - Assert `AccessDeniedException` thrown.

14. **`testGetReturnRequestByOrderId_ReturnsNull_WhenNotFound`**:
    - Order exists and belongs to customer. No return submitted.
    - Result is `null`.

15. **`testGetReturnRequestByOrderId_ThrowsAccessDenied_WhenCustomerDoesNotOwnOrder`**:
    - Customer B requests return info for Customer A's order.
    - Assert `AccessDeniedException` thrown.

---

## 9. Verification Commands for Worker & Reviewers

```bash
# 1. Run target ReturnServiceImplTest in isolation
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
./mvnw test -Dtest=ReturnServiceImplTest

# 2. Run full backend test regression suite (must maintain 100% pass rate)
./mvnw test

# 3. Check disk health headroom
/Users/chaitanyachaitu/scripts/check_disk_health.sh
```
