# E2E Test Infra: SareeKart Operations & Customer Engagement Suite

## Test Philosophy
- Opaque-box, requirement-driven. Derived strictly from `ORIGINAL_REQUEST.md § Follow-up — 2026-09-06T12:09:27Z`.
- Methodology: Category-Partition + BVA + Pairwise + Workload Testing.

## Test Architecture
- **E2E Spec File**: `frontend/tests/operations-engagement.spec.js`
- **Playwright Config**: `frontend/playwright.config.js`
- **Target App URL**: `http://localhost:5173`
- **Backend API**: `http://localhost:8081`

## Seed Test Accounts
| Role | Email | Password | Persona |
|---|---|---|---|
| `ADMIN` | `admin@sareekart.com` | `admin123` | SareeKart Admin |
| `OWNER` | `owner@sareekart.com` | `owner123` | Super Owner |
| `MANAGER` | `manager@sareekart.com` | `manager123` | Store Manager |
| `CUSTOMER` | `customer@sareekart.com` | `customer123` | Chaitanya Customer (has prior order for Product #1) |

## Core Acceptance Test Scenarios (Tier 1)
1. **Navbar Notification Center & Counter**: Customer logs in; bell widget displays unread badge; popover list shows business event notifications; mark as read decrements count.
2. **Product Review Submission & Verified Buyer Badge**: Customer logs in; visits `/products/1`; submits 5-star review; review renders with distinct "Verified Buyer" badge; rating breakdown bars update.
3. **Admin Review Moderation Console**: Admin logs in; navigates to `/admin/reviews`; reviews pending submissions; approves review; verifies in Approved tab.
4. **Maker-Checker Multi-Warehouse Stock Transfer**: Store Manager submits transfer request (WH-01 to WH-02, qty 5); UI shows pending owner approval; Owner approves in Approval Center; stock balances update across hubs.
5. **Carrier Tracking & Milestones**: Customer views tracking modal on My Orders (Blue Dart carrier, AWB, milestone stepper); Admin updates tracking on Admin Orders; changes reflect in real-time.
6. **RBAC & 403 Forbidden Enforcement**: Customer navigating to `/admin/reviews` is redirected; direct API call returns 403 Forbidden with exact message `{"success":false,"message":"Not authorised to perform this action"}`.

## Full Regression & Build Acceptance
- Command 1: `npx playwright test tests/operations-engagement.spec.js --project=chromium` (100% pass)
- Command 2: `npx playwright test --project=chromium` (100% pass across all 24+ test specs)
- Command 3: `./mvnw test` in `backend/backend/` (100% pass, zero errors)
- Command 4: `npm run build` in `frontend/` (0 errors, all chunks < 500 kB)
