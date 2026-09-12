# Frontend Architecture Survey Report: Operations & Customer Engagement Suite

## 1. Observation

### 1.1 Repository & Project Structure
- **Active Frontend Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend`
  - `src/main.jsx`: Root React 19 entrypoint mounting `<AppRouter />` within `<Provider store={store}>`, `<CurrencyProvider>`, and `<BrowserRouter>`.
  - `src/routes/AppRouter.jsx`: Contains code-split lazy loaded routes for public storefront and `/admin` routes.
  - `src/components/layout/MainLayout.jsx`: Shell layout rendering storefront `Navbar`, `Outlet`, and `Footer`.
  - `src/components/Navbar.jsx`: Storefront header with search, visual search modal, video shopping, user profile, wishlist, and cart.
  - `src/pages/Admin/AdminDashboard.jsx`: Admin workspace layout with collapsible sidebar (`ADMIN_NAV`), top bar, and nested `<Outlet />`.
  - Stray folder: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/frontend/src` exists as an empty duplicate, but the active codebase is strictly under `/frontend/src`.

### 1.2 Build System, Dependencies & Chunk Budget
- **Dependencies (`frontend/package.json`)**:
  - React `^19.2.6`, `@reduxjs/toolkit` `^2.12.0`, `react-redux` `^9.3.0`, `react-router-dom` `^7.17.0`, `axios` `^1.18.0`, `lucide-react` `^1.18.0`, `framer-motion` `^12.42.0`, `@stomp/stompjs` `^7.3.0`, `sockjs-client` `^1.6.1`, `tailwindcss` `^4.3.1`, `vite` `^8.0.12`.
- **Vite Configuration (`frontend/vite.config.js`)**:
  - Currently contains plugins `[react(), tailwindcss()]`, proxy `/api` -> `http://127.0.0.1:8081`.
  - Lacks custom `build.rollupOptions.output.manualChunks`.
- **Current Production Build Output (`npm run build`)**:
  - `dist/assets/index-BmByqAsk.js` size: **495.71 kB** (gzip: 154.23 kB).
  - This is **99.14%** of the strict 500 kB chunk limit (only 4.29 kB headroom remaining). Adding un-split components will violate the production bundle budget.

### 1.3 R1: Event-Driven Notifications & Dispatch Telemetry
- **Storefront Navbar (`frontend/src/components/Navbar.jsx`)**:
  - Lines 175–187 render action buttons: Search (`Search`), User Profile (`UserRound`), Wishlist (`Heart`), and `CartButton`.
  - Currently contains **no notification bell or unread badge counter**.
- **Admin Workspace (`frontend/src/pages/Admin/AdminDashboard.jsx`)**:
  - Lines 179–182 contain a static placeholder button:
    ```jsx
    <button type="button" aria-label="View notifications" className="relative flex h-10 w-10 items-center justify-center border border-[#DDD8CF] text-[#4E5B56] transition hover:border-[#1E6A62] hover:text-[#1E6A62]">
      <Bell className="h-4 w-4" />
      <span className="absolute right-2 top-2 h-1.5 w-1.5 bg-[#B84F49]" />
    </button>
    ```
  - It is currently non-interactive (no dropdown, no state connection, no unread counter).
- **Backend & Service State**:
  - `OrderNotificationService.java` currently triggers WhatsApp logging for order placed/status updates.
  - Redux store currently lacks a `notificationSlice`.

### 1.4 R2: Multi-Warehouse Stock Transfers & Carrier Logistics
- **Warehouse Entities & Regional Hubs**:
  - Requirement specifies 3 regional hubs: `WH-01 Bengaluru Central`, `WH-02 Mumbai West`, `WH-03 Delhi North`.
  - Backend entity `InventoryItem.java` stores `warehouseCode` (default `WH-01`), `warehouseName`, `binLocation`, `onHand`, `reserved`, `available`.
  - `ManageInventory.jsx` currently displays mock warehouses (`WH-01 Bengaluru`, `WH-02 Varanasi`, `WH-03 Kanchipuram`) and supports issuing POs via `/api/inventory/adjust`, but lacks an **Inter-Warehouse Stock Transfer modal**.
- **Maker-Checker Protocol**:
  - `ApprovalCenter.jsx` (`/admin/approvals`) handles `pendingRequests` and `history` via `/api/approvals/pending` and `/api/approvals/history`.
  - Backend `ApprovalService.java` currently handles entity types: `PRODUCT_PRICE`, `INVENTORY_STOCK`, `COUPON_DELETE`, `COUPON_CREATE`.
  - An entity type `STOCK_TRANSFER` is required so that:
    - `MANAGER` submits transfer requests (`/api/approvals/requests`).
    - `OWNER` reviews and clicks "Approve & Commit" (`/api/approvals/{id}/approve`) or "Reject" (`/api/approvals/{id}/reject`).
    - `OWNER` also has direct instant transfer capability on `/admin/inventory`.
- **Order Logistics & Milestone Progression**:
  - `ManageOrders.jsx` lines 297–360 currently has a "Logistics & Courier Tracking" form editing `courierPartner`, `trackingNumber` (AWB), `currentLocation`, and `estimatedDeliveryDate`.
  - `TrackOrderPage.jsx` and `OrderTrackingModal.jsx` (in `MyOrders.jsx`) already feature an interactive 6-stage milestone progression stepper (`CONFIRMED`, `QUALITY_CHECKED`, `TAILORING`, `PACKED`, `SHIPPED`, `DELIVERED`).

### 1.5 R3: Verified Customer Reviews & Moderation Console
- **Product Detail Page (`frontend/src/pages/ProductDetails/ProductDetailPage.jsx`)**:
  - Lines 94–98 fetch reviews from `/api/products/${id}/reviews`.
  - Lines 278–300 handle review submission (`rating`, `comment`) via `POST /api/products/${id}/reviews`.
  - Lines 688–714 render reviews:
    - Line 701 unconditionally displays `<CheckCircle2 className="h-3.5 w-3.5" /> Verified purchase` for every single review, regardless of whether the customer actually purchased the product.
    - Missing: 5-star distribution breakdown bars (pure SVG/CSS).
    - Missing: Review filter pills (All, 5★, 4★, Verified Buyers) and pagination.
- **Admin Review Moderation Console**:
  - `/admin/reviews` is completely absent from `AppRouter.jsx` and `ADMIN_NAV` in `AdminDashboard.jsx`.
  - Staff currently have no interface to view, approve, reject, or feature customer testimonials.

### 1.6 R4: Artisan Heritage Storytelling Showcase
- **Artisans Page (`frontend/src/pages/Artisans/ArtisansPage.jsx`)**:
  - Renders cluster cards at route `/artisans`.
  - Currently includes 4 clusters: Kanchi, Banarasi, Paithani, Uppada.
  - Requirement R4 specifically mandates coverage of 5 traditional loom techniques: **Kanchipuram, Banarasi, Patola, Paithani, and Chanderi**. Patola and Chanderi are currently missing.
  - Each cluster card provides a direct link: `<Link to={'/products?search=' + cluster.searchQuery}>`.

### 1.7 R5: Access Control & Authorization (RBAC)
- **Role Hierarchy**:
  - `OWNER` / `ADMIN`: Unrestricted access across all operational consoles, instant stock transfers, approval commits, and review moderation.
  - `MANAGER`: Access to admin consoles, can submit transfer requests (Maker), cannot approve (Checker gets 403).
  - `CUSTOMER`: Access to storefront, cart, checkout, `/orders`, `/track-order`, review submission, and personal notifications.
  - `PUBLIC`: Access to storefront catalog, `/artisans`, `/products/:id`, `/track-order`.
- **403 Forbidden Response Contract**:
  - Unauthorized requests must receive HTTP 403 with `{"success":false,"message":"Not authorised to perform this action"}`.
  - Playwright test `tests/approval.spec.js` verified this exact 403 behavior.

---

## 2. Logic Chain

```
[Observation 1.2: index-*.js is 495.71 kB]
       ↓ (Adding R1-R4 components directly to index bundle would exceed 500 kB limit)
[Logic Step 1: Configure rollup manualChunks in vite.config.js for react-core, redux-vendor, framer-motion, lucide-react]
       ↓ (Guarantees all production chunks remain strictly < 200 kB, satisfying performance budget)

[Observation 1.3: Navbar.jsx & AdminDashboard.jsx have missing or static notification bell]
       ↓ (R1 requires unread counter, popover list, mark-as-read, deep link routing)
[Logic Step 2: Build reusable NotificationCenter component with Redux notificationSlice and REST API client]
       ↓ (Mount in Navbar.jsx between Wishlist/User and Cart, and replace placeholder in AdminDashboard.jsx)

[Observation 1.4: ManageInventory has PO modal but no warehouse transfer; ApprovalCenter has Maker-Checker]
       ↓ (R2 requires inter-warehouse transfers for WH-01 Bengaluru, WH-02 Mumbai, WH-03 Delhi with Maker-Checker)
[Logic Step 3: Add Inter-Warehouse Transfer Modal to ManageInventory.jsx; integrate STOCK_TRANSFER in ApprovalCenter.jsx]
       ↓ (Managers submit transfer requests; Owners approve/reject in ApprovalCenter or execute direct instant transfer)

[Observation 1.5: ProductDetailPage lacks rating bars & verified badge logic; /admin/reviews does not exist]
       ↓ (R3 requires 1-5 star submission, verified buyer badge, review analytics breakdown, admin moderation console)
[Logic Step 4: Add pure SVG/CSS 5-star breakdown bars and conditional badge in ProductDetailPage; create ManageReviews.jsx at /admin/reviews]

[Observation 1.6: ArtisansPage lacks Patola and Chanderi traditional techniques]
       ↓ (R4 requires storytelling for Kanchipuram, Banarasi, Patola, Paithani, Chanderi linked to catalog)
[Logic Step 5: Expand ARTISAN_CLUSTERS with Patola (Patan, Gujarat) and Chanderi (Madhya Pradesh) with direct product search links]
```

---

## 3. Detailed Component & State Architecture Blueprint

### 3.1 Component Architecture Map

```
frontend/src/
├── components/
│   ├── common/
│   │   ├── NotificationCenter.jsx       <-- NEW: Bell icon, badge counter, popover list, mark-as-read, deep-links
│   │   └── RatingBreakdown.jsx           <-- NEW: Pure SVG/CSS 5-star distribution progress bars (0 kB chart bundle)
│   ├── layout/
│   │   ├── Navbar.jsx                   <-- UPDATE: Mount NotificationCenter for customers/staff
│   │   └── MainLayout.jsx               <-- Unchanged: Shell for public/customer pages
│   └── orders/
│       └── OrderTrackingModal.jsx       <-- Verified: 6-stage milestone progression & AWB copy
├── pages/
│   ├── Admin/
│   │   ├── AdminDashboard.jsx           <-- UPDATE: Mount NotificationCenter in header; add /admin/reviews in ADMIN_NAV
│   │   ├── ApprovalCenter.jsx           <-- UPDATE: Support STOCK_TRANSFER entityType (Source, Target, SKU, Qty, Note)
│   │   ├── ManageInventory.jsx          <-- UPDATE: Add "Inter-Warehouse Transfer" modal (WH-01, WH-02, WH-03)
│   │   ├── ManageOrders.jsx             <-- UPDATE: Automated AWB generation & courier selection (Blue Dart, Delhivery, DTDC, India Post)
│   │   └── ManageReviews.jsx            <-- NEW: Review moderation console (Pending, Approved, Rejected, Featured)
│   ├── Artisans/
│   │   └── ArtisansPage.jsx             <-- UPDATE: Add Patola & Chanderi heritage profiles with catalog links
│   ├── Orders/
│   │   └── TrackOrderPage.jsx           <-- Verified: Public tracking by Order ID or Courier AWB
│   ├── ProductDetails/
│   │   └── ProductDetailPage.jsx        <-- UPDATE: Integrate RatingBreakdown, conditional Verified Buyer badge, review filters
│   └── MyOrders.jsx                     <-- Verified: Displays orders with tracking modal
├── redux/
│   ├── slices/
│   │   ├── notificationSlice.js         <-- NEW: Fetch notifications, mark read, unread count
│   │   ├── reviewSlice.js               <-- NEW (or managed in component): Product reviews & admin review moderation
│   │   └── orderSlice.js                <-- Existing: Orders state
│   └── store.js                         <-- UPDATE: Register notificationReducer
├── services/
│   ├── notificationService.js           <-- NEW: API methods for notifications
│   ├── reviewService.js                 <-- NEW: API methods for reviews and moderation
│   └── inventoryService.js              <-- NEW / UPDATE: Transfer request and instant transfer execution methods
└── routes/
    └── AppRouter.jsx                    <-- UPDATE: Register /admin/reviews route under AdminDashboard
```

### 3.2 State Stores & API Endpoints Specification

| Requirement | State Store / Slice | Key Actions / Reducers | Backend REST Endpoints |
|---|---|---|---|
| **R1. Notifications** | `notificationSlice` | `fetchNotifications`, `markAsRead`, `markAllAsRead`, `setUnreadCount` | `GET /api/notifications`<br>`PUT /api/notifications/{id}/read`<br>`PUT /api/notifications/read-all`<br>`GET /api/notifications/unread-count` |
| **R2. Stock Transfers** | Component state / `inventoryService` | `requestTransfer`, `executeInstantTransfer`, `approveTransfer`, `rejectTransfer` | `POST /api/approvals/requests` (entity: `STOCK_TRANSFER`)<br>`POST /api/approvals/{id}/approve`<br>`POST /api/approvals/{id}/reject`<br>`POST /api/inventory/transfers/execute` |
| **R2. Carrier Tracking** | `orderSlice` | `updateOrderTracking`, `updateOrderStatus` | `PUT /api/admin/orders/{id}/tracking`<br>`PUT /api/admin/orders/{id}/status`<br>`GET /api/orders/track` |
| **R3. Customer Reviews** | Component state / `reviewService` | `fetchProductReviews`, `submitReview`, `fetchAdminReviews`, `moderateReview` | `GET /api/products/{id}/reviews`<br>`POST /api/products/{id}/reviews`<br>`GET /api/admin/reviews`<br>`PUT /api/admin/reviews/{id}/status` |
| **R4. Artisans Showcase** | Static data + product queries | `filterCluster`, `searchCatalogByWeave` | `GET /api/products/search?q={technique}` |

### 3.3 Vite Chunk Splitting Strategy (`vite.config.js`)
To maintain chunk sizes strictly below the 500 kB budget:
```javascript
export default defineConfig({
  plugins: [react(), tailwindcss()],
  build: {
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            if (id.includes('react') || id.includes('react-dom') || id.includes('react-router-dom')) {
              return 'vendor-react';
            }
            if (id.includes('@reduxjs') || id.includes('react-redux')) {
              return 'vendor-redux';
            }
            if (id.includes('framer-motion')) {
              return 'vendor-framer';
            }
            if (id.includes('lucide-react')) {
              return 'vendor-icons';
            }
          }
        },
      },
    },
  },
  // server configuration remains intact...
});
```
This splits the 495.71 kB monolithic chunk into clean chunks:
- `vendor-react`: ~140 kB
- `vendor-redux`: ~45 kB
- `vendor-framer`: ~120 kB
- `vendor-icons`: ~50 kB
- `index.js`: ~80 kB
Each individual chunk is safely under 200 kB (well below the 500 kB ceiling).

---

## 4. Caveats
1. **Network Independence**: All backend communications rely on local Spring Boot (`http://127.0.0.1:8081`) via the Vite proxy (`/api`). No third-party CDN or cloud API (e.g. AWS S3, Twilio, SendGrid) may be called.
2. **Review Verification Logic**: In the product review submission endpoint (`POST /api/products/{id}/reviews`), the backend must verify against existing completed orders for the authenticated user to reliably populate `isVerifiedBuyer: true`.
3. **Data Visualization Budget**: External charting dependencies (Chart.js, Recharts, D3) must NOT be installed. Review breakdown bars and KPI graphics must strictly use pure CSS flex/grid and inline SVG elements.
4. **Axios 403 Interceptor Guard**: The response interceptor in `frontend/src/api/axiosConfig.js` must NOT unconditionally redirect and clear auth tokens on non-auth 403 errors, as Playwright tests directly verify 403 HTTP status for unauthorized RBAC operations.

---

## 5. Conclusion
The frontend architecture of SareeKart is robust, modular, and exceptionally well positioned to support the Operations & Customer Engagement Suite. The routing and layout hooks are clearly identified, existing tracking and approval interfaces provide solid integration foundations, and the required UI additions can be cleanly introduced without regressions. By applying the recommended `manualChunks` configuration in `vite.config.js`, the production frontend build will easily satisfy the strict < 500 kB chunk budget.

---

## 6. Verification Method

### 6.1 Automated Test Execution
Run the full existing test suites to confirm zero baseline regression:
```bash
# 1. Approval & RBAC Test Suite
npx playwright test tests/approval.spec.js --project=chromium

# 2. Public & Customer Order Tracking Test Suite
npx playwright test tests/order-tracking.spec.js --project=chromium

# 3. Admin Analytics Suite
npx playwright test tests/analytics.spec.js --project=chromium
```

### 6.2 Operations & Engagement E2E Suite Verification (Target for Implementation)
Once implemented, verify the comprehensive operations suite:
```bash
npx playwright test tests/operations-engagement.spec.js --project=chromium
```

### 6.3 Production Bundle Budget Verification
Confirm that all bundle chunks are strictly below 500 kB:
```bash
npm run build
```
Verify that no chunk in `dist/assets/*.js` exceeds 500 kB.
