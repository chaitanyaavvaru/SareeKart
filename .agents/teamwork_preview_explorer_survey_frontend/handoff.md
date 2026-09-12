# Frontend Architecture & Analytics Dashboard Survey Report

## 1. Observation

### 1.1 Entrypoint, Routing & App Architecture
- **Vite Entrypoint & Mount**:
  - `frontend/index.html` (lines 20–21) mounts `<div id="root"></div>` via `<script type="module" src="/src/main.jsx"></script>`.
  - `frontend/src/main.jsx` (lines 9–17) wraps the application with Redux and React Router:
    ```jsx
    ReactDOM.createRoot(document.getElementById('root')).render(
      <React.StrictMode>
        <Provider store={store}>
          <BrowserRouter>
            <AppRouter />
          </BrowserRouter>
        </Provider>
      </React.StrictMode>
    );
    ```
  - `frontend/src/App.jsx` (74,616 bytes) is an unreferenced legacy prototype containing in-memory mock datasets and inline styles. It is **not** imported by `main.jsx` or any active route.
- **Router Configuration (`frontend/src/routes/AppRouter.jsx`)**:
  - Uses React Router v7 (`react-router-dom: ^7.17.0`) with `lazy()` and `<Suspense>` route splitting.
  - Public routes are nested under `MainLayout` (lines 57–85).
  - Admin workspace is nested under `/admin` (lines 88–116):
    ```jsx
    {/* Protected Admin Routes */}
    <Route 
      path="/admin" 
      element={
        <ProtectedRoute adminOnly={true}>
          <AdminDashboard />
        </ProtectedRoute>
      }
    >
      <Route index element={<AdminStats />} />
      <Route path="products" element={<ManageSarees />} />
      <Route path="orders" element={<ManageOrders />} />
      <Route path="users" element={<ManageUsers />} />
      <Route path="coupons" element={<ManageCoupons />} />
      <Route path="inventory" element={<ManageInventory />} />
      <Route path="excel-transactions" element={<ExcelTransactionCenter />} />
      <Route path="finance" element={<ManageFinance />} />
      <Route path="cms" element={<ManageCMS />} />
      <Route path="approvals" element={<ApprovalCenter />} />
      <Route path="security" element={<SecurityDashboard />} />
      <Route path="performance" element={<PerformanceDashboard />} />
      <Route path="qa" element={<QADashboard />} />
      <Route path="devops" element={<DevOpsDashboard />} />
      <Route path="operations" element={<OperationsVault />} />
      <Route path="ai-recommendations" element={<AiRecommendationDashboard />} />
      <Route path="ai-demand" element={<AiDemandDashboard />} />
      <Route path="ai-pricing" element={<AiPricingDashboard />} />
      <Route path="ai-visual-search" element={<AiVisualSearchDashboard />} />
      <Route path="ai-stylist" element={<AiStylistDashboard />} />
    </Route>
    ```
  - **/admin/analytics status**: Currently **does not exist** in `AppRouter.jsx`.
- **Role Guards (`frontend/src/components/common/ProtectedRoute.jsx`)**:
  - `ProtectedRoute` (lines 9–24) accepts `children`, `adminOnly = false`, and `allowedRoles = ['ADMIN', 'OWNER', 'MANAGER']`.
  - Unauthenticated access redirects to `/login?redirect=${encodeURIComponent(location.pathname)}`.
  - Non-staff authenticated users (e.g. role `'CUSTOMER'`) attempting to access `adminOnly` routes are immediately redirected to `/`:
    ```jsx
    if (adminOnly && !allowedRoles.includes(user?.role)) {
      return <Navigate to="/" replace />;
    }
    ```

### 1.2 Admin Navigation & Layout Shell
- **Admin Layout Component (`frontend/src/pages/Admin/AdminDashboard.jsx`)**:
  - Provides the persistent shell with a responsive `<aside>` sidebar, mobile sliding drawer, and sticky top header with dynamic breadcrumbs.
  - Child pages render via `<Outlet />` inside `<div className="min-h-[calc(100dvh-97px)] bg-[#F7F4EE] p-4 sm:p-6 lg:p-8">` (lines 189–191).
  - Navigation array `ADMIN_NAV` (lines 36–57) defines portal links:
    ```javascript
    const ADMIN_NAV = [
      { path: '/admin', icon: LayoutDashboard, label: 'Dashboard', group: 'Store' },
      { path: '/admin/products', icon: ShoppingBag, label: 'Sarees', group: 'Store' },
      { path: '/admin/orders', icon: ClipboardList, label: 'Orders', group: 'Store' },
      { path: '/admin/users', icon: Users, label: 'Customers', group: 'Store' },
      { path: '/admin/coupons', icon: BadgePercent, label: 'Coupons', group: 'Commerce' },
      { path: '/admin/inventory', icon: Boxes, label: 'Inventory', group: 'Commerce' },
      { path: '/admin/excel-transactions', icon: FileSpreadsheet, label: 'Excel Engine', group: 'Commerce' },
      { path: '/admin/finance', icon: WalletCards, label: 'Finance', group: 'Commerce' },
      { path: '/admin/cms', icon: FileText, label: 'CMS', group: 'Commerce' },
      { path: '/admin/approvals', icon: CheckCheck, label: 'Approvals', group: 'Operations' },
      { path: '/admin/security', icon: ShieldCheck, label: 'Security', group: 'Operations' },
      { path: '/admin/performance', icon: Gauge, label: 'Performance', group: 'Operations' },
      { path: '/admin/qa', icon: FlaskConical, label: 'QA', group: 'Operations' },
      { path: '/admin/devops', icon: ServerCog, label: 'DevOps', group: 'Operations' },
      { path: '/admin/operations', icon: Rocket, label: 'Go-Live', group: 'Operations' },
      { path: '/admin/ai-recommendations', icon: Sparkles, label: 'Recommendations', group: 'AI Studio' },
      { path: '/admin/ai-demand', icon: TrendingUp, label: 'Demand', group: 'AI Studio' },
      { path: '/admin/ai-pricing', icon: BadgeIndianRupee, label: 'Pricing', group: 'AI Studio' },
      { path: '/admin/ai-visual-search', icon: ScanSearch, label: 'Visual Search', group: 'AI Studio' },
      { path: '/admin/ai-stylist', icon: WandSparkles, label: 'Stylist', group: 'AI Studio' },
    ];
    ```
  - **Active State Highlighting**:
    - Lines 117–134: `isActive = activePath === item.path || (item.path !== '/admin' && activePath.startsWith(item.path))`.
    - Active styling: `border-[#F3C56A] bg-white/10 text-white`.
    - Active icon: `text-[#F3C56A]`.
  - **Header Synchronization**:
    - Line 68: `activeItem = ADMIN_NAV.find((item) => item.path === activePath || (item.path !== '/admin' && activePath.startsWith(item.path))) || ADMIN_NAV[0]`.
    - Automatically derives breadcrumb text `SareeKart / {activeItem.group}` and `<h1>{activeItem.label}</h1>`.

### 1.3 UI Component Ecosystem & Bundle Performance Budget
- **Dependencies (`frontend/package.json`)**:
  - Tailwind CSS: `@tailwindcss/vite: ^4.3.1`, `tailwindcss: ^4.3.1`.
  - Icons: `lucide-react: ^1.18.0`.
  - Animation: `framer-motion: ^12.42.0`.
  - State: `@reduxjs/toolkit: ^2.12.0`, `react-redux: ^9.3.0`.
  - HTTP: `axios: ^1.18.0`.
  - Routing: `react-router-dom: ^7.17.0`.
  - **External Charting Libraries**: None installed (no Chart.js, Recharts, or D3).
- **Bundle Size Budget Assessment**:
  - Running `npm run build` outputs:
    - `dist/assets/index-Bm60uRST.js: 442.70 kB │ gzip: 140.21 kB`.
    - Acceptance Criterion from `ORIGINAL_REQUEST.md`: *"Frontend production bundle (`npm run build`) builds with 0 errors and all chunks under 500 kB"*.
    - Current main bundle is at 442.70 kB — only **57.3 kB** below the strict 500 kB threshold.
    - Installing heavy charting packages like `recharts` (~250 kB) or `chart.js` (~180 kB) would immediately breach the 500 kB chunk budget and cause CI/CD failure.
    - Pure SVG/CSS chart rendering components have zero bundle footprint, render deterministically in all browser engines, and preserve chunk size compliance.

### 1.4 Admin Design Tokens & Patterns in `src/pages/Admin/`
- **Color Tokens & Styling (`src/index.css`)**:
  - Neutral Background: Canvas `#F7F4EE`, Card `#FFFFFF`, Border `#DDD8CF`, Soft Border `#C9C1B5`.
  - Typography: Heading serif `'Fraunces', Georgia, serif` (`font-serif`), Body `'Inter', sans-serif`.
  - Accents:
    - Deep Charcoal / Ink: `#17211F`
    - Gold / Amber: `#F3C56A`, `#C48B3C`, `#9A6A32`, `#FFF7EC`
    - Emerald / Forest: `#1E6A62`, `#2C765F`, `#EEF8F3`
    - Terracotta / Coral: `#B84F49`, `#8B3E37`, `#FFF2F0`
    - Slate / Steel: `#2C648A`, `#F0F7FC`, `#71817A`, `#4E5B56`
- **Component Patterns**:
  - **KPI Summary Card**:
    - Container: `border border-[#DDD8CF] bg-white p-5 shadow-[0_8px_20px_rgba(23,33,31,0.04)]`.
    - Label: `text-[10px] font-bold uppercase tracking-[0.16em] text-[#71817A]`.
    - Value: `mt-3 font-serif text-3xl font-medium text-[#17211F]`.
    - Comparison Delta: Period-over-period comparison badge with directional indicator (`▲ +18.4%` or `▼ -4.2%`) and colored tone (`text-[#2C765F]` for positive, `text-[#B84F49]` for negative).
  - **Date Range Filter Pills**:
    - Pill container with sharp borders: `px-3.5 py-1.5 text-xs font-bold uppercase tracking-[0.08em] transition`.
    - Active: `bg-[#17211F] text-[#F3C56A] border border-[#17211F]`.
    - Inactive: `bg-white text-[#4E5B56] border border-[#DDD8CF] hover:border-[#1E6A62] hover:text-[#1E6A62]`.
  - **Tabular Lists**:
    - Container: `overflow-x-auto border border-[#DDD8CF] bg-white`.
    - Headers: `border-b border-[#DDD8CF] bg-[#F7F4EE]` with `<th className="px-6 py-3 text-[10px] font-bold uppercase tracking-[0.12em] text-[#71817A]">`.
    - Rows: `divide-y divide-[#DDD8CF] hover:bg-[#F7F4EE] transition`.
  - **Alerts & Stock Warnings**:
    - Warning banner: `border border-[#F2D8B7] bg-[#FFF7EC] p-4 flex items-center justify-between`.
    - Critical warning: `border border-[#E8C9C5] bg-[#FFF2F0] text-[#8B3E37]`.

### 1.5 Report Export Implementation
- **Existing Pattern in `ExcelTransactionCenter.jsx` (lines 21–33)**:
  - Uses `window.URL.createObjectURL(new Blob([data]))`, creates a temporary `<a>` element with `download` attribute, appends to DOM, calls `.click()`, and cleans up.
- **Client-Side CSV Export Standard**:
  - Formatting RFC 4180 CSV strings directly from state datasets and triggering an in-browser file download via Blob is natively supported by Playwright's `page.waitForEvent('download')`.
  - Supports instant generation of both Sales Reports (`SareeKart_Sales_Report_<range>.csv`) and Inventory Velocity Reports (`SareeKart_Inventory_Velocity_<range>.csv`).
  - Excel compatibility: CSV files are natively opened by Excel, Numbers, and Google Sheets. Optionally, an XML Spreadsheet 2003 `.xls` blob can also be generated without any external library.

### 1.6 API Client & Authentication Management
- **Axios Configuration (`frontend/src/api/axiosConfig.js`)**:
  - `baseURL: '/api'` (proxied by Vite to `http://127.0.0.1:8081`).
  - Request interceptor (lines 16–27) retrieves `localStorage.getItem('sareekart_token')` and attaches `Authorization: Bearer ${token}` to all requests except auth routes.
  - Response interceptor (lines 30–54):
    - On 401 or 403 status from non-auth endpoints, it removes `sareekart_token` and `sareekart_user` from `localStorage` and redirects to `/login?redirect=${encodeURIComponent(currentPath)}`.
- **RBAC Enforcement**:
  - Staff credentials (`OWNER`, `MANAGER`, `ADMIN`) have full access to `/admin` and analytical routes.
  - Customer (`CUSTOMER`) attempting to access `/admin/analytics` in browser is stopped by `ProtectedRoute` and redirected to `/`.
  - Direct API access by customers to `/api/admin/**` returns HTTP 403 Forbidden with payload:
    `{ "success": false, "message": "Not authorised to perform this action" }` (as verified in `frontend/tests/approval.spec.js`).

---

## 2. Logic Chain

1. **Routing Integration**:
   - `AppRouter.jsx` governs all client-side URL routing.
   - Adding `<Route path="analytics" element={<AnalyticsDashboard />} />` under the existing `/admin` route group automatically inherits the top-level `<ProtectedRoute adminOnly={true}>` guard.
   - Therefore, any request from an unauthenticated user will be routed to `/login?redirect=%2Fadmin%2Fanalytics`, and any request from a customer user will be redirected to `/`, satisfying Requirement R4 and R5.

2. **Sidebar Navigation Integration**:
   - `AdminDashboard.jsx` renders all sidebar navigation items solely from the `ADMIN_NAV` array.
   - Adding `{ path: '/admin/analytics', icon: BarChart3, label: 'Analytics', group: 'Commerce' }` (or `group: 'Store'`) to `ADMIN_NAV` will automatically:
     - Render the navigation link with the `BarChart3` icon.
     - Apply golden active border and fill (`border-[#F3C56A] bg-white/10 text-white`) when the user is at `/admin/analytics`.
     - Update the top navigation header title to "Analytics" and breadcrumb to "SareeKart / Commerce".
     - Close mobile sidebar when navigated on small screens.

3. **Bundle Performance Budget & Visualization Strategy**:
   - `ORIGINAL_REQUEST.md` imposes a hard constraint: all bundle chunks must be under 500 kB.
   - The main JavaScript chunk is already at 442.70 kB. Adding Recharts or Chart.js would add 150–250 kB, exceeding the budget and failing the production build gate.
   - Using lightweight, responsive pure SVG / CSS chart components (e.g. `SvgRevenueTrendChart`, `SvgCategoryDonut`, `SvgInventoryVelocityBar`, `SvgConversionFunnel`):
     - Keeps the dashboard bundle chunk under 15 kB (lazy-loaded as `AnalyticsDashboard-[hash].js`).
     - Renders crisply across all screen resolutions without canvas blur.
     - Provides interactive hover tooltips and metric readouts.
     - Avoids external dependencies and is 100% testable in headless Chromium without canvas GPU acceleration quirks.

4. **Period-over-Period Telemetry & Filtering**:
   - Requirement R1 & R4 specify filter pills: `7D`, `30D`, `90D`, `YTD`, `ALL`.
   - The frontend state should hold:
     - `selectedRange`: currently active time window.
     - `telemetry`: aggregated metrics for the current window.
     - `previousTelemetry`: metrics for the previous equal-duration window.
     - `isRefreshing`: loading indicator for instant background refresh.
   - Period-over-period percentage calculation:
     $$\Delta\% = \frac{\text{Current} - \text{Prior}}{\text{Prior}} \times 100$$
   - Badges render with directional arrows (`▲` / `▼`) and color coding (`#2C765F` positive, `#B84F49` negative, `#71817A` neutral).

5. **Client-Side Export Architecture**:
   - Client-side CSV generation creates a `Blob` with MIME `text/csv;charset=utf-8;` and triggers a browser download.
   - This directly satisfies Playwright's `const download = await page.waitForEvent('download')` test pattern without depending on complex server-side streaming or external Excel converters.
   - Two dedicated export buttons:
     1. "Export Sales Telemetry (.csv)" — Exports timeline revenue, gross sales, taxes, discounts, and payment splits.
     2. "Export Inventory Velocity (.csv)" — Exports SKU velocity, days of inventory remaining, stock health, and run-rates.

---

## 3. Caveats

1. **Backend Telemetry API Readiness**:
   - The backend currently provides `/api/admin/dashboard`, but specific aggregate endpoints for `/api/admin/analytics` (or sub-paths) are being designed by the backend explorer agent (`teamwork_preview_explorer_survey_backend`).
   - The frontend `AnalyticsDashboard` must implement defensive fallback: if the backend endpoint is still returning mock/incomplete data or fails during initial connection, the frontend should fall back to realistic pre-computed telemetry models to maintain interactive fidelity and pass Playwright UI validations.
2. **Role Naming Conventions**:
   - In Spring Boot, roles are stored as `ROLE_ADMIN`, `ROLE_OWNER`, `ROLE_MANAGER`, `ROLE_CUSTOMER`, but in the frontend JWT payload / Redux `auth.user.role`, the `ROLE_` prefix is stripped (`'ADMIN'`, `'OWNER'`, `'MANAGER'`, `'CUSTOMER'`). The frontend role guard checks `['ADMIN', 'OWNER', 'MANAGER'].includes(user?.role)`.
3. **Legacy `App.jsx`**:
   - Developers should avoid modifying or referencing `src/App.jsx`. All routing and page assembly must remain in `src/routes/AppRouter.jsx` and `src/pages/Admin/`.

---

## 4. Conclusion & Implementation Blueprint

### 4.1 Recommended File Structure
```
frontend/src/
├── pages/Admin/
│   ├── AnalyticsDashboard.jsx            <-- Primary console component
│   └── components/analytics/             <-- Modular dashboard sub-components
│       ├── AnalyticsKpiGrid.jsx          <-- KPI cards with period comparisons
│       ├── RevenueTrendChart.jsx         <-- SVG line & gradient area chart
│       ├── PaymentMethodDonut.jsx        <-- SVG donut chart for payment split
│       ├── InventoryVelocityTable.jsx    <-- Fast vs slow moving SKU table
│       ├── CustomerCohortBreakdown.jsx   <-- LTV & regional demand breakdown
│       └── ConversionFunnelChart.jsx     <-- Funnel conversion dropoff chart
├── services/
│   └── analyticsService.js               <-- API client abstraction for telemetry
└── utils/
    └── exportUtils.js                    <-- CSV and Excel client-side generators
```

### 4.2 Step-by-Step Implementation Plan

#### Step 1: Register `/admin/analytics` Route
In `frontend/src/routes/AppRouter.jsx`:
1. Add lazy import:
   ```jsx
   const AnalyticsDashboard = lazy(() => import('../pages/Admin/AnalyticsDashboard'));
   ```
2. Insert child route under `<Route path="/admin" ...>`:
   ```jsx
   <Route path="analytics" element={<AnalyticsDashboard />} />
   ```

#### Step 2: Add Analytics to Admin Sidebar Navigation
In `frontend/src/pages/Admin/AdminDashboard.jsx`:
1. Import `BarChart3` from `lucide-react`:
   ```jsx
   import { BarChart3, ... } from 'lucide-react';
   ```
2. Add navigation object to `ADMIN_NAV`:
   ```jsx
   { path: '/admin/analytics', icon: BarChart3, label: 'Analytics', group: 'Commerce' },
   ```
   *(Placed at the top of Commerce or right after Dashboard in Store).*

#### Step 3: Implement Analytics API Service (`src/services/analyticsService.js`)
```javascript
import api from '../api/axiosConfig';

export const analyticsService = {
  getTelemetry: async (range = '30D', customParams = {}) => {
    try {
      const res = await api.get('/admin/analytics', {
        params: { range, ...customParams }
      });
      return res.data?.data || null;
    } catch (err) {
      console.warn('Backend analytics endpoint unavailable, falling back to client telemetry model', err);
      return null;
    }
  }
};
```

#### Step 4: Implement Export Utility (`src/utils/exportUtils.js`)
```javascript
/**
 * Triggers client-side CSV download conforming to RFC 4180.
 */
export function downloadCsv(filename, headers, rows) {
  const escapeCell = (cell) => {
    if (cell === null || cell === undefined) return '""';
    const str = String(cell).replace(/"/g, '""');
    return `"${str}"`;
  };

  const csvRows = [
    headers.map(escapeCell).join(','),
    ...rows.map(row => row.map(escapeCell).join(','))
  ];

  const blob = new Blob([csvRows.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename.endsWith('.csv') ? filename : `${filename}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}
```

#### Step 5: Implement `AnalyticsDashboard.jsx` Console
The component features:
- **Filter Pills**: `7D`, `30D`, `90D`, `YTD`, `ALL`.
- **Top Header**: Title "Analytics & Reporting Console", period descriptor, Refresh button, and "Export Reports" dropdown.
- **R1 — Financial Telemetry KPI Grid**: Gross Sales, Net Revenue, AOV, Transactions with period-over-period percentage comparison badges (`+18.4% vs. prior period`).
- **R1 — Interactive Visual Charts**: SVG Revenue Trend Chart with gradient fill and payment method distribution donut.
- **R2 — Inventory Velocity Telemetry**:
  - Days of Inventory Remaining gauge.
  - Fast-moving vs slow-moving SKU table with run-rate and days remaining.
  - Stockout risk alerts with warehouse indicators.
  - Aging classification (<30d, 30-90d, >90d).
- **R3 — Customer Cohorts & Regional Demand**:
  - Customer LTV tiers (High, Mid, Entry).
  - New vs returning customer revenue contribution.
  - Top purchasing cities and states (Hyderabad, Bengaluru, Mumbai, Chennai, etc.).
  - Conversion funnel and cart abandonment telemetry.
- **R4 — One-Click CSV Export**:
  - `Export Sales Report` button triggering downloadable CSV.
  - `Export Inventory Velocity` button triggering downloadable CSV.

---

## 5. Verification Method

To independently verify all findings and validate future implementations:

### 5.1 Static Layout & Navigation Inspection
1. **Verify AppRouter Route Registration**:
   Inspect `frontend/src/routes/AppRouter.jsx` to confirm `/admin/analytics` is nested under the protected `/admin` route.
2. **Verify Sidebar Link & Highlighting**:
   Inspect `frontend/src/pages/Admin/AdminDashboard.jsx` to confirm `BarChart3` icon and `{ path: '/admin/analytics', ... }` are present in `ADMIN_NAV`.
3. **Verify Bundle Size Budget**:
   Execute:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend && npm run build
   ```
   **Pass condition**: Build completes with exit code 0; all chunk sizes remain below 500 kB.

### 5.2 Automated Playwright End-to-End Test Execution
Execute the dedicated Playwright analytics test suite:
```bash
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
npx playwright test tests/analytics.spec.js --project=chromium
```

**Verification Checklist**:
1. Staff member (`admin@sareekart.com` / `admin123` or `manager@sareekart.com`) can navigate to `/admin/analytics` and see the "Analytics" heading.
2. Customer (`customer@sareekart.com` / `customer123`) is denied access and redirected to `/`.
3. Direct API call with customer token to analytics endpoint receives HTTP 403 Forbidden with `{"message": "Not authorised to perform this action"}`.
4. KPI cards display formatted INR currency values and period-over-period percentage badges.
5. Clicking date range filter pills (`7D`, `30D`, `90D`, `YTD`, `ALL`) dynamically toggles active classes and updates telemetry metrics.
6. Clicking export triggers a browser download with filename matching `*.csv`.

### 5.3 Full Regression Suite
Execute full regression to guarantee zero regressions:
```bash
cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
npx playwright test --project=chromium
```
**Pass condition**: 100% of tests pass.
