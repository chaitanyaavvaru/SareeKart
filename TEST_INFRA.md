# E2E Test Infra: SareeKart Analytics & Reporting Suite

## Test Philosophy
- Opaque-box, requirement-driven. Derived strictly from `ORIGINAL_REQUEST.md` (Follow-up — 2026-09-04T15:17:07Z).
- Verification across 4 distinct test tiers for complete feature, boundary, combinatorial, and realistic workflow validation.
- Zero network exposure: Localhost only (`http://localhost:5173`, `http://localhost:8081`).

## Feature Inventory & Test Mapping
| # | Feature | Requirement Source | Tier 1 | Tier 2 | Tier 3 | Tier 4 |
|---|---------|-------------------|:------:|:------:|:------:|:------:|
| 1 | Sales & Financial Telemetry (R1) | ORIGINAL_REQUEST §R1 | ✓ (5) | ✓ (5) | ✓ | ✓ |
| 2 | Inventory Velocity & Stock Telemetry (R2) | ORIGINAL_REQUEST §R2 | ✓ (5) | ✓ (5) | ✓ | ✓ |
| 3 | Customer Cohorts & Geographic Analytics (R3) | ORIGINAL_REQUEST §R3 | ✓ (5) | ✓ (5) | ✓ | ✓ |
| 4 | Admin Analytics Console UI (R4) | ORIGINAL_REQUEST §R4 | ✓ (5) | ✓ (5) | ✓ | ✓ |
| 5 | Access Control & Authorization (R5) | ORIGINAL_REQUEST §R5 | ✓ (5) | ✓ (5) | ✓ | ✓ |

## Test Architecture
- **Backend Unit / Integration Tests**:
  - Location: `backend/backend/src/test/java/com/sareekart/`
  - Runner: `./mvnw test`
  - Target classes: `AnalyticsServiceTest.java`, `AnalyticsControllerTest.java`
- **Frontend Playwright E2E Tests**:
  - Location: `frontend/tests/analytics.spec.js`
  - Config: `frontend/playwright.config.js`
  - Runner command: `cd frontend && npx playwright test tests/analytics.spec.js --project=chromium`
  - Full regression runner: `cd frontend && npx playwright test --project=chromium`

## Test Tier Structure
- **Tier 1 — Feature Coverage (Core Acceptance)**:
  - Authorized staff (Admin, Manager, Owner) navigates to `/admin/analytics` and sees the console.
  - Customer navigation is redirected to `/` and API access returns HTTP 403 Forbidden with exact message: `"Not authorised to perform this action"`.
  - Unauthenticated access returns HTTP 403 Forbidden with `"Not authorised to perform this action"` on `/api/admin/analytics/**`.
  - High-level KPI summary cards render (Gross Sales, Net Revenue, Tax, Shipping, AOV, Transactions).
  - Date-range filter pills (`7D`, `30D`, `90D`, `YTD`, `All`) toggle and dynamically update metrics.
  - One-click CSV and Excel report exports trigger valid browser downloads.
- **Tier 2 — Boundary & Corner Cases**:
  - Custom date ranges with zero orders in interval (renders 0 values without error).
  - Division by zero safety: zero orders resulting in AOV = 0.00; zero daily run-rate resulting in safe days-remaining indicator.
  - Boundary ranges (e.g. single day `TODAY`, multi-year `ALL`, leap-year intervals).
  - Missing token / invalid token rejection with 403 Forbidden.
- **Tier 3 — Cross-Feature Combinations**:
  - Switching date range filters followed immediately by report export reflects selected window.
  - Role switching (Admin -> Manager -> Owner) confirms consistent access across all 3 privileged roles.
  - Coupon utilization reflecting in sales telemetry discount calculations.
- **Tier 4 — Real-World Workloads**:
  - Complete admin telemetry audit: inspecting financial charts, checking fast/slow inventory SKUs, reviewing customer LTV cohorts and regional demand map.
  - Live order generation: placing a customer order on storefront and verifying immediate reflection in backend analytical telemetry.

## Coverage Thresholds & Pass Criteria
- Backend tests: 100% pass rate (`./mvnw test`), 0 failures, 0 errors.
- Frontend E2E analytics tests: 100% pass rate (`tests/analytics.spec.js`), 0 failures, 0 errors.
- Frontend regression: 100% pass rate across all 15 spec files (`npx playwright test --project=chromium`).
- Production bundle: `npm run build` with 0 errors and all chunks under 500 kB.
