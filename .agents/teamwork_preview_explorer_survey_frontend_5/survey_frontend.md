# SareeKart Frontend Architecture Survey & Design Specification
## Self-Service Returns & Exchanges Suite (v3.0 Module 1)

**Document Reference**: `teamwork_preview_explorer_survey_frontend_5/survey_frontend.md`  
**Date**: 2026-09-11  
**Status**: Survey & Architectural Design Complete  
**Integrity Mode**: Development / Offline Verification  

---

## 1. Executive Summary & Problem Boundary

The SareeKart platform requires an enterprise-grade, customer self-service Returns and Exchanges suite with reverse logistics moderation for store operators (`OWNER`, `MANAGER`, `ADMIN`). 

The current storefront enables users to place orders, download PDF tax invoices, track outbound shipments via `OrderTrackingModal.jsx`, and cancel orders during early `PENDING` / `PROCESSING` phases. However, once an order transitions to `DELIVERED`, customers have no structured in-app mechanism to initiate defect claims, request drapery exchanges, or track reverse pickups. Simultaneously, admin operators lack a centralized console to inspect photographic evidence of defects (such as zari fraying or dye bleed), assign reverse carrier Air Waybills (AWBs), and authorize refunds.

This survey establishes the complete technical blueprint for:
1. **Customer Self-Service Flow**:
   - 7-day post-delivery eligibility gate on `src/pages/MyOrders.jsx`.
   - Accessible `ReturnRequestModal.jsx` with return/exchange selection, 6-item reason taxonomy, client-side drag-and-drop defect photo uploader (max 3 photos), and refund mode preferences.
   - Dynamic Order Card Return Telemetry on `MyOrders.jsx` showing real-time claim status pills, rejection banners, and a "View Return Status" tracking milestones drawer (`ReturnStatusDrawer.jsx`).
2. **Admin Moderation & Reverse Logistics Console**:
   - `src/pages/Admin/ManageReturns.jsx` mounted at `/admin/returns`.
   - 4-card telemetry metrics bar, status tabs, search & filtering, side-by-side photo inspection lightbox, and 1-click state transition controls (Approve, Assign Courier & AWB, Complete Refund, Reject with reason).
3. **API & Services Layer**:
   - `src/services/returnService.js` utilizing the configured Axios instance with Bearer token authentication and `multipart/form-data` uploads.
4. **Bundle & Performance Discipline**:
   - Strict adherence to chunk sizes `< 500 kB`, pure Tailwind CSS v4, pure SVG / Lucide icons, and zero heavy external charting/modal dependencies.

---

## 2. Codebase Architecture Observations

### 2.1 Routing & Navigation Architecture
- **Router Entrypoint**: `src/routes/AppRouter.jsx` employs React 19 `lazy()` and `Suspense` with route-level code splitting.
  - Customer orders view is rendered at path `/orders` mapped to `const MyOrders = lazy(() => import('../pages/MyOrders'))` inside a protected customer shell.
  - Admin workspace is nested under `/admin` wrapped by `ProtectedRoute adminOnly={true}` and rendered via `AdminDashboard.jsx` (which utilizes `<Outlet />`).
  - Existing admin commerce routes include: `/admin/coupons`, `/admin/inventory`, `/admin/reviews`, `/admin/analytics`, `/admin/excel-transactions`, `/admin/finance`, `/admin/invoices`, `/admin/cms`.
  - **Proposed Integration**: Add `const ManageReturns = lazy(() => import('../pages/Admin/ManageReturns'))` and register `<Route path="returns" element={<ManageReturns />} />`.
- **Admin Sidebar**: `src/pages/Admin/AdminDashboard.jsx` defines navigation items in `ADMIN_NAV`.
  - Grouping taxonomy: `Store`, `Commerce`, `Operations`, `AI Studio`.
  - **Proposed Integration**: Add `{ path: '/admin/returns', icon: RotateCcw, label: 'Returns & Exchanges', group: 'Commerce' }`.

### 2.2 Customer Orders View (`src/pages/MyOrders.jsx`)
- **Current Layout**:
  - Top summary header displaying total orders, active count, and total spent.
  - Filter pills: `All`, `PENDING`, `PROCESSING`, `SHIPPED`, `DELIVERED`, `CANCELLED`.
  - Card grid: Each order article displays primary item thumbnail, title, `#SK-{order.id}`, date, payment method, delivery city, and total price.
  - Existing Actions on Card:
    - "Tax Invoice (PDF)" -> downloads invoice blob via `invoiceService`.
    - "Track Shipment" -> opens `OrderTrackingModal`.
    - "Reorder" -> dispatches items to `cartSlice`.
    - "Cancel" -> dispatches `cancelUserOrder` if status is `PENDING` or `PROCESSING`.
- **Delivered Timestamp Handling**:
  - `OrderResponse.java` and API return `createdAt` and `updatedAt`.
  - When an order reaches `DELIVERED`, the delivery timestamp is captured in `order.deliveredAt` or falls back to `order.updatedAt`.
  - 7-day eligibility calculation:
    $$\Delta t_{\text{days}} = \left\lfloor \frac{t_{\text{now}} - t_{\text{delivery}}}{1000 \times 60 \times 60 \times 24} \right\rfloor$$
    An order is eligible if and only if $\text{status} = \text{'DELIVERED'}$ and $0 \le \Delta t_{\text{days}} \le 7$.

### 2.3 Existing Component Patterns
- **Photo Uploaders**:
  - `src/components/admin/SareePhotoDropzone.jsx` provides the established drag-and-drop UX: `onDrop`, `onDragOver`, hidden `<input type="file">`, client preview grid with delete overlay, file size/type validation (`image/jpeg, image/png, image/webp`), and `FormData` upload via Axios.
  - This pattern will be adapted for customer defect uploads in `ReturnRequestModal.jsx`.
- **Modals & Drawers**:
  - `src/components/orders/OrderTrackingModal.jsx` establishes modal backdrop styling (`bg-black/60 backdrop-blur-xs`), keyboard Escape listener, click-outside dismissal, milestone timeline stepper, and WhatsApp support integration.
- **Admin Moderation Consoles**:
  - `src/pages/Admin/ManageReviews.jsx` and `ApprovalCenter.jsx` establish tabbed workflows (`ALL`, `PENDING`, `APPROVED`, etc.), tabular layout with action triggers, feedback alerts, and modal dialogs for rejection rationale.

### 2.4 API Client Layer (`src/api/axiosConfig.js`)
- Configured with `baseURL: '/api'`, default `'Content-Type': 'application/json'`, and 10s timeout.
- Request interceptor automatically reads `localStorage.getItem('sareekart_token')` and appends `Authorization: Bearer <token>`.
- For file uploads, passing `headers: { 'Content-Type': 'multipart/form-data' }` overrides JSON serialization seamlessly.
- Response interceptor handles 401/403 redirection while preserving redirect query parameters.

### 2.5 Bundle Budget & Production Chunks
- Production build inspection (`npm run build`) verifies:
  - React/DOM vendor chunk: ~227 kB.
  - Framer Motion vendor chunk: ~132 kB.
  - Lucide icons vendor chunk: ~29 kB.
  - Redux Toolkit chunk: ~21 kB.
  - All application page chunks are between 4 kB and 48 kB.
  - **Constraint Verification**: All chunk sizes remain strictly under the 500 kB budget. No external heavyweight chart or lightbox libraries should be introduced.

---

## 3. Customer Returns & Exchanges Flow Design

### 3.1 Eligibility Gate & Order Card Actions (`MyOrders.jsx`)

On each order card in `MyOrders.jsx`, an eligibility evaluation occurs:

```javascript
// Eligibility Gate Evaluation
const isDelivered = order.status?.toUpperCase() === 'DELIVERED';
const deliveryDate = order.deliveredAt || order.updatedAt || order.createdAt;
const daysSinceDelivery = deliveryDate
  ? Math.floor((new Date() - new Date(deliveryDate)) / (1000 * 60 * 60 * 24))
  : 999;
const isEligibleForReturn = isDelivered && daysSinceDelivery >= 0 && daysSinceDelivery <= 7;
const daysRemaining = Math.max(0, 7 - daysSinceDelivery);
```

#### State 1: Active Return / Exchange Claim Exists
If a return claim has already been submitted for this order (matched via `orderReturnMap[order.id]`):
- **Card Header Pill**: Displays live return telemetry pill:
  - `PENDING`: `Return: Pending Review` (Amber badge, `Clock` icon)
  - `APPROVED`: `Return: Approved` (Teal badge, `ShieldCheck` icon)
  - `PICKUP_SCHEDULED`: `Pickup Scheduled - {reverseCourier} (AWB: {reverseTrackingNumber})` (Indigo badge, `Truck` icon)
  - `COMPLETED`: `Return Completed` (Emerald badge, `CheckCircle2` icon)
  - `REJECTED`: `Return Rejected` (Rose badge, `XCircle` icon)
- **Rejection Notice Box**: If `REJECTED`, renders an alert box on the card:
  - *"Return Claim Rejected: {adminNotes || 'Does not meet condition guidelines.'}"*
- **Card Action Button**:
  - Replaces the initiate button with **"View Return Status"** button.
  - Clicking opens `ReturnStatusDrawer.jsx` with full milestone progression and AWB tracking.

#### State 2: No Return Claim & Eligible (`isEligibleForReturn === true`)
- Renders an active button:
  ```jsx
  <button
    type="button"
    onClick={() => setReturnModalOrder(order)}
    className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#1E6A62] bg-[#E3F0ED] text-xs font-bold uppercase tracking-wider text-[#1E6A62] hover:bg-[#1E6A62] hover:text-white transition shadow-xs cursor-pointer"
  >
    <RotateCcw className="h-4 w-4" />
    <span>Return / Exchange ({daysRemaining}d left)</span>
  </button>
  ```

#### State 3: Not Eligible (`isEligibleForReturn === false`)
- Renders a disabled button with an accessible, hoverable explanation tooltip:
  ```jsx
  <div className="group relative w-full">
    <button
      type="button"
      disabled
      aria-disabled="true"
      className="flex h-10 w-full items-center justify-center gap-2 rounded-[8px] border border-[#DDD8CF] bg-[#F7F4EE] text-xs font-bold uppercase tracking-wider text-[#9AA59F] cursor-not-allowed opacity-75"
    >
      <RotateCcw className="h-4 w-4" />
      <span>Return / Exchange</span>
    </button>
    <div className="pointer-events-none absolute bottom-full left-1/2 -translate-x-1/2 mb-2 hidden w-64 rounded-lg bg-[#17211F] p-2 text-center text-xs font-medium text-white shadow-lg group-hover:block z-20">
      {!isDelivered
        ? "Returns & exchanges are only available once the order is delivered."
        : "Return window closed. 7-day post-delivery eligibility has expired."}
      <div className="absolute top-full left-1/2 -translate-x-1/2 border-4 border-transparent border-t-[#17211F]" />
    </div>
  </div>
  ```

---

### 3.2 Component: `src/components/orders/ReturnRequestModal.jsx`

#### Functional Specifications:
- **Dialog Shell**: Accessible modal dialog (`role="dialog"`, `aria-modal="true"`) with backdrop blur and escape key listener.
- **Order Header Summary**:
  - Displays Order `#SK-{order.id}`, item count, order total, and primary saree image with title.
- **Return Type Selector**:
  - Segmented toggle between **"Return for Refund"** (`RETURN`) and **"Exchange Saree"** (`EXCHANGE`).
- **Reason Taxonomy**:
  Radio or styled selection pills matching backend enum:
  1. `COLOR_MISMATCH`: Color Mismatch (Shade differs noticeably from studio pictures)
  2. `ZARI_DEFECT`: Zari / Weave Defect (Tarnished, frayed, loose, or broken metallic threads)
  3. `FABRIC_FEEL`: Fabric Feel / Texture (Silk weight or texture not as expected)
  4. `INCORRECT_ITEM`: Incorrect Item Delivered (Wrong SKU or design sent)
  5. `SIZE_MISMATCH`: Length / Blouse Piece Deficient (Saree or blouse fabric short of standard measurements)
  6. `OTHER`: Other Issue (Detailed in customer comments)
- **Defect Photo Uploader**:
  - Drag-and-drop zone supporting up to 3 defect photographs.
  - Accepts `image/jpeg`, `image/png`, `image/webp` (max 10 MB each).
  - Client-side thumbnail preview grid with image loading states and removal (`X`) buttons.
  - Immediate upload to `POST /api/returns/upload-photo` returning hosted URLs (`/uploads/return-photos/...`).
- **Refund Preference Selector**:
  - `ORIGINAL_PAYMENT`: Original Payment Method (Source Bank / UPI, processed in 3–5 days).
  - `STORE_CREDIT`: SareeKart Store Credit (Instant wallet credit + 5% patronage bonus).
  - `EXCHANGE_DRAPE`: Exchange Replacement Drape (Ship replacement saree once return is verified).
  - Note: Selecting "Exchange Saree" automatically defaults and locks to `EXCHANGE_DRAPE`.
- **Customer Comments & Exchange SKU**:
  - Textarea for detailed defect description (minimum 10 characters required for validation).
  - Optional field for replacement saree SKU if `EXCHANGE` selected.
- **Submit Action**:
  - Dispatches `returnService.submitReturnRequest({ orderId, type, reason, comments, images, refundMode, exchangeSku })`.
  - Disables button and displays loading spinner during network request.
  - Displays toast confirmation on success and refreshes user order return telemetry.

---

### 3.3 Component: `src/components/orders/ReturnStatusDrawer.jsx`

#### Functional Specifications:
- Displays complete reverse logistics telemetry:
  - Return Claim ID `#RET-{claim.id}` and Order `#SK-{order.id}`.
  - Return Type: `RETURN` (Refund) or `EXCHANGE` (Replacement).
  - Refund Amount / Drape Mode.
- **Reverse Courier & AWB Tracker**:
  - Reverse Courier: e.g. `BlueDart Reverse Logistics`, `Delhivery Reverse`.
  - Reverse Tracking AWB with 1-click clipboard copy button (`Copy` / `Check` feedback).
  - Handover Instructions: *"Please place saree in the original keepsake box with tags intact. Hand over to courier agent only after verifying the return label."*
- **6-Stage Reverse Logistics Milestone Stepper**:
  1. `Request Lodged`: Claim submitted with condition photos.
  2. `Atelier Review`: SareeKart quality team verifies photos and claim reason.
  3. `Return Authorized`: Claim approved for doorstep reverse pickup.
  4. `Courier Dispatched`: Reverse AWB assigned and pickup agent en route.
  5. `Quality Inspection`: Item received at central artisan guild hub and inspected.
  6. `Refund / Exchange Completed`: Funds disbursed to original source / store credit or exchange saree shipped.
- **Rejection Notice Box**:
  - If `status === 'REJECTED'`, displays a prominent amber/rose banner detailing the admin's rejection note.
  - Includes a direct "WhatsApp Concierge" button linking to SareeKart support (`wa.me/919059564499`) with pre-filled claim details for dispute review.
- **Uploaded Condition Photos Gallery**:
  - Shows customer-uploaded photos in an inspection thumbnail strip.

---

## 4. Admin Moderation Console Design (`ManageReturns.jsx`)

### 4.1 Route & Access Control
- Route: `/admin/returns`
- Access: Restrict to staff roles `OWNER`, `MANAGER`, `ADMIN`.
- If an unauthenticated user or non-staff customer navigates to `/admin/returns`, they receive HTTP 403 Forbidden with `{ "success": false, "message": "Not authorised to perform this action" }` and are redirected.

### 4.2 KPI Summary Metrics Bar
Four responsive summary metric cards at the top of the console:
1. **Total Claims**:
   - Total returns & exchanges lodged.
   - Aggregate refund value represented.
   - Icon: `RotateCcw` in neutral dark `#17211F`.
2. **Pending Review**:
   - Claims awaiting initial moderation (`status === 'PENDING'`).
   - Highlighted in amber (`bg-amber-50 text-amber-900 border-amber-200`).
   - Icon: `Clock`.
3. **Pickups Scheduled**:
   - Items in active reverse transit with assigned AWBs (`status === 'PICKUP_SCHEDULED'`).
   - Highlighted in teal (`bg-[#E3F0ED] text-[#1E6A62] border-[#B8D8D1]`).
   - Icon: `Truck`.
4. **Completed Refunds**:
   - Successfully inspected and refunded/exchanged claims (`status === 'COMPLETED'`).
   - Highlighted in emerald (`bg-emerald-50 text-emerald-800 border-emerald-200`).
   - Icon: `CheckCircle2`.

### 4.3 Filterable Claims Table & Search
- **Status Filter Tabs**:
  - `ALL` (All claims)
  - `PENDING` (Pending initial approval)
  - `APPROVED` (Approved, awaiting courier booking)
  - `PICKUP_SCHEDULED` (In reverse transit)
  - `COMPLETED` (Refunded / Exchanged)
  - `REJECTED` (Disallowed claims)
- **Search Bar**:
  - Instant text filter matching Claim ID (`#RET-`), Order ID (`#SK-`), Customer Name, or AWB tracking number.
- **Table Columns**:
  1. **Claim ID**: `#RET-{claim.id}` with submission timestamp.
  2. **Order Reference**: `#SK-{claim.orderId}`, order value, customer name & city.
  3. **Type & Reason**: Badge for `RETURN` vs `EXCHANGE`, reason pill (e.g. `ZARI_DEFECT`, `COLOR_MISMATCH`).
  4. **Refund / Exchange**: Refund amount (INR) and mode (`ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE`).
  5. **Defect Photos**: Interactive thumbnail stack. Clicking any thumbnail opens the Photo Inspection Lightbox.
  6. **Reverse Logistics**: Courier partner name and AWB tracking code.
  7. **Status**: Color-coded pill (`PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `COMPLETED`, `REJECTED`).
  8. **1-Click Moderation Actions**:
     - Status-sensitive action buttons.

---

### 4.4 One-Click Moderation Action Controls

```
                                  ┌─────────────────────────────┐
                                  │           PENDING           │
                                  └──────────────┬──────────────┘
                                                 │
                        ┌────────────────────────┴────────────────────────┐
                        │ [Approve Return]                                │ [Reject Return]
                        ▼                                                 ▼
        ┌───────────────────────────────┐                 ┌───────────────────────────────┐
        │           APPROVED            │                 │           REJECTED            │
        └───────────────┬───────────────┘                 │  (Mandatory Reason Recorded)  │
                        │                                 └───────────────────────────────┘
                        │ [Assign Courier & Schedule AWB]
                        ▼
        ┌───────────────────────────────┐
        │       PICKUP_SCHEDULED        │
        │    (Reverse AWB Assigned)     │
        └───────────────┬───────────────┘
                        │
                        │ [Complete Refund / Exchange]
                        ▼
        ┌───────────────────────────────┐
        │           COMPLETED           │
        │  (Funds Disbursed / Exchanged)│
        └───────────────────────────────┘
```

1. **Approve Return**:
   - Single-click action button that updates status to `APPROVED`.
   - Informs customer that condition photos have passed preliminary review.
2. **Assign Courier & Schedule Pickup Modal**:
   - Triggered when claim is in `APPROVED` (or direct from `PENDING`).
   - Fields:
     - **Carrier Partner**: Dropdown (`BlueDart Reverse Logistics`, `Delhivery Reverse`, `DTDC Express`, `India Post Speed Post`).
     - **Reverse AWB Tracking Number**: Auto-generated suggested AWB (e.g., `BDR-RET-${claim.orderId}-${Date.now().toString().slice(-4)}`) or manual carrier barcode scan.
     - **Pickup Instructions / Notes**: Optional field for warehouse intake instructions.
   - Advances claim status to `PICKUP_SCHEDULED`.
3. **Complete Refund / Exchange**:
   - Triggered once reverse shipment has arrived at the atelier hub.
   - Advances status to `COMPLETED` and marks refund processed.
4. **Reject Return Modal**:
   - Triggered from `PENDING` or `APPROVED`.
   - Enforces a **mandatory rejection reason**:
     - Pre-configured policy chips:
       - *"Item shows evidence of wear, perfume, or washing."*
       - *"Security tag / Silk Mark authentic seal removed."*
       - *"Defect not substantiated by submitted condition photographs."*
       - *"Return window exceeded policy (7 days from delivery)."*
       - *"Blouse piece has already been cut or altered."*
     - Custom notes textarea.
   - Submits reason and advances status to `REJECTED`. Customer's order card immediately reflects the rejection explanation.

---

### 4.5 Component: Side-by-Side Photo Inspection Drawer / Lightbox

When an operator clicks any defect thumbnail in `ManageReturns.jsx`, an inspection lightbox slides into view:
- **Left Pane / Gallery**: High-resolution zoomable display of customer defect photos (up to 3 photos) with thumb selector.
- **Right Pane / Context**:
  - Customer claim comments verbatim.
  - Saree catalog photo and specifications (Weave type, Zari purity, Silk Mark ID) for side-by-side comparison.
  - Direct action buttons (Approve / Reject / Assign Courier) inside the lightbox so the admin does not have to navigate away.

---

## 5. API Client & Services Layer Specification

### 5.1 Service: `src/services/returnService.js`

```javascript
import api from '../api/axiosConfig';

/**
 * Return & Exchange API Service for SareeKart
 */
const returnService = {
  // ==========================================
  // Customer Endpoints
  // ==========================================

  /**
   * Submit a new customer return / exchange request
   * @param {Object} data { orderId, type, reason, comments, images, refundMode, exchangeSku }
   */
  submitReturnRequest: async (data) => {
    const response = await api.post('/returns', data);
    return response.data;
  },

  /**
   * Fetch all return claims submitted by the authenticated customer
   */
  getMyReturns: async () => {
    const response = await api.get('/returns/my-requests');
    return response.data;
  },

  /**
   * Fetch return claim status for a specific order
   * @param {number|string} orderId
   */
  getOrderReturnStatus: async (orderId) => {
    const response = await api.get(`/returns/order/${orderId}`);
    return response.data;
  },

  /**
   * Upload a condition/defect photograph
   * @param {File} file
   */
  uploadReturnPhoto: async (file) => {
    const formData = new FormData();
    formData.append('file', file);
    const response = await api.post('/returns/upload-photo', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return response.data;
  },

  // ==========================================
  // Admin & Staff Moderation Endpoints
  // ==========================================

  /**
   * Fetch filterable return claims for admin moderation
   * @param {string} status 'ALL' | 'PENDING' | 'APPROVED' | 'PICKUP_SCHEDULED' | 'COMPLETED' | 'REJECTED'
   */
  getAllAdminReturns: async (status = 'ALL') => {
    const url = status && status !== 'ALL' ? `/admin/returns?status=${status}` : '/admin/returns';
    const response = await api.get(url);
    return response.data;
  },

  /**
   * Update return claim status (Approve, Assign Courier, Complete, Reject)
   * @param {number|string} id Claim ID
   * @param {Object} statusData { status, reverseCourier, reverseTrackingNumber, adminNotes, rejectionReason }
   */
  updateReturnStatus: async (id, statusData) => {
    const response = await api.put(`/admin/returns/${id}/status`, statusData);
    return response.data;
  },
};

export default returnService;
```

### 5.2 Resilient Offline Mock Fallbacks
To ensure flawless performance in offline test environments (e.g. before the backend database migration is executed or during UI component unit previews), `returnService` or the components include mock data fallbacks:

```javascript
export const MOCK_RETURN_CLAIMS = [
  {
    id: 101,
    orderId: 37,
    userId: 2,
    customerName: 'Kalyani Sundaram',
    customerEmail: 'kalyani@example.com',
    type: 'RETURN',
    reason: 'COLOR_MISMATCH',
    comments: 'The saree shade is deep crimson rather than vermilion red displayed on the product page.',
    status: 'PICKUP_SCHEDULED',
    images: [
      'https://images.unsplash.com/photo-1610030469983-98e550d6193c?auto=format&fit=crop&fm=webp&w=600&q=80',
      'https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 18500,
    refundMode: 'ORIGINAL_PAYMENT',
    exchangeSku: null,
    reverseCourier: 'BlueDart Reverse Logistics',
    reverseTrackingNumber: 'BDR-89214',
    adminNotes: 'Condition verified from photos. Pickup scheduled.',
    createdAt: new Date(Date.now() - 2 * 86400000).toISOString(),
    updatedAt: new Date(Date.now() - 1 * 86400000).toISOString()
  },
  {
    id: 102,
    orderId: 38,
    userId: 3,
    customerName: 'Meenakshi Iyer',
    customerEmail: 'meenakshi@example.com',
    type: 'EXCHANGE',
    reason: 'ZARI_DEFECT',
    comments: 'Frayed metallic zari threads on the lower pallu border.',
    status: 'PENDING',
    images: [
      'https://images.unsplash.com/photo-1583391733956-3750e0ff4e8b?auto=format&fit=crop&fm=webp&w=600&q=80'
    ],
    refundAmount: 24000,
    refundMode: 'EXCHANGE_DRAPE',
    exchangeSku: 'KAN-SILK-MRN-02',
    reverseCourier: null,
    reverseTrackingNumber: null,
    adminNotes: null,
    createdAt: new Date(Date.now() - 12 * 3600000).toISOString(),
    updatedAt: new Date(Date.now() - 12 * 3600000).toISOString()
  }
];
```

---

## 6. Frontend File & Route Integration Plan

### 6.1 Routing in `src/routes/AppRouter.jsx`
```javascript
// Add lazy import
const ManageReturns = lazy(() => import('../pages/Admin/ManageReturns'));

// Under the protected admin route:
<Route 
  path="/admin" 
  element={
    <ProtectedRoute adminOnly={true}>
      <AdminDashboard />
    </ProtectedRoute>
  }
>
  ...
  <Route path="reviews" element={<ManageReviews />} />
  <Route path="returns" element={<ManageReturns />} />
  <Route path="invoices" element={<InvoicesPage />} />
  ...
</Route>
```

### 6.2 Sidebar Navigation in `src/pages/Admin/AdminDashboard.jsx`
```javascript
import {
  ...
  RotateCcw,
} from 'lucide-react';

const ADMIN_NAV = [
  ...
  { path: '/admin/coupons', icon: BadgePercent, label: 'Coupons', group: 'Commerce' },
  { path: '/admin/inventory', icon: Boxes, label: 'Inventory', group: 'Commerce' },
  { path: '/admin/returns', icon: RotateCcw, label: 'Returns & Exchanges', group: 'Commerce' },
  { path: '/admin/reviews', icon: MessageSquare, label: 'Reviews', group: 'Commerce' },
  { path: '/admin/analytics', icon: BarChart3, label: 'Analytics', group: 'Commerce' },
  ...
];
```

---

## 7. Complete Component Architecture & Implementation Blueprint

### 7.1 Detailed Component Specifications

```
src/
├── api/
│   └── axiosConfig.js            # Existing (token auth + multipart support)
├── services/
│   └── returnService.js          # NEW: submitReturn, getMyReturns, uploadPhoto, admin moderation
├── components/
│   └── orders/
│       ├── OrderTrackingModal.jsx   # Existing outbound shipment tracker
│       ├── ReturnRequestModal.jsx   # NEW: Customer self-service modal with 7d check, photos, preferences
│       └── ReturnStatusDrawer.jsx   # NEW: Customer return telemetry drawer & milestones
└── pages/
    ├── MyOrders.jsx                 # MODIFIED: 7d eligibility gate, return telemetry pills, action triggers
    └── Admin/
        ├── AdminDashboard.jsx       # MODIFIED: Added Returns & Exchanges to Commerce nav
        └── ManageReturns.jsx        # NEW: Admin moderation console with metrics, table, AWB & reject modals
```

---

## 8. Bundle Budget & Performance Considerations

1. **Chunk Size Verification**:
   - `ManageReturns.jsx` will compile to an isolated chunk of ~14–18 kB (gzipped: ~4.5 kB).
   - `ReturnRequestModal.jsx` and `ReturnStatusDrawer.jsx` will be lazy-loaded alongside `MyOrders.jsx` or as dynamic imports, contributing < 10 kB.
   - All chunks remain comfortably below the 480 kB Vite limit and well within the 500 kB project budget.
2. **Asset Optimization**:
   - Zero heavy UI dependencies (no jQuery, no bulky lightbox packages).
   - Defect photos are served with modern formats (`webp`, `png`, `jpeg`) and lazy-loaded with placeholder fallback images.
3. **Accessibility**:
   - Modals implement `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, and keyboard Escape trapping.
   - Disabled return buttons provide screen-reader accessible tooltips (`aria-disabled="true"`).

---

## 9. Verification & Automated Testing Plan

### 9.1 Playwright End-to-End Test Suite (`frontend/tests/returns-exchanges.spec.js`)
Test scenarios to be implemented and verified:
1. **7-Day Eligibility Gate**:
   - Sign in as customer and navigate to `/orders`.
   - Non-delivered order (`SHIPPED` / `PENDING`) exhibits disabled "Return / Exchange" button with explanatory tooltip.
   - Delivered order within 7 days displays active "Return / Exchange" button.
2. **Customer Return Submission**:
   - Click "Return / Exchange" on eligible delivered order card.
   - Modal opens: select "Return for Refund", select reason `COLOR_MISMATCH`, upload defect photo, select `ORIGINAL_PAYMENT`, type comments.
   - Submit claim; modal closes, and order card immediately displays `Return: Pending Review` status pill.
3. **Telemetry & Milestones Drawer**:
   - Click "View Return Status" on order card.
   - Milestones drawer opens displaying 6-stage transit progression, AWB details, and condition photos.
4. **Admin Moderation & Reverse Logistics**:
   - Sign in as Admin/Owner and navigate to `/admin/returns`.
   - Verify KPI cards render (Total Claims, Pending Review, etc.).
   - Locate pending return claim; click photo thumbnail to verify high-res defect lightbox.
   - Click "Approve Return" -> status advances to `APPROVED`.
   - Click "Schedule Pickup" -> select `BlueDart Reverse Logistics`, enter AWB `BDR-89214`, submit -> status advances to `PICKUP_SCHEDULED`.
   - Verify customer's order card on `/orders` now reflects `Pickup Scheduled - BlueDart Reverse Logistics (AWB: BDR-89214)`.
   - Test rejection modal with mandatory reason requirement; verify rejected badge and explanation appear on storefront.

---

## 10. Summary & Handoff Readiness

The frontend architecture for SareeKart's Self-Service Returns and Exchanges system is comprehensively mapped and ready for execution. All components, styling tokens, responsive behaviors, modal dialogs, and service integrations adhere strictly to project conventions, offline execution requirements, and bundle budgets.
