# Orchestrator Plan: SareeKart v3.0 Module 1 (Self-Service Customer Returns & Exchanges)

## Objective
Implement an end-to-end self-service customer returns and exchanges system for SareeKart, complete with:
- 7-day post-delivery eligibility validation.
- Defect condition photo upload (up to 3 photos, stored locally and served publicly).
- Return vs exchange taxonomy, customer refund preferences, and comments.
- JPA domain model, repository queries, and service state transitions (`PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`).
- Reverse-pickup courier assignment (e.g. Blue Dart, Delhivery) and AWB tracking numbers.
- Role-based access control (Customers for their own requests, `OWNER`, `MANAGER`, `ADMIN` for moderation and reverse logistics).
- Storefront order card return status pills and telemetry drawer on `MyOrders.jsx`.
- Admin moderation console at `/admin/returns` (`ManageReturns.jsx`) with metrics, claims table, photo drawer, and 1-click action controls.
- Comprehensive backend unit/service tests (`ReturnServiceImplTest.java`), full backend regression (`./mvnw test`), frontend production build verification (`npm run build` < 500 kB), and storage health monitoring.

## Execution Phases

### Phase 0: Survey & Codebase Mapping (Parallel Explorers)
- Dispatch 3 parallel Explorers to inspect:
  - Explorer 1 (Backend Architecture): Inspect Order, User, SecurityConfig, existing order status enums, upload handlers, and API response conventions.
  - Explorer 2 (Frontend Architecture): Inspect `MyOrders.jsx`, `AdminDashboard.jsx`, routing (`AppRouter.jsx`), upload patterns, modal patterns, and Tailwind styling.
  - Explorer 3 (Database & Test Infrastructure): Inspect Flyway migrations, schema conventions, existing test suites, Maven setup, and test runner requirements.
- Merge Explorer findings into updated `PROJECT.md` at project root with complete Feature Inventory and Interface Contracts.

### Phase 1: Milestone Decomposition & Project Setup
- Finalize `PROJECT.md` with:
  - Feature Inventory mapped to Milestones.
  - Interface Contracts (REST endpoints, DTOs, request/response structures).
  - Code Layout and write ownership boundaries.

### Phase 2: Milestone 1 — Backend Domain Model, Services, REST API & Unit Tests
- Iteration Loop:
  - Explorer: Detail exact classes to create/modify, JPA entity annotations, security rules, file upload path handling.
  - Worker: Implement `ReturnRequest` entity, `ReturnRequestRepository`, `ReturnService`, `ReturnServiceImpl`, `ReturnController`, `AdminReturnController`, DTOs, security config adjustments, and `ReturnServiceImplTest`.
  - Reviewer (x2): Review code quality, RBAC, boundary condition handling, exception handling, and verify unit test passes.
  - Challenger (x2): Stress test edge cases (eligibility cutoff, duplicate returns, cross-customer access denial, state transition invalid jumps).
  - Forensic Auditor: Check for authenticity, absence of hardcoded mocks, genuine DB persistence and logic.
  - Gate: Strict AND pass criteria.

### Phase 3: Milestone 2 — Frontend Customer Return Modal, Telemetry & Admin Console
- Iteration Loop:
  - Explorer: Detail UI design, modal steps/layout, photo upload preview & API call, order card pills in `MyOrders.jsx`, and `ManageReturns.jsx` admin console.
  - Worker: Implement `ReturnRequestModal.jsx`, update `MyOrders.jsx`, create `ManageReturns.jsx`, update `AppRouter.jsx` and `AdminDashboard.jsx`. Verify `npm run build` < 500 kB.
  - Reviewer (x2): Review UI/UX, responsive layout, error handling, visual polish, bundle size.
  - Challenger (x2): Test edge cases in UI states (e.g. non-delivered orders, expired returns, long notes, image previews).
  - Forensic Auditor: Verify real components, genuine API integration, no facade bypasses.
  - Gate: Strict AND pass criteria.

### Phase 4: Milestone 3 — Full Regression Verification & Integration Testing
- Iteration Loop:
  - Worker / Test Writer: Run and verify all backend tests (`./mvnw test`), frontend build (`npm run build`), verify disk health (`check_disk_health.sh`).
  - Reviewer (x2): Review entire test output and verify adherence to acceptance criteria.
  - Challenger (x2): Adversarial verification across modules.
  - Forensic Auditor: Full codebase integrity audit for Module 1.
  - Gate: Strict AND pass criteria.

### Phase 5: Handoff & Notification
- Produce comprehensive final handoff report.
- Notify Sentinel via `send_message` that the project is ready for victory audit.
