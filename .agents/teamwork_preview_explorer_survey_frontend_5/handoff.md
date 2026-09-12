# Handoff Report — Frontend Architecture Survey for Returns & Exchanges

**Agent Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_frontend_5`  
**Target Module**: SareeKart v3.0 Module 1 — Self-Service Customer Returns & Exchanges  
**Type**: Hard Handoff (Investigation Complete)  

---

## 1. Observation

1. **Order Routing & View Structure**:
   - In `frontend/src/routes/AppRouter.jsx` (line 14), customer orders are imported via:
     ```javascript
     const MyOrders = lazy(() => import('../pages/MyOrders'));
     ```
     and routed at `/orders` (lines 92–99) inside a `ProtectedRoute`.
   - In `frontend/src/routes/AppRouter.jsx` (lines 103–136), admin pages are mounted under `/admin` within `ProtectedRoute adminOnly={true}`. Existing commerce routes include `analytics`, `products`, `orders`, `coupons`, `inventory`, `reviews`, `invoices`, and `cms`.
2. **Order Card Presentation & Action Buttons**:
   - In `frontend/src/pages/MyOrders.jsx` (lines 167–261), order items render inside a responsive grid. Order status pills use `statusStyles` (lines 40–47).
   - Order delivery timestamps are tracked via `order.deliveredAt` or `order.updatedAt` (matching `OrderResponse.java` lines 33–34).
   - Card action buttons (lines 224–258) currently include "Tax Invoice (PDF)", "Track Shipment" (opening `OrderTrackingModal`), "Reorder", and "Cancel" (conditional on `PENDING` / `PROCESSING`).
3. **Photo Upload Pattern**:
   - `frontend/src/components/admin/SareePhotoDropzone.jsx` (lines 33–66) handles drag-and-drop and click-to-browse uploads using browser `FormData` and `api.post` with header `'Content-Type': 'multipart/form-data'`. It provides client previews with image deletion controls and error notifications.
4. **Tracking Modal & Milestones UX**:
   - `frontend/src/components/orders/OrderTrackingModal.jsx` (lines 18–37) implements a 6-stage milestone tracker (`MILESTONES`), courier partner and AWB copy badge, destination details, and WhatsApp Concierge deep linking.
5. **Admin Moderation & Rejection Patterns**:
   - `frontend/src/pages/Admin/ManageReviews.jsx` (lines 141–160) and `ApprovalCenter.jsx` (lines 12–23) demonstrate tabbed status filters (`ALL`, `PENDING`, `APPROVED`, etc.), tabular claims listing, and rejection modals with mandatory reason validation.
   - `frontend/src/pages/Admin/AdminDashboard.jsx` (lines 39–63) defines `ADMIN_NAV` grouped into `Store`, `Commerce`, `Operations`, and `AI Studio`.
6. **API Client & Auth Interceptor**:
   - In `frontend/src/api/axiosConfig.js` (lines 7–27), Axios is configured with `baseURL: '/api'` and automatically attaches `Authorization: Bearer <token>` from `localStorage.getItem('sareekart_token')`.
7. **Bundle Budget & Build Performance**:
   - Executing `npm run build` in `frontend/` yields 0 errors. The largest vendor chunks are `vendor-react` (227.44 kB), `vendor-framer-motion` (132.83 kB), and `vendor-lucide` (29.58 kB). All chunk sizes are strictly below the 500 kB project budget.
   - Free disk space on `/System/Volumes/Data` is 79 GiB (> 30% threshold).

---

## 2. Logic Chain

1. **Eligibility Enforcement**:
   - *Premise*: R1 dictates that only orders in `DELIVERED` status within 7 calendar days are eligible for return/exchange; non-eligible orders must show a disabled button with an explanatory tooltip.
   - *Inference*: `MyOrders.jsx` can evaluate `const isDelivered = order.status?.toUpperCase() === 'DELIVERED'` and `Math.floor((Date.now() - new Date(order.deliveredAt || order.updatedAt)) / 86400000) <= 7`. If eligible, render an active "Return / Exchange" button; if not eligible, render a disabled button wrapped in a CSS hover tooltip explaining whether the order is not yet delivered or the 7-day window has expired.
2. **Duplicate Prevention & Return Telemetry**:
   - *Premise*: Backend R2 strictly rejects duplicate returns for the same order, and R5 requires real-time telemetry pills on order cards.
   - *Inference*: Customer orders should be correlated with return claims fetched via `returnService.getMyReturns()`. If a return claim exists for `order.id`, the "Return / Exchange" action is replaced with "View Return Status" (triggering `ReturnStatusDrawer.jsx`), and a telemetry badge (`Return: Pending Review`, `Pickup Scheduled`, or `Return Rejected`) is displayed alongside the order status pill.
3. **Defect Photo Ingestion**:
   - *Premise*: R1 requires customer photo evidence (up to 3 photos) with preview and upload to `uploads/return-photos/`.
   - *Inference*: Adapting the existing `SareePhotoDropzone.jsx` component architecture into `ReturnRequestModal.jsx` allows immediate drag-and-drop file ingestion, client previews, and multipart upload to `POST /api/returns/upload-photo`.
4. **Admin Moderation & Reverse Logistics**:
   - *Premise*: R4 requires an admin console at `/admin/returns` accessible by `OWNER`, `MANAGER`, and `ADMIN` with KPI metrics, claims table, photo inspection lightbox, and 1-click state transitions (Approve, Assign Courier & AWB, Complete, Reject).
   - *Inference*: Creating `src/pages/Admin/ManageReturns.jsx`, registering it in `AppRouter.jsx`, and adding an entry in `AdminDashboard.jsx` under `Commerce` with the `RotateCcw` icon satisfies all navigation and RBAC constraints while matching existing admin styling patterns (`ManageReviews.jsx`).
5. **Chunk & Performance Discipline**:
   - *Premise*: Storage and resource discipline rules require web frontend chunks to stay strictly under 500 kB and use pure SVG/Tailwind.
   - *Inference*: Implementing the lightbox and milestone timeline using native Tailwind v4 classes and Lucide React icons introduces 0 additional runtime dependencies, ensuring `ManageReturns.jsx` compiles to a lightweight chunk (~16 kB).

---

## 3. Caveats

- **Mock Fallback Handling**: If backend endpoints are still being provisioned or tested offline, `returnService.js` and `ManageReturns.jsx` should include mock fallback arrays (`MOCK_RETURN_CLAIMS`) to ensure uninterrupted UI preview and zero test breakages.
- **Delivery Timestamp Consistency**: The frontend checks `order.deliveredAt || order.updatedAt || order.createdAt` to ensure backward compatibility with older orders where `deliveredAt` was not explicitly populated.

---

## 4. Conclusion

The frontend design and architectural blueprint are complete and documented in `survey_frontend.md`. The implementation plan is partitioned into clean, decoupled files:
1. `src/services/returnService.js`: Customer and staff API integration.
2. `src/components/orders/ReturnRequestModal.jsx`: Self-service claim modal with 6-reason taxonomy and photo dropzone.
3. `src/components/orders/ReturnStatusDrawer.jsx`: Reverse logistics timeline and courier AWB tracker.
4. `src/pages/MyOrders.jsx`: 7-day eligibility gate, dynamic telemetry pills, and disabled tooltip.
5. `src/pages/Admin/ManageReturns.jsx`: Metrics cards, claims table, photo inspection drawer, courier AWB modal, and rejection modal.
6. `src/routes/AppRouter.jsx` and `src/pages/Admin/AdminDashboard.jsx`: Routing and commerce navigation links.

---

## 5. Verification Method

1. **Production Build & Bundle Size Check**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npm run build
   ```
   *Expected Result*: Build completes with 0 errors, and all chunk sizes remain strictly under 500 kB.
2. **Lint & Code Integrity**:
   ```bash
   npm run lint
   ```
   *Expected Result*: Zero ESLint errors or syntax warnings.
3. **Playwright E2E Test Suite**:
   ```bash
   npx playwright test tests/returns-exchanges.spec.js --project=chromium
   ```
   *Expected Result*:
   - Verifies 7-day eligibility gating and disabled tooltip on non-eligible orders.
   - Verifies customer claim submission with photo upload.
   - Verifies customer order card updates with return telemetry pill.
   - Verifies admin access to `/admin/returns`, photo inspection lightbox, and 1-click AWB courier assignment and rejection actions.
