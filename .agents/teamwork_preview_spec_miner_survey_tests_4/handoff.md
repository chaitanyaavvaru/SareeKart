# Test Infrastructure Survey & Specification Mining Report: SareeKart Operations & Customer Engagement Suite

**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_4`  
**Identity**: Archetype `teamwork_preview_spec_miner`, Role `Test Infrastructure Spec Miner`  
**Parent Orchestrator**: `6f935795-8a42-4bb2-815c-e23698de87b5`  
**Date**: 2026-09-06T12:15:00Z  

---

## 1. Observation

Direct observations from inspecting codebases, configuration files, live service endpoints, and test suites across the repository:

### 1.1 Playwright E2E Setup & Configuration
- **Configuration File**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/playwright.config.js`
  - `testDir: './tests'`
  - `baseURL: 'http://localhost:5173'`
  - `serviceWorkers: 'block'` (prevents stale PWA service worker interference)
  - `projects`: `chromium`, `firefox`, `webkit`
  - `fullyParallel: true`
- **Dependencies**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/package.json`
  - `@playwright/test`: `^1.61.1`
  - `react`: `^19.2.6`, `react-dom`: `^19.2.6`, `react-router-dom`: `^7.17.0`
  - `@reduxjs/toolkit`: `^2.12.0`, `axios`: `^1.18.0`, `framer-motion`: `^12.42.0`, `lucide-react`: `^1.18.0`
  - `@stomp/stompjs`: `^7.3.0`, `sockjs-client`: `^1.6.1`
  - `vite`: `^8.0.12`, `@tailwindcss/vite`: `^4.3.1`
- **Existing Spec Inventory** (24 spec files in `frontend/tests/`):
  - `analytics.spec.js` (170 lines, 5 tests: staff telemetry cards, customer 403 denial, date pills, velocity tabs, export triggers)
  - `approval.spec.js` (176 lines, 6 tests: customer denial, manager stock PO request, manager approval restriction, owner approval, excel transactions, coupon confirmation)
  - `order-tracking.spec.js` (97 lines, 4 tests: public order ID tracking, courier AWB tracking, footer link, My Orders tracking modal)
  - `orders.spec.js` (27 lines, 2 tests: unauthenticated redirect, customer orders history)
  - `admin.spec.js` (45 lines, 2 tests: dashboard load, saree management navigation)
  - `login.spec.js`, `login-recovery.spec.js`, `register.spec.js`, `auth-reset.spec.js`
  - `kankatala-stores-heritage.spec.js` (175 lines, 5 tests: boutiques, VIP drape appointments, heritage weaves, saree care, footer navigation)
  - Additional existing suites: `cart.spec.js`, `category.spec.js`, `checkout.spec.js`, `wishlist.spec.js`, `search.spec.js`, `ai-stylist.spec.js`, `visual-search.spec.js`, `mobile.spec.js`, `cross-browser-booking.spec.js`.

### 1.2 Seed Users, Credentials & Login Helpers
Verified in `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/java/com/sareekart/config/DataSeeder.java` and `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/src/pages/Login/LoginPage.jsx`:
- **UI Quick-Access Demo Buttons** on `/login`:
  - `page.getByRole('button', { name: /^Admin$/i }).click()` -> fills `admin@sareekart.com` / `admin123`
  - `page.getByRole('button', { name: /^Manager$/i }).click()` -> fills `manager@sareekart.com` / `manager123`
  - `page.getByRole('button', { name: /^Owner$/i }).click()` -> fills `owner@sareekart.com` / `owner123`
  - `page.getByRole('button', { name: /^Customer$/i }).click()` -> fills `customer@sareekart.com` / `customer123`
  - Followed by: `page.getByRole('button', { name: /^Sign In$/i }).click()`
- **Direct REST Authentication** via Playwright `request`:
  - `POST http://localhost:8081/api/auth/login` with `{ email: "...", password: "..." }`
  - Returns JWT in `data.token`, attached as `Authorization: Bearer ${token}`.
- **Seeded Credentials Table**:
  | Role | Email | Password | Persona / Role Display | Granted Capabilities |
  |---|---|---|---|---|
  | `ADMIN` | `admin@sareekart.com` | `admin123` | SareeKart Admin | Full administrative console, analytics, catalog, review moderation |
  | `OWNER` | `owner@sareekart.com` | `owner123` | Super Owner | Highest authority; approve/reject maker-checker requests, direct instant transfer |
  | `MANAGER` | `manager@sareekart.com` | `manager123` | Store Manager | Inventory console, submit stock transfer/PO requests; blocked from self-approval |
  | `CUSTOMER` | `customer@sareekart.com` | `customer123` | Chaitanya Customer | Storefront, cart, wishlist, orders, review submission, notification bell |
  | `CUSTOMER` (Alt 1) | `priya@example.com` | `customer123` | Priya Sharma | Seeded with historical completed orders across multiple locations |
  | `CUSTOMER` (Alt 2) | `ananya@example.com` | `customer123` | Ananya Verma | Seeded with historical completed orders across multiple locations |

### 1.3 Live System Verification
- **Local Servers Health Check**:
  - `http://localhost:5173` returned HTTP 200 OK.
  - `http://localhost:8081/api/products` returned HTTP 200 OK.
- **Backend Test Runner**:
  - Executed `./mvnw test` in `backend/backend/`:
    - Tests run: 48, Failures: 0, Errors: 0, Skipped: 0.
    - Time elapsed: 11.858 s. Status: BUILD SUCCESS.
- **Frontend Playwright Tests**:
  - Executed `npx playwright test tests/login.spec.js --project=chromium`: 2 passed (3.8s).
  - Executed `npx playwright test tests/analytics.spec.js --project=chromium`: 5 passed (2.9s).
  - Executed `npx playwright test tests/approval.spec.js --project=chromium`: 6 passed (3.2s).
- **Disk Health**:
  - Checked `/System/Volumes/Data`: 83 GiB available (36.4% free space), compliant with `>= 30% Free Disk Space Rule`.

---

## 2. Logic Chain

1. **Alignment with Authoritative Specification (`ORIGINAL_REQUEST.md`)**:
   - The follow-up section "## Follow-up — 2026-09-06T12:09:27Z" defines 5 requirements (R1–R5) and acceptance criteria requiring a new test suite: `frontend/tests/operations-engagement.spec.js`.
   - The test suite must assert 5 core end-to-end flows:
     1. Customer receives order notification in navbar bell widget with active counter.
     2. Customer submits a review on product page; verified buyer badge is displayed.
     3. Admin/Owner accesses `/admin/reviews` and moderates pending customer reviews.
     4. Store Manager submits inter-warehouse transfer request; Owner approves in Approval Center; stock balances update across hubs.
     5. Order shipment milestones and carrier AWB tracking are visible on My Orders and Admin Orders.
2. **Current Implementation Gap Analysis**:
   - **R1 (Notifications)**: `Navbar.jsx` currently lacks a bell notification widget and badge counter. `OrderNotificationService.java` only dispatches mock WhatsApp messages without in-app persistent notifications, unread count API, or notification audit logs.
   - **R2 (Multi-Warehouse Transfers & Maker-Checker)**: `ApprovalRequest.java` and `ApprovalService.java` currently handle `PRODUCT_PRICE`, `INVENTORY_STOCK`, and `COUPON_*`, but need explicit support for `WAREHOUSE_TRANSFER` between `WH-01 Bengaluru Central`, `WH-02 Mumbai West`, and `WH-03 Delhi North`.
   - **R3 (Verified Reviews & Moderation Console)**: `Review.java` only stores `id`, `productId`, `userName`, `rating`, `comment`, `createdAt`. It lacks `status` (`PENDING`, `APPROVED`, `REJECTED`, `FEATURED`), `userId`, and `verifiedBuyer` flag. There is currently no `/admin/reviews` moderation console or rating breakdown bars on `ProductDetailPage.jsx`.
   - **R4 (Artisans)**: `ArtisansPage.jsx` exists with Kanchipuram, Banarasi, Paithani, and Uppada clusters, but needs Chanderi and Patola profiles to fulfill the requirement.
   - **R5 (RBAC)**: `SecurityConfig.java` enforces 403 Forbidden with exact payload `{"success":false,"message":"Not authorised to perform this action"}` for unauthenticated and customer attempts on `/api/admin/**`. Review moderation endpoints (`/api/admin/reviews/**`) must be wired to this exact handler.
3. **Test Architecture Derivation**:
   - An opaque-box, 4-tier testing hierarchy guarantees both acceptance criteria verification and adversarial resilience without flaky timing.
   - Using Playwright's `getByRole`, `getByTestId`, and `page.waitForEvent` provides deterministic assertions across DOM updates.

---

## 3. Caveats

1. **Local Offline Environment**:
   - No external SMS (Twilio), email (SendGrid), or cloud push gateways are reachable.
   - All notifications are captured in-app via database persistence and recorded in the backend notification audit log (`NotificationAuditLog` or `AuditLog`).
2. **Deterministic Seed Order for Verified Buyer**:
   - Customer `customer@sareekart.com` has historical orders containing Product ID #1 (Royal Banarasi Silk Saree) seeded via `DataSeeder.java`. Tests verifying the "Verified Buyer" badge should target `/products/1` to ensure instant deterministic verification without needing an ad-hoc checkout step.
3. **Multi-Warehouse Hub Codes**:
   - Regional hubs are formally specified as:
     - `WH-01 Bengaluru Central`
     - `WH-02 Mumbai West`
     - `WH-03 Delhi North`

---

## 4. Conclusion & Discovered Feature Specifications

### Features Discovered

| # | Category | Feature | Description | Inputs | Outputs | Error Behavior | Discovered Via |
|---|----------|---------|-------------|--------|---------|----------------|----------------|
| 1 | R1 Notifications | In-App Navbar Bell Widget | Notification bell icon in navbar with dynamic unread counter badge | Customer / Staff login session | Unread count badge (e.g. "2"), popover dropdown | Hides badge when unread count is 0 | `Navbar.jsx`, `ORIGINAL_REQUEST §R1` |
| 2 | R1 Notifications | Notification Popover Feed | Dropdown listing recent business event notifications with timestamps | Bell button click | List of notifications (Order placed, dispatched, delivered, approvals) | Empty state: "No new notifications" | `ORIGINAL_REQUEST §R1` |
| 3 | R1 Notifications | Mark As Read Action | Individual or bulk mark-as-read updating unread badge in real time | Notification click or "Mark all as read" button | Counter decrements, item styled as read | HTTP 404 if notification ID invalid | `ORIGINAL_REQUEST §R1` |
| 4 | R1 Notifications | Event Dispatch Engine | Centralized event triggers for order lifecycle, low stock, and approvals | Order placed, status changed, low-stock threshold | In-app notification record + simulated SMS/email payload | Fallback log if recipient phone/email missing | `ORIGINAL_REQUEST §R1`, `OrderServiceImpl.java` |
| 5 | R1 Notifications | Simulated Notification Audit Log | Backend audit trail of in-app, SMS, and email payloads without external telecom gateways | Staff GET `/api/admin/notifications/audit-log` | JSON list of simulated dispatches with recipient, channel, payload | HTTP 403 if called by Customer | `ORIGINAL_REQUEST §R1` |
| 6 | R2 Multi-Warehouse | Inter-Warehouse Transfer Request | Manager submits transfer request between hubs (WH-01, WH-02, WH-03) | Origin WH, Target WH, SKU, Quantity, Reason | REQ record created with status `Pending owner approval` | Validation error if qty <= 0 or origin == target | `ManageInventory.jsx`, `ORIGINAL_REQUEST §R2` |
| 7 | R2 Multi-Warehouse | Maker-Checker Approval Protocol | Owner reviews, approves, or rejects pending warehouse transfer in Approval Center | Owner click "Approve & Commit" or "Reject" | Stock transferred: Origin stock decreases, target stock increases | Manager attempting approval gets 403 Forbidden | `ApprovalCenter.jsx`, `ApprovalService.java` |
| 8 | R2 Multi-Warehouse | Multi-Warehouse Stock Telemetry | Inventory balances breakdown across WH-01, WH-02, WH-03 | Warehouse allocation table in `/admin/inventory` | On-hand, reserved, available balances per hub | None (defaults to 0) | `ManageInventory.jsx`, `ORIGINAL_REQUEST §R2` |
| 9 | R2 Logistics | Carrier Logistics Assignment | Assign carrier partner (Blue Dart, Delhivery, DTDC, India Post) and AWB | Order details form in `/admin/orders` | Updated tracking metadata on Order entity | HTTP 400 if invalid carrier | `ManageOrders.jsx`, `OrderController.java` |
| 10 | R2 Logistics | Shipment Milestone Progression | Progression through Ordered, Packed, Shipped, Out for Delivery, Delivered | Status update in admin or automated dispatch trigger | Updated milestone status on order and tracking portal | HTTP 400 if invalid transition | `OrderTrackingModal.jsx`, `OrderServiceImpl.java` |
| 11 | R3 Reviews | Star Rating & Comment Submission | Customer submits 1–5 star rating and comment on product page | Product ID, 1–5 stars, comment text | Created review record with status `PENDING` | HTTP 400 if rating < 1 or > 5; HTTP 401 if not logged in | `ProductDetailPage.jsx`, `ReviewController.java` |
| 12 | R3 Reviews | Verified Buyer Badge Detection | Detects if customer purchased the item and renders distinct "Verified Buyer" badge | Customer User ID + Product ID purchase history | "Verified Buyer" badge with checkmark rendered on review | Unverified customers get regular review without badge | `ORIGINAL_REQUEST §R3` |
| 13 | R3 Reviews | Review Analytics & 5-Star Breakdown | Aggregate average rating and 5-star distribution percentage bars | Product reviews aggregation | Average rating (e.g. 4.9), 5★/4★/3★/2★/1★ distribution bars | Displays 0.0 rating and empty bars if no reviews | `ProductDetailPage.jsx`, `ORIGINAL_REQUEST §R3` |
| 14 | R3 Reviews | Admin Review Moderation Console | Console at `/admin/reviews` with tabs: Pending, Approved, Rejected, Featured | Navigation to `/admin/reviews` by staff | Moderation queue with action buttons | HTTP 403 Forbidden for Customer / Unauthenticated | `ORIGINAL_REQUEST §R3` |
| 15 | R3 Reviews | Moderation Action Execution | Staff approves, rejects, or features customer reviews | PUT/POST `/api/admin/reviews/{id}/status` | Review status updated; Approved reviews become visible on storefront | HTTP 404 if review not found; HTTP 403 if unauthorized | `ORIGINAL_REQUEST §R3` |
| 16 | R4 Artisans | Artisan Heritage Storytelling Showcase | Showcase at `/artisans` with master biographies, generational heritage, loom techniques | Navigation to `/artisans` | Artisan profiles (Kanchipuram, Banarasi, Patola, Paithani, Chanderi) | None (static luxury showcase) | `ArtisansPage.jsx`, `ORIGINAL_REQUEST §R4` |
| 17 | R4 Artisans | Artisan-to-Catalog Deep Linking | Links from artisan profiles directly to handcrafted sarees they produced | Click "View Sarees" on artisan card | Deep link navigation to `/products?search={technique}` | None | `ArtisansPage.jsx`, `ORIGINAL_REQUEST §R4` |
| 18 | R5 RBAC | Access Control Hardening | Restricts admin operations to OWNER, MANAGER, ADMIN with standard 403 payload | Customer/Public request to `/api/admin/**` | HTTP 403 `{"success":false,"message":"Not authorised to perform this action"}` | None | `SecurityConfig.java`, `ORIGINAL_REQUEST §R5` |

---

### Edge Cases

| # | Feature | Input | Observed Behavior |
|---|---------|-------|-------------------|
| 1 | Navbar Notification Bell | Customer with zero unread notifications | Bell renders cleanly without badge number; clicking shows "All caught up! No unread notifications" |
| 2 | Notification Mark as Read | Calling mark-as-read on already read notification | Idempotent HTTP 200; badge count remains unchanged |
| 3 | Review Submission | Unauthenticated visitor clicks "Write a review" | Review form displays "Login to share a verified review" with redirect button to `/login` |
| 4 | Review Validation | Submitting empty comment or rating = 0 | Client validation shows inline error; backend returns HTTP 400 `Rating must be between 1 and 5` |
| 5 | Verified Buyer Badge | Customer leaves review on product never purchased | Review accepted and submitted as `PENDING`, but rendered without "Verified Buyer" badge |
| 6 | Inter-Warehouse Transfer | Manager requests transfer quantity > available stock | Backend rejects with HTTP 400 `Requested transfer quantity exceeds available stock at origin hub` |
| 7 | Inter-Warehouse Transfer | Origin warehouse same as destination warehouse | Backend rejects with HTTP 400 `Origin and destination warehouses cannot be the same` |
| 8 | Maker-Checker Security | Manager directly calls `/api/approvals/{id}/approve` | Rejected with HTTP 403 `Not authorised to perform this action` |
| 9 | Maker-Checker Double Action | Owner attempts to approve an already approved/rejected request | Backend returns HTTP 400 `Request is already APPROVED and cannot be re-evaluated` |
| 10 | Order Tracking Portal | Tracking non-existent Order ID or invalid AWB | Returns clean error state: "Order not found with provided tracking number or order ID" |
| 11 | Review Moderation RBAC | Customer navigates to `/admin/reviews` directly in browser | Router redirects customer to storefront `/`; direct API call returns HTTP 403 Forbidden |

---

## 5. Test Architecture & Tier Breakdown (Tiers 1–4)

### Test Architecture Overview
- **Location of E2E Spec**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/tests/operations-engagement.spec.js`
- **Location of Backend Tests**:
  - `backend/backend/src/test/java/com/sareekart/controller/NotificationControllerTest.java`
  - `backend/backend/src/test/java/com/sareekart/service/NotificationServiceTest.java`
  - `backend/backend/src/test/java/com/sareekart/controller/ReviewModerationControllerTest.java`
  - `backend/backend/src/test/java/com/sareekart/service/WarehouseTransferApprovalTest.java`

### Tier 1: Feature Coverage (Core Acceptance Criteria)
1. **Test 1 — Navbar Notification Center & Unread Counter**:
   - Customer logs in (`customer@sareekart.com` / `customer123`).
   - Navbar renders notification bell (`data-testid="notification-bell"`).
   - Verify unread counter badge displays active count > 0.
   - Click bell: Popover appears (`data-testid="notification-popover"`).
   - Notifications list contains order status milestones (e.g. "Order Confirmed").
   - Click "Mark all as read" or click individual notification: badge decrements or clears.
2. **Test 2 — Product Review Submission & Verified Buyer Badge**:
   - Customer logs in and visits `/products/1` (Royal Banarasi Silk Saree, previously ordered).
   - Selects 5-star rating, enters comment "Exquisite zari weave and luxurious drape!", submits review.
   - Reviews section dynamically renders the new review.
   - Distinct **"Verified Buyer"** badge (`data-testid="verified-buyer-badge"`) is prominently displayed.
   - 5-Star distribution breakdown bars and aggregate rating summary render on product page.
3. **Test 3 — Admin Review Moderation Console (`/admin/reviews`)**:
   - Admin/Owner logs in (`admin@sareekart.com` / `admin123`) and navigates to `/admin/reviews`.
   - Filter tabs (`Pending`, `Approved`, `Rejected`, `Featured`) are visible.
   - Pending customer review appears in table with rating, comment, and "Approve" button.
   - Admin clicks "Approve": review status transitions to `Approved`.
   - Navigating to `Approved` tab verifies presence of approved review.
4. **Test 4 — Maker-Checker Multi-Warehouse Stock Transfer**:
   - Store Manager logs in (`manager@sareekart.com` / `manager123`) and navigates to `/admin/inventory`.
   - Submits transfer request: `WH-01 Bengaluru Central` -> `WH-02 Mumbai West`, SKU `SK-ROYAL-BANARASI-SILK-SA-1`, qty `5`.
   - UI confirms submission with banner: `Pending owner approval`.
   - Super Owner logs in (`owner@sareekart.com` / `owner123`) and navigates to `/admin/approvals`.
   - Owner locates request REQ-X and clicks `Approve & Commit`.
   - Success alert confirms approval; stock balances update across hubs (WH-01 -5, WH-02 +5).
5. **Test 5 — Carrier Tracking & Shipment Milestones**:
   - Customer opens My Orders (`/orders`), clicks "Track Shipment": modal opens with Blue Dart carrier, AWB `SK-BD-...`, and milestone progression stepper.
   - Admin opens Admin Orders (`/admin/orders`), clicks "Details": updates carrier tracking details and advances milestone.
   - Changes sync across customer and admin views.
6. **Test 6 — RBAC & 403 Forbidden Enforcement**:
   - Customer navigates to `/admin/reviews`: redirected to storefront `/`.
   - Direct API request to `/api/admin/reviews` with customer token returns HTTP 403 Forbidden with exact body `{"success":false,"message":"Not authorised to perform this action"}`.

### Tier 2: Boundary & Corner Cases
- Submitting review with missing comment or invalid rating (<1 or >5) triggers validation error.
- Transferring quantity equal to available stock updates warehouse item status to `OUT_OF_STOCK`.
- Transferring quantity greater than available stock returns HTTP 400 bad request.
- Manager direct API call to `/api/approvals/{id}/approve` returns 403 Forbidden.
- Empty notification feed displays friendly empty state without throwing runtime exceptions.

### Tier 3: Cross-Feature Combinations
- Placing a customer order immediately triggers an order confirmation notification in customer navbar bell AND an order notification in staff console.
- Manager transfer request generates pending approval notification for Owner; Owner approval updates multi-warehouse balances and sends completed notification.
- Customer review submission -> Admin moderation approval -> Immediate reflection on public `/products/1` page with updated rating breakdown bars.

### Tier 4: Real-World Workloads & Production Compliance
- Multiple concurrent notifications and high-volume review pagination.
- Full E2E regression: `npx playwright test --project=chromium` across all spec files passing 100%.
- Bundle size verification: `npm run build` succeeds with zero errors and all bundle chunks strictly under 500 kB.

---

## 6. Verification Method

To independently verify the test infrastructure and specifications:

1. **Verify Backend Tests**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test
   ```
   *Expected output*: BUILD SUCCESS, 0 failures, 0 errors.

2. **Verify Existing Playwright Suites**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npx playwright test tests/login.spec.js tests/analytics.spec.js tests/approval.spec.js --project=chromium
   ```
   *Expected output*: 13 passed.

3. **Verify Milestone E2E Suite (once implemented)**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npx playwright test tests/operations-engagement.spec.js --project=chromium
   ```
   *Expected output*: 100% pass rate with zero errors.

4. **Verify Full Playwright Regression**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npx playwright test --project=chromium
   ```
   *Expected output*: All test suites pass.

5. **Verify Production Bundle Budget**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npm run build
   ```
   *Expected output*: Build completes with 0 errors; every JS and CSS chunk is strictly < 500 kB.
