# SareeKart Frontend Architecture & Production Bundle Survey

**Agent**: `teamwork_preview_explorer_survey_2`  
**Date**: 2026-09-03  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_2/`  
**Project Path**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend`

---

## Executive Summary

1. **Production Build & Bundle Size Compliance**:
   - `npm run build` succeeds with **0 errors**.
   - The largest bundle chunk generated is `dist/assets/index-DIcL-ISg.js` at **438.72 kB** (gzip: 139.06 kB).
   - **Requirement Met**: All chunks are strictly **under 500 kB** (highest chunk is ~61 kB below the 500 kB limit).
   - Route-level code-splitting is operational via `React.lazy()` across 19 pages/dashboards.
2. **Linting Status**:
   - `npm run lint` fails with **221 errors and 1 warning** (222 problems total).
   - Bulk of errors are `no-unused-vars` (vestigial `import React` with React 19 JSX transform, unused Lucide icons, unused state setters), plus 1 `react-hooks/set-state-in-effect` error in `ManageSarees.jsx:83`.
3. **Local Health Checks & Backend Proxy**:
   - Frontend server at `http://localhost:5173` returns `HTTP 200 OK`.
   - Backend API at `http://localhost:8081/api/products` returns `HTTP 200 OK` on GET.
   - Vite reverse proxy (`/api` -> `http://127.0.0.1:8081`) functions properly: `http://localhost:5173/api/products` returns `HTTP 200 OK`.
   - **Critical Port Binding Defect**: Vite binds exclusively to IPv6 `localhost` (`::1`), refusing `127.0.0.1`.
4. **Test Suite Status**:
   - No unit test suite (`jest`/`vitest`) is configured in `package.json`.
   - Playwright E2E suite in `tests/` has 42 configured browser tests (14 per browser engine).
   - In Chromium test run: 4 passed (`cart.spec.js` and `products.spec.js`), 10 failed due to updated UI copy mismatches and IPv4 `127.0.0.1` connection refusal.
5. **Offline Boundary Compliance**:
   - `frontend/src/index.css` contains an external Google Fonts `@import` (`https://fonts.googleapis.com/...`).
   - Catalog components reference external CDN images (`kankatala.com`, `unsplash.com`).
   - Lingering Playwright template `frontend/e2e/example.spec.js` points to `https://playwright.dev/`.

---

## 1. Frontend Codebase Layout & Configuration

### Directory Hierarchy
```
/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/
├── dist/                     # Production build artifacts (62 files in assets/)
├── e2e/                      # Playwright default template (example.spec.js)
├── node_modules/             # Installed dependencies
├── public/                   # Static assets (manifest.json, favicon.svg)
├── src/
│   ├── api/                  # Axios instance and interceptors (axiosConfig.js)
│   ├── assets/               # Local SVGs
│   ├── components/           # UI components (Cart, Layout, Reviews, Modals)
│   ├── constants/            # App constants
│   ├── data/                 # Static catalog data & mock sets (products.js)
│   ├── pages/                # Route views (Home, Products, ProductDetails, Login, Admin/*)
│   ├── redux/                # Redux Toolkit store & slices (auth, cart, order, product)
│   ├── routes/               # AppRouter.jsx (Route definitions & lazy imports)
│   ├── services/             # API services (productService, categoryService, etc.)
│   ├── App.jsx               # DEAD CODE (1021 lines of unreferenced mock prototype)
│   ├── AdminInbox.jsx        # Unreferenced component imported only by App.jsx
│   ├── index.css             # Tailwind v4 import & Google font import
│   └── main.jsx              # Application bootstrap entry point
├── tests/                    # 10 Playwright test specs (3 are 0-byte empty files)
├── eslint.config.js          # ESLint flat config
├── index.html                # HTML entry point with window.global polyfill
├── package.json              # Package manifest
├── playwright.config.js      # Playwright runner configuration
└── vite.config.js            # Vite build and dev-server configuration
```

### Anomaly / Dead Code Analysis
- **Unused `App.jsx`**: `src/App.jsx` (74,616 bytes, 1,021 lines) contains an old self-contained mock implementation of SareeKart. The actual application entry point (`src/main.jsx`) imports and renders `<AppRouter />` from `src/routes/AppRouter.jsx`, bypassing `App.jsx` entirely. Vite successfully tree-shakes `App.jsx` out of the production bundle.
- **Nested Directory**: `frontend/frontend/src/App.jsx` exists as an empty placeholder from earlier repository restructuring and can be safely disregarded.

### Dependency Stack (`package.json`)
- **Core Framework**: React `^19.2.6`, React DOM `^19.2.6`
- **Routing**: `react-router-dom` `^7.17.0`
- **State Management**: `@reduxjs/toolkit` `^2.12.0`, `react-redux` `^9.3.0`
- **Styling**: `tailwindcss` `^4.3.1`, `@tailwindcss/vite` `^4.3.1`
- **UI & Motion**: `framer-motion` `^12.42.0`, `lucide-react` `^1.18.0`, `swiper` `^14.0.1`
- **Networking**: `axios` `^1.18.0`, `@stomp/stompjs` `^7.3.0`, `sockjs-client` `^1.6.1`
- **Build & Dev**: `vite` `^8.0.12`, `@vitejs/plugin-react` `^6.0.1`, `eslint` `^10.3.0`
- **Testing**: `@playwright/test` `^1.61.1`

---

## 2. Build Commands, Linting, & Test Suite

### 2.1 Build Execution (`npm run build`)
Command: `vite build`  
Execution result:
- **Status**: Succeeded (Exit code 0)
- **Time**: ~199 ms
- **Module Count**: 2,267 modules transformed
- **Output Directory**: `dist/`

### 2.2 Linting Execution (`npm run lint`)
Command: `eslint .`  
Execution result:
- **Status**: FAILED (Exit code 1)
- **Total Problems**: 222 (221 errors, 1 warning)
- **Categorization**:
  1. `no-unused-vars` (220 occurrences):
     - Unneeded `import React from 'react'` in React 19 JSX components (e.g. `ManageInventory.jsx:1`, `ManageFinance.jsx:1`, `SecurityDashboard.jsx:1`).
     - Unused Lucide icon imports across admin dashboards (e.g., `DollarSign`, `PieChart`, `Building`, `Filter`, `Check`).
     - Unused React state variables/setters (e.g. `setLedger`, `setInventory`, `setChecklist`, `setTelemetry`, `setSuites`, `setAuditLogs`).
     - Unused parameters in Redux extraReducers (`productSlice.js:189`, `productSlice.js:240`).
  2. `react-hooks/set-state-in-effect` (1 occurrence):
     - `src/pages/Admin/ManageSarees.jsx:83`: Calling `fetchProducts()` synchronously directly within `useEffect(() => { fetchProducts(); }, [page, searchActive])`.
  3. `react-hooks/exhaustive-deps` (1 occurrence):
     - `src/pages/Admin/ManageSarees.jsx:84`: Missing `fetchProducts` dependency in `useEffect`.

### 2.3 Test Suite Execution
- **Unit / Integration Tests**: No Jest or Vitest runner is installed or declared in `package.json`.
- **E2E Tests (`@playwright/test`)**:
  - `playwright.config.js` directs tests to `./tests`.
  - Empty (0 byte) spec files: `tests/category.spec.js`, `tests/checkout.spec.js`, `tests/orders.spec.js`.
  - Chromium test run results (14 tests):
    - **Passed (4)**:
      - `tests/products.spec.js`: should display product list
      - `tests/products.spec.js`: should be able to view product details
      - `tests/cart.spec.js`: should add product to cart
      - `tests/login.spec.js`: should navigate to register page
    - **Failed (10)**:
      - `tests/home.spec.js`: Target copy `SareeKart handloom, in full color` not found; current copy is `The new season edit` / `Sarees with a point of view.`.
      - `tests/login.spec.js`: Target heading `Come back to your edit.` not found; current heading is `Pick up where you left off.`.
      - `tests/search.spec.js`: Target heading `Find the saree that fits the plan.` not found; current heading is `Handloom, with a point of view.`.
      - `tests/admin.spec.js` (2 tests): Timed out waiting for button `Admin Portal`; actual button text is `Admin`.
      - `tests/register.spec.js` (5 tests): Connection refused attempting `http://127.0.0.1:5173/register?redirect=/orders` due to IPv6 host binding.

---

## 3. Bundle Size Analysis & Acceptance Criteria Compliance

### Acceptance Criteria Check:
> "Frontend production bundle (`npm run build`) builds with 0 errors and all chunks under 500 kB."

### Chunk Breakdown Table (Sorted by Size)

| Output Chunk File | Type | Uncompressed Size | Gzipped Size | Compliance Status (<500 kB) |
|---|---|---|---|---|
| `dist/assets/index-DIcL-ISg.js` | Main Entry / Vendor | **438.72 kB** | 139.06 kB | **PASS** (-61.28 kB margin) |
| `dist/assets/index-CrDXbq0V.css` | Global Stylesheet | **86.92 kB** | 15.72 kB | **PASS** |
| `dist/assets/axiosConfig-BZtHTr9i.js` | Shared API Module | **44.93 kB** | 17.18 kB | **PASS** |
| `dist/assets/CheckoutPage-BbTLkW58.js` | Route Chunk | **17.24 kB** | 5.11 kB | **PASS** |
| `dist/assets/ManageSarees-BbEpDC_u.js` | Admin Route Chunk | **16.52 kB** | 4.08 kB | **PASS** |
| `dist/assets/ProductDetailPage-Cbqvn9qf.js` | Route Chunk | **16.20 kB** | 4.90 kB | **PASS** |
| `dist/assets/HomePage-COKNnBdb.js` | Route Chunk | **16.14 kB** | 4.42 kB | **PASS** |
| `dist/assets/ManageOrders-DGPFBQsE.js` | Admin Route Chunk | **13.05 kB** | 3.16 kB | **PASS** |
| `dist/assets/ManageInventory-DAEfO3En.js` | Admin Route Chunk | **12.65 kB** | 3.30 kB | **PASS** |
| `dist/assets/AdminStats-CNvaBG11.js` | Admin Route Chunk | **11.84 kB** | 3.38 kB | **PASS** |
| `dist/assets/ProductsPage-ZnuylSVW.js` | Route Chunk | **10.92 kB** | 3.73 kB | **PASS** |
| `dist/assets/AdminDashboard-BZAmWd5G.js` | Admin Route Shell | **10.90 kB** | 3.65 kB | **PASS** |
| `dist/assets/ManageCoupons-EgkjG25t.js` | Admin Route Chunk | **10.26 kB** | 2.61 kB | **PASS** |
| `dist/assets/ManageCMS-BORRZ-Zj.js` | Admin Route Chunk | **10.11 kB** | 2.78 kB | **PASS** |
| `dist/assets/AiPricingDashboard-BQI9nuDE.js` | Admin Route Chunk | **10.06 kB** | 2.68 kB | **PASS** |
| `dist/assets/jsx-runtime-D-t8TrU0.js` | JSX Runtime Chunk | **9.98 kB** | 3.95 kB | **PASS** |
| `dist/assets/SecurityDashboard-DXjEs4Qf.js` | Admin Route Chunk | **9.95 kB** | 2.98 kB | **PASS** |
| `dist/assets/AiDemandDashboard-B8CzC-DU.js` | Admin Route Chunk | **9.32 kB** | 2.54 kB | **PASS** |
| `dist/assets/ManageFinance-DBc7lugV.js` | Admin Route Chunk | **9.21 kB** | 2.66 kB | **PASS** |
| `dist/assets/AiRecommendationDashboard-HFyqGiGc.js` | Admin Route Chunk | **8.86 kB** | 2.52 kB | **PASS** |
| `dist/assets/RegisterPage-ASZ3ZHEe.js` | Route Chunk | **8.49 kB** | 2.63 kB | **PASS** |
| `dist/assets/OperationsVault-BaZDCaAs.js` | Admin Route Chunk | **8.30 kB** | 2.44 kB | **PASS** |
| `dist/assets/AiStylistDashboard-BoJR8ytA.js` | Admin Route Chunk | **7.68 kB** | 2.29 kB | **PASS** |
| `dist/assets/DevOpsDashboard-_hxloyuH.js` | Admin Route Chunk | **7.56 kB** | 2.18 kB | **PASS** |
| `dist/assets/QADashboard-DBeTFiM3.js` | Admin Route Chunk | **7.55 kB** | 2.11 kB | **PASS** |
| `dist/assets/PerformanceDashboard-CbRM1nNh.js` | Admin Route Chunk | **7.38 kB** | 1.93 kB | **PASS** |
| `dist/assets/MyOrders-CwY-mF6Y.js` | Route Chunk | **7.28 kB** | 2.63 kB | **PASS** |
| `dist/assets/AiVisualSearchDashboard-mGStlWTT.js` | Admin Route Chunk | **7.03 kB** | 2.02 kB | **PASS** |
| `dist/assets/ManageUsers-B_Kztdad.js` | Admin Route Chunk | **6.83 kB** | 2.33 kB | **PASS** |
| `dist/assets/products-BwqdaI7h.js` | Shared Data Chunk | **6.76 kB** | 2.20 kB | **PASS** |
| `dist/assets/ProductCard-B25LxBFL.js` | Shared Component | **6.72 kB** | 2.77 kB | **PASS** |
| `dist/assets/LoginPage-DTp1W92H.js` | Route Chunk | **5.94 kB** | 2.24 kB | **PASS** |
| Other 30 icon/utility micro-chunks | Micro chunks | < 1.0 kB each | < 0.4 kB each | **PASS** |

### Compliance Assessment
- Requirement: **PASS**. All chunks are under 500 kB.
- Headroom: 61.28 kB headroom on the largest chunk (`index-DIcL-ISg.js`).

---

## 4. Code-Splitting & Vite Rollup Optimization

### Current Architecture
- `src/routes/AppRouter.jsx` employs `React.lazy()` for all route targets:
  - 7 public/customer routes (Home, Products, ProductDetail, Login, Register, Checkout, MyOrders).
  - 12 admin dashboards and management portals.
- `vite.config.js` currently defines **no rollupOptions or manualChunks**.
  - As a result, Rollup bundles all root-level libraries (`react`, `react-dom`, `react-router-dom`, `@reduxjs/toolkit`, `framer-motion`, `swiper`, `@stomp/stompjs`, `sockjs-client`, Lucide icons used in Shell) into `dist/assets/index-DIcL-ISg.js`.

### Proposed Manual Chunks Configuration (for Downstream Implementation)
To future-proof chunk limits and reduce the main bundle from 438 kB to ~120 kB, the following `manualChunks` strategy in `vite.config.js` is recommended:

```javascript
// Recommended addition to vite.config.js
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
    host: '0.0.0.0', // Resolves IPv4 127.0.0.1 refusal
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://127.0.0.1:8081',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  build: {
    chunkSizeWarningLimit: 500,
    rollupOptions: {
      output: {
        manualChunks: {
          'vendor-react': ['react', 'react-dom', 'react-router-dom'],
          'vendor-redux': ['@reduxjs/toolkit', 'react-redux'],
          'vendor-motion': ['framer-motion'],
          'vendor-ui': ['swiper', 'lucide-react'],
        },
      },
    },
  },
});
```

---

## 5. Frontend Health Check & Backend Proxy Configuration

### 5.1 Endpoint Verification Results

| Endpoint | Target Method | Status Code | Content-Type | Verification Evidence |
|---|---|---|---|---|
| `http://localhost:5173` | GET | `200 OK` | `text/html` | Verified via `curl -I` |
| `http://127.0.0.1:5173` | GET | `Connection refused` | N/A | Node/Vite IPv6-only default listener |
| `http://localhost:8081/api/products` | GET | `200 OK` | `application/json` | Returns JSON product array |
| `http://localhost:8081/api/products` | HEAD (`curl -I`) | `401 Unauthorized` | `application/json` | Spring Security requires auth for HEAD |
| `http://localhost:5173/api/products` | GET | `200 OK` | `application/json` | Vite proxy forwards to backend cleanly |

### 5.2 The IPv6 Binding Defect & Fix
- In `manage.sh` line 59: `nohup npm run dev > "$LOG_FRONTEND" 2>&1 &`
- Vite initializes with default host (`localhost`). On macOS Darwin, `localhost` resolves to `::1`.
- `lsof -i :5173` confirms: `node ... IPv6 ... TCP localhost:5173 (LISTEN)`.
- Requests directed to `http://127.0.0.1:5173` fail immediately with `ECONNREFUSED`.
- **Remediation**: Set `host: '0.0.0.0'` in `vite.config.js` or start Vite with `--host 0.0.0.0`.

---

## 6. Offline Boundary Compliance

### Audit of External Network Calls & Dependencies

| Type | Location | Details | Offline Impact | Remediation Recommendation |
|---|---|---|---|---|
| **External Font** | `src/index.css:1` | `@import url('https://fonts.googleapis.com/css2?family=Fraunces...&family=Inter...');` | Browser attempts external DNS/HTTP fetch | Bundle font files locally in `src/assets/fonts` or rely on system fallback stack |
| **Catalog Images** | `src/data/products.js`, `src/components/SignatureCollections.jsx`, etc. | Hardcoded URLs to `kankatala.com` and `images.unsplash.com` | If disconnected, images fail to load (handled by `SafeImage` fallback) | Store core sample assets in `public/images/` or rely on SVG placeholders |
| **Playwright Spec** | `frontend/e2e/example.spec.js:5,12` | `await page.goto('https://playwright.dev/');` | Test suite touches external domain if `e2e` is targeted | Delete or point to `http://localhost:5173/` |
| **Social Media Links** | `src/components/Footer.jsx` | Links to `https://instagram.com`, `https://wa.me/...` | Static anchors, no background network fetch on page load | Compliant as long as unclicked |

---

## 7. Actionable Recommendations for Remediation Agents

1. **Vite Host Binding Fix**:
   - In `frontend/vite.config.js`, add `host: '0.0.0.0'` to `server` block.
2. **ESLint Resolution**:
   - Run `eslint --fix .` in `frontend/` to clean auto-fixable unused imports.
   - Adjust `eslint.config.js` rule settings (e.g. configure `'no-unused-vars': ['warn', { argsIgnorePattern: '^_' }]`) or clean unused imports.
   - In `ManageSarees.jsx:83`, wrap `fetchProducts()` in `useCallback` or move call to an event/effect handler conforming to React hooks conventions.
3. **Playwright Spec Updates**:
   - Update `tests/home.spec.js` locators to reflect current headline "The new season edit" / "Sarees with a point of view.".
   - Update `tests/login.spec.js` heading assertion from "Come back to your edit." to "Pick up where you left off.".
   - Update `tests/search.spec.js` heading assertion from "Find the saree that fits the plan." to "Handloom, with a point of view.".
   - Update `tests/admin.spec.js` button locator from `/Admin Portal/i` to `getByRole('button', { name: 'Admin', exact: true })`.
   - In `tests/register.spec.js:13`, change `http://127.0.0.1:5173` to `http://localhost:5173`.
4. **Offline Hardening**:
   - Remove or replace `@import url('https://fonts.googleapis.com/...')` in `frontend/src/index.css`.
   - Remove `frontend/e2e/example.spec.js` or repoint to local URL.
