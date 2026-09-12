# Project: SareeKart Operations & Customer Engagement Suite

## Architecture
- **Backend**: Spring Boot 3.5.15 / Java 17 located in `backend/backend/`, Maven wrapper `./mvnw`, running on port `8081`. Uses H2 in-memory for unit/integration tests and local MySQL on port `3306` (`sareekart_db`) for runtime.
- **Frontend**: React 19 / Vite 8 located in `frontend/`, running on port `5173`. Proxies `/api` requests to `http://127.0.0.1:8081`.
- **Offline Isolation**: All tests, builds, and verification scripts execute strictly on `localhost` without external network calls or cloud dependencies.
- **Bundle Budget**: Production bundle chunk size strictly below 500 kB via Vite `manualChunks` in `vite.config.js`. Pure SVG/CSS visualizations used for reviews and breakdown bars.

## Code Layout
- Backend Entities & Repos: `backend/backend/src/main/java/com/sareekart/entity/`, `repository/`
- Backend DTOs & Services: `backend/backend/src/main/java/com/sareekart/dto/`, `service/`, `service/impl/`
- Backend Controllers & Security: `backend/backend/src/main/java/com/sareekart/controller/`, `config/SecurityConfig.java`, `exception/GlobalExceptionHandler.java`
- Backend Tests: `backend/backend/src/test/java/com/sareekart/`
- Frontend Routing & Nav: `frontend/src/routes/AppRouter.jsx`, `frontend/src/pages/Admin/AdminDashboard.jsx`, `frontend/src/components/Navbar.jsx`
- Frontend Components: `frontend/src/components/common/NotificationCenter.jsx`, `frontend/src/components/orders/OrderTrackingModal.jsx`
- Frontend Consoles: `frontend/src/pages/Admin/ManageReviews.jsx`, `frontend/src/pages/Admin/ManageInventory.jsx`, `frontend/src/pages/Admin/ApprovalCenter.jsx`, `frontend/src/pages/Artisans/ArtisansPage.jsx`
- Frontend Tests: `frontend/tests/operations-engagement.spec.js`, `frontend/playwright.config.js`

## Feature Inventory
| # | Feature | Description | Milestone | Source |
|---|---------|-------------|-----------|--------|
| 1 | Centralized Event-Driven Notification Engine (R1) | Centralized notification triggers on order placed, dispatched (with tracking AWB), delivered, low-stock alerts, pending Maker-Checker approvals | M1 | ORIGINAL_REQUEST §R1 |
| 2 | Navbar Notification Center Widget (R1) | Bell icon in storefront Navbar and Admin header with unread badge counter, popover list, mark-as-read, and link routing | M1 | ORIGINAL_REQUEST §R1 |
| 3 | Simulated Notification Event Audit Log (R1) | Backend audit log recording in-app, simulated SMS, and email payloads at `/api/admin/notifications/dispatch-log` | M1 | ORIGINAL_REQUEST §R1 |
| 4 | Multi-Warehouse Regional Hubs & Balances (R2) | Hubs `WH-01 Bengaluru Central`, `WH-02 Mumbai West`, `WH-03 Delhi North` with per-hub stock tracking and allocation | M2 | ORIGINAL_REQUEST §R2 |
| 5 | Maker-Checker Stock Transfers (R2) | Manager submits transfer request; Owner reviews, approves, or rejects in Approval Center; Owner direct instant transfer execution | M2 | ORIGINAL_REQUEST §R2 |
| 6 | Courier Partner & AWB Logistics (R2) | Assign carrier (Blue Dart, Delhivery, DTDC, India Post), generate automated tracking AWB, and set origin hub | M2 | ORIGINAL_REQUEST §R2 |
| 7 | Shipment Milestone Progression (R2) | 5-stage milestones (`Ordered`, `Packed`, `Shipped`, `Out for Delivery`, `Delivered`) displayed on My Orders and Admin Orders | M2 | ORIGINAL_REQUEST §R2 |
| 8 | Customer Review Submission & Star Rating (R3) | 1-5 star rating and comment submission form on product detail pages | M3 | ORIGINAL_REQUEST §R3 |
| 9 | Verified Buyer Badge Detection (R3) | Checks completed/placed orders for product; displays distinct verified buyer badge with checkmark | M3 | ORIGINAL_REQUEST §R3 |
| 10 | Review Analytics & 5-Star Breakdown (R3) | Average rating, total review count, and pure SVG/CSS 5-star distribution breakdown bars | M3 | ORIGINAL_REQUEST §R3 |
| 11 | Admin Review Moderation Console (R3) | Filterable moderation console at `/admin/reviews` with tabs (`Pending`, `Approved`, `Rejected`, `Featured`) and actions | M3 | ORIGINAL_REQUEST §R3 |
| 12 | Artisan Heritage Storytelling Showcase (R4) | Showcase at `/artisans` featuring 5 loom techniques: Kanchipuram, Banarasi, Patola, Paithani, Chanderi | M4 | ORIGINAL_REQUEST §R4 |
| 13 | Artisan-to-Catalog Deep Linking (R4) | Direct links from artisan profiles to handcrafted sarees produced by each artisan guild | M4 | ORIGINAL_REQUEST §R4 |
| 14 | Access Control & Authorization (RBAC) (R5) | Restricts staff endpoints to OWNER, MANAGER, ADMIN with standard 403 Forbidden payload `{"success":false,"message":"Not authorised to perform this action"}` | M1-M4 | ORIGINAL_REQUEST §R5 |
| 15 | Automated Backend Tests | Unit & integration tests for notifications, warehouse transfers, and reviews; `./mvnw test` passing 100% | M1-M4 | Acceptance Criteria |
| 16 | Automated Playwright E2E Tests | `frontend/tests/operations-engagement.spec.js` and full regression `npx playwright test --project=chromium` passing 100% | M5 | Acceptance Criteria |
| 17 | Production Frontend Build & Chunk Budget | `npm run build` passing with all bundle chunks strictly under 500 kB | M5 | Acceptance Criteria |

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| M1 | Event-Driven Notifications & Dispatch Telemetry (R1, R5) | `Notification`, `NotificationDispatchLog` entities, repos, service, controller; Navbar & Admin notification center with unread badge counter and popover; backend unit tests | None | PLANNED |
| M2 | Multi-Warehouse Transfers & Carrier Logistics (R2, R5) | Hubs WH-01, WH-02, WH-03, `WarehouseStock`, `StockTransfer`, Maker-Checker protocol in `ApprovalService` and `ApprovalCenter.jsx`, courier/AWB assignment, 5-stage milestone progression in tracking modal; backend unit tests | M1 | PLANNED |
| M3 | Verified Customer Reviews & Moderation Console (R3, R5) | `Review` entity extension, verified buyer verification via order history, 5-star breakdown bars, `/admin/reviews` moderation console with status tabs, RBAC 403 enforcement; backend unit tests | M1 | PLANNED |
| M4 | Artisan Heritage Storytelling Showcase (R4) | `Artisan` entity & API with 5 heritage profiles (Kanchipuram, Banarasi, Patola, Paithani, Chanderi) linked to catalog products, `ArtisansPage.jsx` update; backend unit tests | None | PLANNED |
| M5 | E2E Playwright Suite, Production Build & Hardening | `vite.config.js` `manualChunks` optimization (< 500 kB), comprehensive `frontend/tests/operations-engagement.spec.js` (Tiers 1-4), full regression `npx playwright test --project=chromium`, challenger verification, clean forensic audit | M1, M2, M3, M4 | PLANNED |

## Interface Contracts

### 1. Notifications API
- `GET /api/notifications`: List current user/staff notifications.
- `GET /api/notifications/unread-count`: Returns `{ "unreadCount": N }`.
- `PUT /api/notifications/{id}/read`: Mark notification as read.
- `PUT /api/notifications/read-all`: Mark all notifications as read.
- `GET /api/admin/notifications/dispatch-log`: Staff simulated notification audit trail (In-App, SMS, Email).

### 2. Multi-Warehouse & Transfer API
- `GET /api/admin/warehouses/stock`: Stock balances across WH-01, WH-02, WH-03.
- `POST /api/approvals/requests`: Submit Maker-Checker transfer request (`entityType: "STOCK_TRANSFER"`).
- `POST /api/approvals/{id}/approve`: Owner approves transfer.
- `POST /api/approvals/{id}/reject`: Owner rejects transfer.
- `POST /api/admin/transfers/instant`: Owner direct instant transfer execution.
- `PUT /api/admin/orders/{id}/tracking`: Update carrier partner, AWB, location, and milestone status.

### 3. Reviews API
- `GET /api/products/{productId}/reviews`: Returns approved & featured reviews, average rating, and 5-star distribution.
- `POST /api/products/{productId}/reviews`: Authenticated customer submits review; backend verifies purchase history to set `isVerifiedBuyer`.
- `GET /api/admin/reviews?status={PENDING|APPROVED|REJECTED|FEATURED|ALL}`: Staff moderation queue.
- `PUT /api/admin/reviews/{id}/status`: Staff updates review status (`APPROVED`, `REJECTED`, `FEATURED`).

### 4. Artisans API
- `GET /api/artisans`: List 5 artisan heritage profiles.
- `GET /api/artisans/{slug}`: Single artisan profile.
- `GET /api/artisans/{slug}/products`: Handcrafted sarees produced by artisan.
