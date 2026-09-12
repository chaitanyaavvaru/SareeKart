# Execution Plan — Operations & Customer Engagement Suite

## Objective
Implement R1 through R5 for SareeKart, ensuring:
- Backend: Centralized notifications, multi-warehouse stock transfers with Maker-Checker protocol, carrier logistics (AWB generation, status milestones), verified reviews with moderation, artisan profiles linked to catalog, and RBAC with standard 403 response.
- Frontend: Notification center in navbar with unread count, carrier tracking on My Orders and Admin Orders, stock transfer Maker-Checker console, review submission & moderation console, artisan showcase.
- Testing: 100% pass on `./mvnw test`, 100% pass on `frontend/tests/operations-engagement.spec.js` and full regression `npx playwright test --project=chromium`, and production build `npm run build` with chunks < 500 kB.

## Phases
1. **Phase 0: Comprehensive Survey**
   - Dispatch 3 parallel explorers to inspect:
     1. Backend: Entities (Order, Product, User, Warehouse), existing notifications, tracking, flyway migrations, security configuration.
     2. Frontend: Navbar, order views, admin panels, catalog detail pages, existing routes and state management.
     3. Spec Miner: Playwright setup, existing test patterns, mock vs real backend expectations, seed accounts.
2. **Phase 1: Architecture & Feature Inventory (`PROJECT.md` & `TEST_INFRA.md`)**
   - Synthesize survey findings.
   - Define exact API contracts, database schema migrations, and UI state models.
3. **Phase 2: Milestone Execution (Iteration Loop)**
   - M1: Event-Driven Notifications & Dispatch Telemetry (R1, R5)
   - M2: Multi-Warehouse Stock Transfers & Carrier Logistics (R2, R5)
   - M3: Verified Customer Reviews & Moderation Console (R3, R5)
   - M4: Artisan Heritage Storytelling Showcase (R4)
4. **Phase 3: E2E Playwright Suite (`operations-engagement.spec.js`) & Full Regression**
5. **Phase 4: Production Build Verification & Adversarial Audit**
6. **Phase 5: Victory Audit & Reporting to Sentinel**
