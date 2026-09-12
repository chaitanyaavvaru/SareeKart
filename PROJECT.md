# Project: SareeKart v3.0 Module 1 — Self-Service Customer Returns & Exchanges

## Architecture
- **Backend**: Spring Boot 3.5.15 / Java 17 located in `backend/backend/`, Maven wrapper `./mvnw`, running on port `8081`. Uses H2 in-memory for unit/integration tests and local MySQL on port `3306` (`sareekart_db`) for runtime.
- **Frontend**: React 19 / Vite 8 located in `frontend/`, running on port `5173`. Proxies `/api` requests to `http://127.0.0.1:8081`.
- **Offline Isolation**: All tests, builds, and verification scripts execute strictly on `localhost` without external network calls or cloud dependencies.
- **Bundle Budget**: Production bundle chunk size strictly below 500 kB. Pure SVG (Lucide React) and Tailwind CSS used to maintain bundle constraint.
- **Photo Storage & Serving**: Defect condition photos stored locally in `uploads/return-photos/` and served statically at `/uploads/**` via Spring Boot `StaticResourceConfig.java`.

## Code Layout
- Backend Domain Entities & Enums: `backend/backend/src/main/java/com/sareekart/entity/ReturnRequest.java`, `enums/ReturnStatus.java`, `enums/ReturnType.java`, `enums/ReturnReason.java`, `enums/RefundMode.java`
- Backend Repository: `backend/backend/src/main/java/com/sareekart/repository/ReturnRequestRepository.java`
- Backend DTOs: `backend/backend/src/main/java/com/sareekart/dto/request/ReturnCreateRequest.java`, `ReturnStatusUpdateRequest.java`, `dto/response/ReturnResponse.java`
- Backend Services: `backend/backend/src/main/java/com/sareekart/service/ReturnService.java`, `backend/backend/src/main/java/com/sareekart/service/impl/ReturnServiceImpl.java`
- Backend Controllers: `backend/backend/src/main/java/com/sareekart/controller/ReturnController.java`, `AdminReturnController.java`
- Backend Security & Exceptions: `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`
- Backend Database Migration: `backend/backend/src/main/resources/db/migration/V17__create_return_requests_table.sql`
- Backend Tests: `backend/backend/src/test/java/com/sareekart/service/ReturnServiceImplTest.java`, `ReturnStateMachineAdversarialTest.java`, `controller/ReturnControllerTest.java`, `AdminReturnControllerTest.java`
- Frontend Service: `frontend/src/services/returnService.js`
- Frontend Components: `frontend/src/components/orders/ReturnRequestModal.jsx`, `frontend/src/components/orders/ReturnStatusDrawer.jsx`
- Frontend Pages: `frontend/src/pages/MyOrders.jsx`, `frontend/src/pages/Admin/ManageReturns.jsx`
- Frontend Navigation & Routing: `frontend/src/routes/AppRouter.jsx`, `frontend/src/pages/Admin/AdminDashboard.jsx`

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Backend Domain Model & Persistence (R2) | `ReturnRequest` JPA entity, `return_requests` table, `ReturnRequestRepository` queries, unique order constraint, and `V17__create_return_requests_table.sql` schema migration | M1 (DONE) | ORIGINAL_REQUEST §R2 |
| 2 | Backend Service Layer & State Machine (R2) | `ReturnService` & `ReturnServiceImpl` enforcing order ownership, `DELIVERED` status check, <= 7-day post-delivery cutoff, duplicate prevention, and state transitions (`PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`) | M1 (DONE) | ORIGINAL_REQUEST §R2 |
| 3 | REST Endpoints & RBAC Access Control (R3) | Customer endpoints (`POST /api/returns`, `GET /api/returns/my-requests`, `GET /api/returns/order/{orderId}`, `POST /api/returns/upload-photo`) and Admin endpoints (`GET /api/admin/returns`, `PUT /api/admin/returns/{id}/status`), with 403 Forbidden on unauthorized access | M1 (DONE) | ORIGINAL_REQUEST §R3 |
| 4 | Backend Unit Test Suite (AC) | Unit & integration tests in `ReturnServiceImplTest.java` covering all 6 mandatory acceptance scenarios plus edge cases; passing with 100% pass rate (44 unit tests, 139 full suite) | M1 (DONE) | Acceptance Criteria |
| 5 | Self-Service Returns & Exchanges Modal (R1) | `ReturnRequestModal.jsx` with Return vs Exchange toggle, 6-reason taxonomy, drag-and-drop defect photo uploader (up to 3 photos with client preview), refund preference, and customer comments | M2 | ORIGINAL_REQUEST §R1 |
| 6 | 7-Day Eligibility Gate & Order Card Telemetry (R1, R5) | Active "Return / Exchange" button for delivered orders <= 7 days old, disabled button with explanatory tooltip for non-eligible orders, return status pills, and "View Return Status" tracking drawer | M2 | ORIGINAL_REQUEST §R1, §R5 |
| 7 | Admin Moderation & Reverse Logistics Console (R4) | `ManageReturns.jsx` at `/admin/returns` with KPI cards (Total Claims, Pending Review, Pickups Scheduled, Completed Refunds), filterable claims table, photo inspection drawer, and 1-click action controls (Approve, Assign Courier/AWB, Complete, Reject) | M2 | ORIGINAL_REQUEST §R4 |
| 8 | Admin Navigation & App Routing Integration (R4) | Register `/admin/returns` in `AppRouter.jsx` and add "Returns & Exchanges" under Commerce in `AdminDashboard.jsx` sidebar | M2 | ORIGINAL_REQUEST §R4 |
| 9 | Full Regression & Production Bundle Budget (AC) | Full backend regression (`./mvnw test` all tests passing 100%), frontend production build (`npm run build` 0 errors, all chunks < 500 kB), and disk health verification (>= 30% free space) | M3 | Acceptance Criteria |
| 10 | Adversarial Hardening & Forensic Integrity Audit (AC) | White-box stress-testing by Challengers and forensic verification by Auditor ensuring genuine implementation with zero cheating | M3 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Backend Domain Model, Services, REST API & Unit Tests | `ReturnRequest` entity, enums, `ReturnRequestRepository`, `ReturnService` & `ReturnServiceImpl`, `ReturnController`, `AdminReturnController`, security RBAC, migration script, and `ReturnServiceImplTest.java` | None | DONE |
| M2 | Frontend Customer Returns Modal, Telemetry & Admin Console | `returnService.js`, `ReturnRequestModal.jsx`, `ReturnStatusDrawer.jsx`, `MyOrders.jsx` eligibility & telemetry updates, `ManageReturns.jsx` admin console, `AdminDashboard.jsx` & `AppRouter.jsx` | M1 | DONE |
| M3 | Full Regression, Production Build, Adversarial Hardening & Audit | Full `./mvnw test` passing 100%, frontend `npm run build` < 500 kB, disk check >= 30%, Challenger tests, and Forensic Auditor verification | M1, M2 | DONE |

## Interface Contracts

### Customer Returns REST API
- Base Path: `/api/returns`
- Authorization: Bearer JWT (Role: `CUSTOMER`, `OWNER`, `MANAGER`, `ADMIN`)

1. `POST /api/returns`
   - Request Body:
     ```json
     {
       "orderId": 102,
       "type": "RETURN",
       "reason": "COLOR_MISMATCH",
       "refundMode": "ORIGINAL_PAYMENT",
       "comments": "Color is noticeably darker than in the photo",
       "images": ["/uploads/return-photos/defect-1.jpg"],
       "exchangeSku": null
     }
     ```
   - Response (201 Created):
     ```json
     {
       "success": true,
       "message": "Return request submitted successfully",
       "data": {
         "id": 1,
         "orderId": 102,
         "type": "RETURN",
         "reason": "COLOR_MISMATCH",
         "status": "PENDING",
         "refundAmount": 2499.00,
         "refundMode": "ORIGINAL_PAYMENT",
         "exchangeSku": null,
         "reverseCourier": null,
         "reverseTrackingNumber": null,
         "images": ["/uploads/return-photos/defect-1.jpg"],
         "comments": "Color is noticeably darker than in the photo",
         "adminNotes": null,
         "createdAt": "2026-09-11T10:15:00Z",
         "updatedAt": "2026-09-11T10:15:00Z"
       }
     }
     ```

2. `GET /api/returns/my-requests`
   - Response (200 OK): `ApiResponse<List<ReturnResponse>>`

3. `GET /api/returns/order/{orderId}`
   - Response (200 OK): `ApiResponse<ReturnResponse>` (or null data if none exists)

4. `POST /api/returns/upload-photo`
   - Content-Type: `multipart/form-data` (field: `photo` or `file`)
   - Response (200 OK):
     ```json
     {
       "success": true,
       "message": "Photo uploaded successfully",
       "data": {
         "url": "/uploads/return-photos/defect-uuid.jpg"
       }
     }
     ```

### Admin Returns Moderation REST API
- Base Path: `/api/admin/returns`
- Authorization: Bearer JWT (`OWNER`, `MANAGER`, `ADMIN`)
- Unauthorized Response: HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`

1. `GET /api/admin/returns?status={ALL|PENDING|APPROVED|PICKUP_SCHEDULED|REJECTED|COMPLETED}`
   - Response (200 OK): `ApiResponse<List<ReturnResponse>>`

2. `PUT /api/admin/returns/{id}/status`
   - Request Body:
     ```json
     {
       "status": "PICKUP_SCHEDULED",
       "reverseCourier": "Blue Dart Reverse Logistics",
       "reverseTrackingNumber": "BDR-9812401",
       "adminNotes": "Pickup scheduled for tomorrow 10am-2pm"
     }
     ```
   - Validation:
     - If `status == REJECTED`, `adminNotes` is mandatory.
     - If `status == PICKUP_SCHEDULED`, `reverseCourier` and `reverseTrackingNumber` are mandatory.
   - Response (200 OK): `ApiResponse<ReturnResponse>`
