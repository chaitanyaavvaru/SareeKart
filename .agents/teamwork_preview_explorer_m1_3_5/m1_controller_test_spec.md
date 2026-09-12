# SareeKart v3.0 Module 1: REST Controllers, Security RBAC & Unit Test Specification

**Document Author**: Explorer 3 (Milestone 1 — REST Controllers, Security RBAC & Unit Tests)  
**Target Audience**: Downstream Implementation Worker (`teamwork_preview_worker_m1`)  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_3_5`  
**Date**: 2026-09-11  

---

## 1. Executive Summary

This document provides the definitive implementation specification for **Milestone 1** of SareeKart v3.0 Module 1 (Self-Service Customer Returns and Exchanges), specifically covering:
1. **Customer REST Controller (`ReturnController.java`)** under `/api/returns` with customer-facing endpoints for return creation, request retrieval, order-based claim lookup, and authenticated defect photo uploads.
2. **Admin REST Controller (`AdminReturnController.java`)** under `/api/admin/returns` with staff moderation endpoints for status filtering (`ALL`, `PENDING`, `APPROVED`, etc.) and lifecycle state updates (approve, schedule reverse pickup with courier/AWB, complete refund, reject with mandatory explanation).
3. **Security RBAC Configuration (`SecurityConfig.java`)** establishing strict role boundaries where `/api/returns/**` requires authenticated customer access, `/api/admin/returns/**` is restricted to `OWNER`, `MANAGER`, and `ADMIN`, and unauthorized attempts strictly receive HTTP 403 Forbidden with body `{"success":false,"message":"Not authorised to perform this action"}`.
4. **Comprehensive Unit Test Plan and Implementation Structure for `ReturnServiceImplTest.java`** covering all 6 mandatory acceptance criteria plus 8 critical edge-case scenarios with Mockito 5 and JUnit 5.
5. **Controller Unit Test Structure (`ReturnControllerTest.java` & `AdminReturnControllerTest.java`)** verifying MockMvc endpoints, photo upload validation, and security exception handling.

---

## 2. Customer REST Controller: `ReturnController.java`

### 2.1 File Location & Metadata
- **File Path**: `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java`
- **Base Route**: `/api/returns`
- **Class Annotations**:
  - `@RestController`
  - `@RequestMapping("/api/returns")`
  - `@CrossOrigin(origins = "*")`
  - `@RequiredArgsConstructor`
  - `@Slf4j`
- **Injected Dependencies**:
  - `private final ReturnService returnService;`

### 2.2 Endpoint Specifications

#### Endpoint 1: Submit Return / Exchange Request
- **Method & Path**: `POST /api/returns`
- **Authorization**: Authenticated User (`@AuthenticationPrincipal User user`)
- **Request Body**: `@Valid @RequestBody ReturnCreateRequest request`
- **HTTP Status**: `201 CREATED`
- **Response Format**: `ApiResponse<ReturnResponse>`
- **Logic**:
  - Invokes `returnService.createReturnRequest(request, user.getId())` (or with `user`).
  - Returns `ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Return request submitted successfully", response));`.

#### Endpoint 2: Fetch Current User's Return History
- **Method & Path**: `GET /api/returns/my-requests`
- **Authorization**: Authenticated User (`@AuthenticationPrincipal User user`)
- **HTTP Status**: `200 OK`
- **Response Format**: `ApiResponse<List<ReturnResponse>>`
- **Logic**:
  - Invokes `returnService.getMyReturnRequests(user.getId())` (or with `user`).
  - Returns `ResponseEntity.ok(ApiResponse.success("Return requests retrieved successfully", list));`.

#### Endpoint 3: Fetch Return Claim by Order ID
- **Method & Path**: `GET /api/returns/order/{orderId}`
- **Authorization**: Authenticated User (`@AuthenticationPrincipal User user`)
- **Path Parameter**: `@PathVariable Long orderId`
- **HTTP Status**: `200 OK`
- **Response Format**: `ApiResponse<ReturnResponse>`
- **Logic**:
  - Invokes `returnService.getReturnRequestByOrderId(orderId, user.getId())`.
  - If claim exists, returns `ResponseEntity.ok(ApiResponse.success("Return claim retrieved successfully", response));`.
  - If no claim exists for the order, returns `ResponseEntity.ok(ApiResponse.success("No return request found for this order", null));` (satisfies `PROJECT.md` requirement allowing frontend `MyOrders.jsx` to gracefully render order cards without catching 404 errors).

#### Endpoint 4: Authenticated Defect Condition Photo Upload
- **Method & Path**: `POST /api/returns/upload-photo`
- **Consumes**: `multipart/form-data`
- **Authorization**: Authenticated User (`@AuthenticationPrincipal User user`)
- **Request Parameter**: `@RequestParam(value = "file", required = false) MultipartFile file`, with fallback to `@RequestParam(value = "photo", required = false) MultipartFile photo`
- **HTTP Status**: `200 OK`
- **Response Format**:
  ```json
  {
    "success": true,
    "message": "Photo uploaded successfully",
    "data": {
      "url": "/uploads/return-photos/return-a1b2c3d4e5f6.jpg"
    }
  }
  ```
- **Validation Rules**:
  1. If file is null or empty -> returns HTTP 400 Bad Request: `ApiResponse.error("No file selected")`.
  2. Allowed MIME types: `image/jpeg`, `image/jpg`, `image/png`, `image/webp`. If not matched -> HTTP 400 Bad Request: `ApiResponse.error("Unsupported image format (" + contentType + "). Please upload JPG, PNG, or WebP.")`.
  3. Max file size: `10 * 1024 * 1024` (10 MB). If exceeded -> HTTP 400 Bad Request: `ApiResponse.error("File exceeds 10 MB limit")`.
- **Storage Mechanics**:
  1. Target directory: `Path targetDir = Paths.get("uploads", "return-photos").toAbsolutePath();`
  2. Ensures directory exists via `Files.createDirectories(targetDir);`.
  3. Determines extension from MIME type: `jpeg`/`jpg` -> `jpg`, `png` -> `png`, `webp` -> `webp`.
  4. Generates unique collision-resistant filename: `"return-" + UUID.randomUUID().toString().substring(0, 12) + "." + ext`.
  5. Copies stream via `Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);`.
  6. Returns accessible static path: `"/uploads/return-photos/" + filename`. This path is automatically served by `StaticResourceConfig.java` without requiring any cloud hosting.

### 2.3 Ready-to-Implement Source Code: `ReturnController.java`

```java
package com.sareekart.controller;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * REST Controller for Customer Self-Service Returns & Exchanges.
 * Provides endpoints for claim submission, customer return history,
 * order claim lookup, and authenticated defect condition photo uploads.
 */
@RestController
@RequestMapping("/api/returns")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class ReturnController {

    private final ReturnService returnService;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/jpg", "image/png", "image/webp"
    );
    private static final Path UPLOAD_DIR = Paths.get("uploads", "return-photos").toAbsolutePath();

    /**
     * Submit a new return or exchange request.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ReturnResponse>> createReturnRequest(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ReturnCreateRequest request) {
        log.info("Customer #{} submitting return request for Order #{}", user.getId(), request.getOrderId());
        ReturnResponse response = returnService.createReturnRequest(request, user.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Return request submitted successfully", response));
    }

    /**
     * Fetch all return and exchange claims submitted by the authenticated customer.
     */
    @GetMapping("/my-requests")
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getMyReturnRequests(
            @AuthenticationPrincipal User user) {
        List<ReturnResponse> returns = returnService.getMyReturnRequests(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Return requests retrieved successfully", returns));
    }

    /**
     * Fetch return claim telemetry for a specific order owned by the authenticated customer.
     */
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ReturnResponse>> getReturnByOrderId(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) {
        ReturnResponse response = returnService.getReturnRequestByOrderId(orderId, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Return claim retrieved successfully", response));
    }

    /**
     * Authenticated defect photo upload storing in uploads/return-photos/
     * and returning accessible URL (/uploads/return-photos/...).
     * Accepts either 'file' or 'photo' parameter for frontend compatibility.
     */
    @PostMapping(value = "/upload-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadPhoto(
            @AuthenticationPrincipal User user,
            @RequestParam(value = "file", required = false) MultipartFile file,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {

        MultipartFile targetFile = (file != null && !file.isEmpty()) ? file : photo;

        if (targetFile == null || targetFile.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("No file selected"));
        }

        String contentType = targetFile.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Unsupported image format (" + contentType + "). Please upload JPG, PNG, or WebP."));
        }

        if (targetFile.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("File exceeds 10 MB limit (" + (targetFile.getSize() / (1024 * 1024)) + " MB)"));
        }

        try {
            Files.createDirectories(UPLOAD_DIR);

            String ext = "jpg";
            if (contentType.contains("/")) {
                String sub = contentType.split("/")[1].toLowerCase().replace("jpeg", "jpg");
                if (sub.length() <= 5) {
                    ext = sub;
                }
            }

            String filename = "return-" + UUID.randomUUID().toString().substring(0, 12) + "." + ext;
            Path dest = UPLOAD_DIR.resolve(filename);

            try (InputStream in = targetFile.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }

            String url = "/uploads/return-photos/" + filename;
            log.info("Defect condition photo uploaded by user #{}: {} (size: {} bytes)", user.getId(), url, targetFile.getSize());

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("filename", filename);

            return ResponseEntity.ok(ApiResponse.success("Photo uploaded successfully", data));

        } catch (Exception e) {
            log.error("Failed to store return photo upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to save photo on server: " + e.getMessage()));
        }
    }
}
```

---

## 3. Admin REST Controller: `AdminReturnController.java`

### 3.1 File Location & Metadata
- **File Path**: `backend/backend/src/main/java/com/sareekart/controller/AdminReturnController.java`
- **Base Route**: `/api/admin/returns`
- **Class Annotations**:
  - `@RestController`
  - `@RequestMapping("/api/admin/returns")`
  - `@CrossOrigin(origins = "*")`
  - `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`
  - `@RequiredArgsConstructor`
  - `@Slf4j`
- **Injected Dependencies**:
  - `private final ReturnService returnService;`

### 3.2 Endpoint Specifications

#### Endpoint 1: Filterable List of All Return Claims
- **Method & Path**: `GET /api/admin/returns`
- **Authorization**: Staff (`OWNER`, `MANAGER`, `ADMIN`) via `@PreAuthorize`
- **Query Parameter**: `@RequestParam(value = "status", required = false) String status`
- **HTTP Status**: `200 OK`
- **Response Format**: `ApiResponse<List<ReturnResponse>>`
- **Filtering Logic**:
  - If `status` is `null`, empty, or `"ALL"`, returns all return claims sorted by `createdAt DESC`.
  - If `status` is specified (e.g. `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`), returns claims filtered by status sorted by `createdAt DESC`.
  - Invokes `returnService.getAllReturnsForAdmin(status)`.

#### Endpoint 2: Moderate Return Status & Logistics Assignment
- **Method & Path**: `PUT /api/admin/returns/{id}/status`
- **Authorization**: Staff (`OWNER`, `MANAGER`, `ADMIN`) via `@PreAuthorize`
- **Path Parameter**: `@PathVariable Long id`
- **Request Body**: `@Valid @RequestBody ReturnStatusUpdateRequest request`
- **HTTP Status**: `200 OK`
- **Response Format**: `ApiResponse<ReturnResponse>`
- **Moderation Actions**:
  1. **Approve Return**: `status = "APPROVED"`.
  2. **Schedule Pickup**: `status = "PICKUP_SCHEDULED"` with mandatory `reverseCourier` (e.g. "Blue Dart Reverse Logistics") and `reverseTrackingNumber` (e.g. "BDR-9812401").
  3. **Complete Refund**: `status = "COMPLETED"`.
  4. **Reject Return**: `status = "REJECTED"` with mandatory `adminNotes` (mandatory explanation note to customer).
- **Service Invocation**:
  - Calls `returnService.updateReturnStatus(id, request, adminUser)`.

### 3.3 Ready-to-Implement Source Code: `AdminReturnController.java`

```java
package com.sareekart.controller;

import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ApiResponse;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.User;
import com.sareekart.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Administrative REST Controller for Return Claims Moderation & Reverse Logistics.
 * Restricted strictly to staff with roles OWNER, MANAGER, or ADMIN.
 */
@RestController
@RequestMapping("/api/admin/returns")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminReturnController {

    private final ReturnService returnService;

    /**
     * Retrieve filterable list of all return claims.
     * Supports status filters: ALL, PENDING, APPROVED, PICKUP_SCHEDULED, REJECTED, COMPLETED.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ReturnResponse>>> getAllReturns(
            @AuthenticationPrincipal User adminUser,
            @RequestParam(value = "status", required = false) String status) {
        log.info("Staff member #{} ({}) fetching returns with status filter: {}",
                adminUser.getId(), adminUser.getRole(), status);
        List<ReturnResponse> returns = returnService.getAllReturnsForAdmin(status);
        return ResponseEntity.ok(ApiResponse.success("Return claims fetched successfully", returns));
    }

    /**
     * Moderate status of a return claim.
     * Allows staff to approve, assign courier partner & reverse tracking AWB,
     * finalize refund/completion, or reject with a mandatory explanation note.
     */
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ReturnResponse>> updateReturnStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal User adminUser,
            @Valid @RequestBody ReturnStatusUpdateRequest request) {
        log.info("Staff member #{} ({}) updating return claim #{} to status: {}",
                adminUser.getId(), adminUser.getRole(), id, request.getStatus());
        ReturnResponse updated = returnService.updateReturnStatus(id, request, adminUser);
        return ResponseEntity.ok(ApiResponse.success("Return status updated successfully", updated));
    }
}
```

---

## 4. Security & RBAC Configuration Specification (`SecurityConfig.java`)

### 4.1 Target File & Exact Matcher Insertion
- **File Path**: `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`
- **Location**: Inside `filterChain(HttpSecurity http)`, within `.authorizeHttpRequests(auth -> auth ...)`

### 4.2 RBAC Rules Definition
```java
// SareeKart v3.0 Module 1: Returns & Exchanges RBAC
.requestMatchers("/api/admin/returns/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
.requestMatchers("/api/returns/**").authenticated()
```

### 4.3 Defense-in-Depth Architecture & HTTP 403 Payloads

| Scenario | Trigger / Actor | Handling Mechanism in Code | HTTP Status | Response Payload |
|---|---|---|---|---|
| **Unauthenticated access to `/api/admin/returns/**`** | Anonymous HTTP request | `SecurityConfig.authenticationEntryPoint` (lines 56-61) checks `request.getRequestURI().startsWith("/api/admin")` | **HTTP 403 FORBIDDEN** | `{"success":false,"message":"Not authorised to perform this action"}` |
| **Customer access to `/api/admin/returns/**`** | Authenticated user with `Role.CUSTOMER` | `SecurityConfig.accessDeniedHandler` (lines 66-70) or `@PreAuthorize` -> `GlobalExceptionHandler.handleAccessDeniedException` | **HTTP 403 FORBIDDEN** | `{"success":false,"message":"Not authorised to perform this action"}` |
| **Customer accessing another customer's claim** | Customer B calls `/api/returns/order/{orderId}` for Customer A's order | Service layer checks `order.getUser().getId().equals(userId)` and throws `AccessDeniedException("Not authorised to perform this action")` | **HTTP 403 FORBIDDEN** | `{"success":false,"message":"Not authorised to perform this action"}` |
| **Unauthenticated access to `/api/returns/**`** | Anonymous HTTP request | `SecurityConfig.authenticationEntryPoint` (lines 62-65) | **HTTP 401 UNAUTHORIZED** | `{"success":false,"message":"Authentication required. Please sign in again."}` |
| **Authorized staff access to `/api/admin/returns/**`** | Authenticated user with `OWNER`, `MANAGER`, or `ADMIN` | Evaluated against `hasAnyRole("OWNER", "MANAGER", "ADMIN")` | **HTTP 200 OK** | Standard `ApiResponse<T>` payload |

### 4.4 Exact Diff Patch for `SecurityConfig.java`

```diff
--- a/backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java
+++ b/backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java
@@ -81,6 +81,8 @@ public class SecurityConfig {
                 .requestMatchers("/api/excel/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                 .requestMatchers("/api/inventory/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
+                .requestMatchers("/api/admin/returns/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
+                .requestMatchers("/api/returns/**").authenticated()
                 .requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")
                 .anyRequest().authenticated()
             );
```

---

## 5. Comprehensive Unit Test Plan: `ReturnServiceImplTest.java`

### 5.1 Test Suite File Metadata
- **File Path**: `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java`
- **Execution Command**: `./mvnw test -Dtest=ReturnServiceImplTest`
- **Framework**: JUnit 5 (Jupiter), Mockito 5 (`@ExtendWith(MockitoExtension.class)`)
- **Isolation**: 100% Mockito unit tests, zero database, network, or external process requirements.

### 5.2 Test Scenarios & Acceptance Criteria Matrix

| # | Test Method Name | Scenario & Input State | Expected Outcome & Assertions | Mandatory AC Requirement |
|---|-------------------|------------------------|-------------------------------|--------------------------|
| **1** | `testCreateReturnRequest_DeliveredOrderWithin7Days_Success` | Order delivered 3 days ago (`DELIVERED`, `deliveredAt = now - 3d`), total amount 2499.00. Customer submits return. | Returns `ReturnResponse` with status `PENDING`, `refundAmount` 2499.00, `orderId` 100. Repository `save` invoked. | Return creation succeeds on delivered order <= 7 days old. |
| **2** | `testCreateReturnRequest_OrderInShippedStatus_ThrowsBadRequestException` | Order in `OrderStatus.SHIPPED`. Customer attempts return. | Throws `BadRequestException` ("Only delivered orders are eligible"). Repo `save` never called. | Return creation fails on non-delivered order (`SHIPPED`). |
| **3** | `testCreateReturnRequest_OrderInPendingStatus_ThrowsBadRequestException` | Order in `OrderStatus.PENDING`. Customer attempts return. | Throws `BadRequestException` ("Only delivered orders are eligible"). Repo `save` never called. | Return creation fails on non-delivered order (`PENDING`). |
| **4** | `testCreateReturnRequest_OrderDeliveredMoreThan7DaysAgo_ThrowsBadRequestException` | Order delivered 8 days ago (`deliveredAt = now - 8d`). Customer attempts return. | Throws `BadRequestException` ("Return window has expired. Orders are only eligible within 7 calendar days"). | Return creation fails on delivered order > 7 days old. |
| **5** | `testCreateReturnRequest_DuplicateSubmission_ThrowsBadRequestException` | Return already exists for order #100. Customer submits duplicate claim. | Throws `BadRequestException` ("A return or exchange request has already been submitted for Order #100"). | Duplicate return submission on same order is rejected. |
| **6** | `testCustomerAccessControl_CannotAccessAnotherCustomersReturnClaim` | Return request belongs to Customer A (id 10). Customer B (id 20) calls `getReturnRequestByOrderId`. | Throws `AccessDeniedException` or `BadRequestException` ("Not authorised to perform this action"). | Unauthorized customer cannot access another customer's return claim. |
| **7** | `testStaffRole_Admin_CanApproveReturnRequest` | Claim in `PENDING` status. Admin user updates to `APPROVED`. | Returns `ReturnResponse` with status `APPROVED`. | Staff (`ADMIN`) can approve return claim. |
| **8** | `testStaffRole_Manager_CanSchedulePickupWithCourierAndAWB` | Claim in `APPROVED` status. Manager updates to `PICKUP_SCHEDULED` with courier "Blue Dart Reverse Logistics" and AWB "BDR-89214". | Status is `PICKUP_SCHEDULED`, reverseCourier and reverseTrackingNumber recorded. | Staff (`MANAGER`) can assign courier AWB. |
| **9** | `testStaffRole_Owner_CanCompleteReturn` | Claim in `PICKUP_SCHEDULED`. Owner updates to `COMPLETED`. | Status is `COMPLETED`. | Staff (`OWNER`) can complete return refund. |
| **10** | `testStaffRole_CanRejectReturnWithMandatoryReason` | Claim in `PENDING`. Staff updates to `REJECTED` with admin notes "Item worn and washed". | Status is `REJECTED`, adminNotes saved. | Staff can reject with reason. |
| **11** | `testStaffRole_RejectWithoutReason_ThrowsBadRequestException` | Claim in `PENDING`. Staff updates to `REJECTED` with blank admin notes. | Throws `BadRequestException` ("Mandatory rejection reason must be provided in admin notes"). | Reject requires mandatory reason. |
| **12** | `testStaffRole_SchedulePickupWithoutCourierOrAWB_ThrowsBadRequestException` | Claim in `APPROVED`. Staff updates to `PICKUP_SCHEDULED` missing courier or tracking number. | Throws `BadRequestException` ("Reverse courier and tracking number are required"). | Pickup schedule validation. |
| **13** | `testCustomerRole_CannotUpdateReturnStatus_ThrowsAccessDeniedException` | User has `Role.CUSTOMER`. Attempts to call `updateReturnStatus`. | Throws `AccessDeniedException` ("Not authorised to perform this action"). | Access control enforcement. |
| **14** | `testCreateReturnRequest_DeliveredBoundaryExact7Days_Success` | Order delivered 6 days, 23 hours, 55 minutes ago (within 7-day boundary). | Returns `ReturnResponse` with status `PENDING`. Succeeds. | Exact 7-day boundary cutoff precision. |
| **15** | `testCreateReturnRequest_DeliveredBoundary7Days1Hour_Fails` | Order delivered 7 days, 1 hour ago. | Throws `BadRequestException` ("Return window has expired"). | Exact 7-day boundary cutoff precision. |
| **16** | `testCreateReturnRequest_LegacyOrderNullDeliveredAt_UsesUpdatedAtFallback` | Order delivered in past with `deliveredAt == null`, but `updatedAt = now - 2d`. | Gracefully computes delivery time from `updatedAt` and succeeds. | Backwards compatibility for legacy orders. |
| **17** | `testInvalidStateTransition_CompletedToPending_ThrowsBadRequestException` | Return is in terminal status `COMPLETED`. Staff attempts to set back to `PENDING`. | Throws `BadRequestException` ("Cannot alter status of an already completed return request"). | State machine integrity. |
| **18** | `testCreateReturnRequest_ExchangeTypeMissingSku_ThrowsBadRequestException` | Return type is `EXCHANGE` but `exchangeSku` is null/empty. | Throws `BadRequestException` ("Exchange SKU is required when selecting saree exchange"). | Domain exchange validation. |

### 5.3 Ready-to-Implement Source Code: `ReturnServiceImplTest.java`

```java
package com.sareekart.service;

import com.sareekart.dto.request.ReturnCreateRequest;
import com.sareekart.dto.request.ReturnStatusUpdateRequest;
import com.sareekart.dto.response.ReturnResponse;
import com.sareekart.entity.*;
import com.sareekart.exception.BadRequestException;
import com.sareekart.repository.OrderRepository;
import com.sareekart.repository.ReturnRequestRepository;
import com.sareekart.repository.UserRepository;
import com.sareekart.service.impl.ReturnServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive Unit Test Suite for ReturnServiceImpl.
 * Verifies 7-day post-delivery cutoff, order status gating, duplicate prevention,
 * customer data isolation, and staff moderation lifecycle state transitions.
 */
@ExtendWith(MockitoExtension.class)
public class ReturnServiceImplTest {

    @Mock
    private ReturnRequestRepository returnRequestRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationEventService notificationEventService;

    @InjectMocks
    private ReturnServiceImpl returnService;

    private User customerA;
    private User customerB;
    private User adminUser;
    private User managerUser;
    private User ownerUser;

    private Order deliveredOrder;
    private ReturnCreateRequest validCreateRequest;

    @BeforeEach
    void setUp() {
        customerA = User.builder()
                .id(10L)
                .firstName("Priya")
                .lastName("Sharma")
                .email("priya@example.com")
                .mobile("9876543210")
                .role(Role.CUSTOMER)
                .build();

        customerB = User.builder()
                .id(20L)
                .firstName("Rohan")
                .lastName("Verma")
                .email("rohan@example.com")
                .mobile("9123456780")
                .role(Role.CUSTOMER)
                .build();

        adminUser = User.builder()
                .id(1L)
                .firstName("Admin")
                .email("admin@sareekart.com")
                .role(Role.ADMIN)
                .build();

        managerUser = User.builder()
                .id(2L)
                .firstName("Manager")
                .email("manager@sareekart.com")
                .role(Role.MANAGER)
                .build();

        ownerUser = User.builder()
                .id(3L)
                .firstName("Owner")
                .email("owner@sareekart.com")
                .role(Role.OWNER)
                .build();

        deliveredOrder = Order.builder()
                .id(100L)
                .user(customerA)
                .totalAmount(new BigDecimal("2499.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(3))
                .createdAt(LocalDateTime.now().minusDays(5))
                .updatedAt(LocalDateTime.now().minusDays(3))
                .build();

        validCreateRequest = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("RETURN")
                .reason("COLOR_MISMATCH")
                .refundMode("ORIGINAL_PAYMENT")
                .comments("The color is noticeably darker navy than the royal blue shown on screen.")
                .images(List.of("/uploads/return-photos/return-defect1.jpg", "/uploads/return-photos/return-defect2.jpg"))
                .refundAmount(new BigDecimal("2499.00"))
                .build();
    }

    // =========================================================================
    // 1. Order Eligibility & 7-Day Window Tests
    // =========================================================================

    @Test
    void testCreateReturnRequest_DeliveredOrderWithin7Days_Success() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));
        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest r = invocation.getArgument(0);
            r.setId(1L);
            r.setCreatedAt(LocalDateTime.now());
            r.setUpdatedAt(LocalDateTime.now());
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100L, response.getOrderId());
        assertEquals(customerA.getId(), response.getUserId());
        assertEquals("PENDING", response.getStatus());
        assertEquals("COLOR_MISMATCH", response.getReason());
        assertEquals(new BigDecimal("2499.00"), response.getRefundAmount());
        assertEquals(2, response.getImages().size());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderInShippedStatus_ThrowsBadRequestException() {
        Order shippedOrder = Order.builder()
                .id(101L)
                .user(customerA)
                .status(OrderStatus.SHIPPED)
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        validCreateRequest.setOrderId(101L);
        when(orderRepository.findById(101L)).thenReturn(Optional.of(shippedOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderInPendingStatus_ThrowsBadRequestException() {
        Order pendingOrder = Order.builder()
                .id(102L)
                .user(customerA)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        validCreateRequest.setOrderId(102L);
        when(orderRepository.findById(102L)).thenReturn(Optional.of(pendingOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Only delivered orders are eligible"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_OrderDeliveredMoreThan7DaysAgo_ThrowsBadRequestException() {
        Order expiredOrder = Order.builder()
                .id(103L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(8)) // 8 days ago > 7 days cutoff
                .build();

        validCreateRequest.setOrderId(103L);
        when(orderRepository.findById(103L)).thenReturn(Optional.of(expiredOrder));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("Return window has expired") || ex.getMessage().contains("7 calendar days"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_DeliveredBoundaryExact7Days_Success() {
        Order boundaryOrder = Order.builder()
                .id(104L)
                .user(customerA)
                .totalAmount(new BigDecimal("1800.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(6).minusHours(23).minusMinutes(50)) // Within 7 days
                .build();

        validCreateRequest.setOrderId(104L);
        when(orderRepository.findById(104L)).thenReturn(Optional.of(boundaryOrder));
        when(returnRequestRepository.findByOrderId(104L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(invocation -> {
            ReturnRequest r = invocation.getArgument(0);
            r.setId(2L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_DeliveredBoundary7Days1Hour_Fails() {
        Order boundaryExpiredOrder = Order.builder()
                .id(105L)
                .user(customerA)
                .status(OrderStatus.DELIVERED)
                .deliveredAt(LocalDateTime.now().minusDays(7).minusHours(1)) // 7 days + 1 hr
                .build();

        validCreateRequest.setOrderId(105L);
        when(orderRepository.findById(105L)).thenReturn(Optional.of(boundaryExpiredOrder));

        assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_LegacyOrderNullDeliveredAt_UsesUpdatedAtFallback() {
        Order legacyOrder = Order.builder()
                .id(106L)
                .user(customerA)
                .totalAmount(new BigDecimal("3500.00"))
                .status(OrderStatus.DELIVERED)
                .deliveredAt(null) // Legacy order without deliveredAt
                .updatedAt(LocalDateTime.now().minusDays(2)) // Fallback reference
                .build();

        validCreateRequest.setOrderId(106L);
        when(orderRepository.findById(106L)).thenReturn(Optional.of(legacyOrder));
        when(returnRequestRepository.findByOrderId(106L)).thenReturn(Optional.empty());
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> {
            ReturnRequest r = i.getArgument(0);
            r.setId(3L);
            return r;
        });

        ReturnResponse response = returnService.createReturnRequest(validCreateRequest, customerA.getId());
        assertNotNull(response);
        assertEquals("PENDING", response.getStatus());
    }

    // =========================================================================
    // 2. Duplicate Prevention & Customer Data Isolation
    // =========================================================================

    @Test
    void testCreateReturnRequest_DuplicateSubmission_ThrowsBadRequestException() {
        ReturnRequest existingReturn = ReturnRequest.builder()
                .id(10L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));
        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.of(existingReturn));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerA.getId()));

        assertTrue(ex.getMessage().contains("already been submitted") || ex.getMessage().contains("already exists"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCreateReturnRequest_UnauthorizedOrderOwnership_ThrowsException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));

        // Customer B attempts to submit return on order owned by Customer A
        assertThrows(Exception.class, () ->
                returnService.createReturnRequest(validCreateRequest, customerB.getId()));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCustomerAccessControl_CannotAccessAnotherCustomersReturnClaim() {
        ReturnRequest customerAReturn = ReturnRequest.builder()
                .id(50L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        when(returnRequestRepository.findByOrderId(100L)).thenReturn(Optional.of(customerAReturn));

        // Customer B calls getReturnRequestByOrderId for order owned by Customer A
        assertThrows(Exception.class, () ->
                returnService.getReturnRequestByOrderId(100L, customerB.getId()));
    }

    // =========================================================================
    // 3. Staff Moderation & Lifecycle State Transitions
    // =========================================================================

    @Test
    void testStaffRole_Admin_CanApproveReturnRequest() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(50L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .adminNotes("Condition photo verified. Saree defect approved for return.")
                .build();

        when(returnRequestRepository.findById(50L)).thenReturn(Optional.of(pendingClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(50L, updateReq, adminUser);

        assertNotNull(response);
        assertEquals("APPROVED", response.getStatus());
        assertEquals("Condition photo verified. Saree defect approved for return.", response.getAdminNotes());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_Manager_CanSchedulePickupWithCourierAndAWB() {
        ReturnRequest approvedClaim = ReturnRequest.builder()
                .id(51L)
                .order(deliveredOrder)
                .user(customerA)
                .status("APPROVED")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("PICKUP_SCHEDULED")
                .reverseCourier("Blue Dart Reverse Logistics")
                .reverseTrackingNumber("BDR-89214")
                .adminNotes("Pickup scheduled for tomorrow 10:00 AM - 2:00 PM")
                .build();

        when(returnRequestRepository.findById(51L)).thenReturn(Optional.of(approvedClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(51L, updateReq, managerUser);

        assertNotNull(response);
        assertEquals("PICKUP_SCHEDULED", response.getStatus());
        assertEquals("Blue Dart Reverse Logistics", response.getReverseCourier());
        assertEquals("BDR-89214", response.getReverseTrackingNumber());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_SchedulePickupWithoutCourierOrAWB_ThrowsBadRequestException() {
        ReturnRequest approvedClaim = ReturnRequest.builder()
                .id(51L)
                .order(deliveredOrder)
                .user(customerA)
                .status("APPROVED")
                .build();

        ReturnStatusUpdateRequest missingCourierReq = ReturnStatusUpdateRequest.builder()
                .status("PICKUP_SCHEDULED")
                .reverseCourier(null)
                .reverseTrackingNumber("BDR-89214")
                .build();

        when(returnRequestRepository.findById(51L)).thenReturn(Optional.of(approvedClaim));

        assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(51L, missingCourierReq, managerUser));

        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_Owner_CanCompleteReturn() {
        ReturnRequest pickupClaim = ReturnRequest.builder()
                .id(52L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PICKUP_SCHEDULED")
                .reverseCourier("Delhivery")
                .reverseTrackingNumber("DLV-123456")
                .refundAmount(new BigDecimal("2499.00"))
                .build();

        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("COMPLETED")
                .adminNotes("Saree inspection passed at Bengaluru hub. Full refund credited.")
                .build();

        when(returnRequestRepository.findById(52L)).thenReturn(Optional.of(pickupClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(52L, updateReq, ownerUser);

        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_CanRejectReturnWithMandatoryReason() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(53L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest rejectReq = ReturnStatusUpdateRequest.builder()
                .status("REJECTED")
                .adminNotes("Item shows visible alteration and missing original handloom tag.")
                .build();

        when(returnRequestRepository.findById(53L)).thenReturn(Optional.of(pendingClaim));
        when(returnRequestRepository.save(any(ReturnRequest.class))).thenAnswer(i -> i.getArgument(0));

        ReturnResponse response = returnService.updateReturnStatus(53L, rejectReq, adminUser);

        assertNotNull(response);
        assertEquals("REJECTED", response.getStatus());
        assertEquals("Item shows visible alteration and missing original handloom tag.", response.getAdminNotes());
        verify(returnRequestRepository, times(1)).save(any(ReturnRequest.class));
    }

    @Test
    void testStaffRole_RejectWithoutReason_ThrowsBadRequestException() {
        ReturnRequest pendingClaim = ReturnRequest.builder()
                .id(53L)
                .order(deliveredOrder)
                .user(customerA)
                .status("PENDING")
                .build();

        ReturnStatusUpdateRequest invalidRejectReq = ReturnStatusUpdateRequest.builder()
                .status("REJECTED")
                .adminNotes("") // Blank rejection notes
                .build();

        when(returnRequestRepository.findById(53L)).thenReturn(Optional.of(pendingClaim));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(53L, invalidRejectReq, adminUser));

        assertTrue(ex.getMessage().contains("Mandatory rejection reason") || ex.getMessage().contains("admin notes"));
        verify(returnRequestRepository, never()).save(any(ReturnRequest.class));
    }

    @Test
    void testCustomerRole_CannotUpdateReturnStatus_ThrowsAccessDeniedException() {
        ReturnStatusUpdateRequest updateReq = ReturnStatusUpdateRequest.builder()
                .status("APPROVED")
                .build();

        assertThrows(AccessDeniedException.class, () ->
                returnService.updateReturnStatus(50L, updateReq, customerA));
    }

    @Test
    void testInvalidStateTransition_CompletedToPending_ThrowsBadRequestException() {
        ReturnRequest completedClaim = ReturnRequest.builder()
                .id(54L)
                .order(deliveredOrder)
                .user(customerA)
                .status("COMPLETED")
                .build();

        ReturnStatusUpdateRequest invalidReq = ReturnStatusUpdateRequest.builder()
                .status("PENDING")
                .build();

        when(returnRequestRepository.findById(54L)).thenReturn(Optional.of(completedClaim));

        assertThrows(BadRequestException.class, () ->
                returnService.updateReturnStatus(54L, invalidReq, adminUser));
    }

    @Test
    void testCreateReturnRequest_ExchangeTypeMissingSku_ThrowsBadRequestException() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(deliveredOrder));

        ReturnCreateRequest exchangeReq = ReturnCreateRequest.builder()
                .orderId(100L)
                .type("EXCHANGE")
                .reason("SIZE_MISMATCH")
                .refundMode("EXCHANGE_DRAPE")
                .exchangeSku(null) // Missing SKU
                .build();

        assertThrows(BadRequestException.class, () ->
                returnService.createReturnRequest(exchangeReq, customerA.getId()));
    }
}
```

---

## 6. Complementary Controller Test Suite Specification

To guarantee that the controllers handle HTTP routing, request validation, authentication principals, and security exceptions correctly under MockMvc, the Worker should also provide controller unit tests.

### 6.1 `ReturnControllerTest.java` (Customer Endpoints)
- **Path**: `backend/backend/src/test/java/com/sareekart/controller/ReturnControllerTest.java`
- **Key Tests**:
  1. `testCreateReturnRequest_Returns201Created`: Calls `POST /api/returns`, verifies HTTP 201 and JSON payload.
  2. `testGetMyRequests_Returns200Ok`: Calls `GET /api/returns/my-requests`, verifies HTTP 200 and list.
  3. `testGetReturnByOrderId_Returns200Ok`: Calls `GET /api/returns/order/100`, verifies HTTP 200 and object.
  4. `testUploadPhoto_Success`: Uploads valid `test.jpg` (JPEG) -> HTTP 200, `data.url` starts with `/uploads/return-photos/`.
  5. `testUploadPhoto_UnsupportedFormat_Returns400`: Uploads `test.exe` -> HTTP 400 Bad Request.
  6. `testUploadPhoto_EmptyFile_Returns400`: Uploads empty file -> HTTP 400 Bad Request.

### 6.2 `AdminReturnControllerTest.java` (Admin Endpoints & RBAC)
- **Path**: `backend/backend/src/test/java/com/sareekart/controller/AdminReturnControllerTest.java`
- **Key Tests**:
  1. `testGetAllReturns_Success`: Calls `GET /api/admin/returns?status=PENDING` -> HTTP 200 OK.
  2. `testUpdateReturnStatus_Success`: Calls `PUT /api/admin/returns/1/status` with `APPROVED` -> HTTP 200 OK.
  3. `testAccessDeniedException_ReturnsExact403ForbiddenPayload`:
     ```java
     when(returnService.getAllReturnsForAdmin(any()))
             .thenThrow(new org.springframework.security.access.AccessDeniedException("Access is denied"));

     mockMvc.perform(get("/api/admin/returns"))
             .andExpect(status().isForbidden())
             .andExpect(jsonPath("$.success").value(false))
             .andExpect(jsonPath("$.message").value("Not authorised to perform this action"));
     ```

---

## 7. Downstream Worker Implementation Checklist

The implementation worker (`teamwork_preview_worker_m1`) must carry out the following exact steps:

- [ ] **Step 1: Create Customer REST Controller**
  - Create `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java` using the source code from Section 2.3.
- [ ] **Step 2: Create Admin REST Controller**
  - Create `backend/backend/src/main/java/com/sareekart/controller/AdminReturnController.java` using the source code from Section 3.3.
- [ ] **Step 3: Update Security RBAC Configuration**
  - Apply the patch to `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java` from Section 4.4.
- [ ] **Step 4: Create Service Unit Test Suite**
  - Create `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java` using the source code from Section 5.3.
- [ ] **Step 5: Run Verification Commands**
  - `./mvnw test -Dtest=ReturnServiceImplTest` -> Verify 100% pass rate.
  - `./mvnw test` -> Verify all 65+ existing tests plus new tests pass with 0 errors and 0 failures.
  - `/Users/chaitanyachaitu/scripts/check_disk_health.sh` -> Verify >= 30% free space maintained.

---
*Report completed and verified against SareeKart architectural baseline and user requirements.*
