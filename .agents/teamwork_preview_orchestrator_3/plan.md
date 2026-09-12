# Plan — SareeKart Analytics & Reporting Suite

## Objective
Build a comprehensive Analytics & Reporting Suite for SareeKart covering sales performance, financial telemetry, customer retention cohorts, inventory velocity, and interactive visual reporting per ORIGINAL_REQUEST.md.

## Phases
1. **Phase 0: Survey & Scope Discovery**
   - Survey 1: Backend architecture, entities, repositories, services, security/auth filters, and existing admin endpoints.
   - Survey 2: Frontend routes, admin navigation, components, charting libraries, and state management.
   - Survey 3: Testing infrastructure, existing test cases, Playwright configuration, and test data seeding.
2. **Phase 1: Project Plan & Test Infrastructure Formulation**
   - Update `PROJECT.md` with Feature Inventory, Milestones, and Interface Contracts.
   - Update `TEST_INFRA.md` with E2E opaque-box test strategy across Tiers 1-4.
3. **Phase 2: Milestone Execution (Implementation & E2E Testing Tracks)**
   - M1: Backend Analytics Telemetry Engine & Access Control (R1, R2, R3, R5).
   - M2: Interactive Admin Analytics Console & Export (R4).
   - M3: E2E Test Suite Development (`tests/analytics.spec.js` + full regression verification).
4. **Phase 3: Final Verification & Audit**
   - Execute `./mvnw test` with 0 failures/errors.
   - Execute `npx playwright test tests/analytics.spec.js` with 0 errors.
   - Execute full regression `npx playwright test` with 100% pass rate.
   - Forensic audit verification.
5. **Phase 4: Synthesis & Reporting to Sentinel**
