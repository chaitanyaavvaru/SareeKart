# DISPATCH: Milestone M1 - Security Hardening, Controller Contracts, Seeding & Tests

## Mission
Design controller endpoints, security hardening (R5), test data seeding in `DataSeeder.java`, and unit/integration test specifications for Milestone M1 (`AnalyticsControllerTest` and `AnalyticsServiceTest`).

## Authority & Scope
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md` (specifically ## Follow-up — 2026-09-04T15:17:07Z).
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md`.
- Working Directory: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3`
- Target Codebase: `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`

## Detailed Investigation
1. Controller Architecture (`AnalyticsController.java`):
   - Map under `@RequestMapping("/api/admin/analytics")`.
   - Protect with `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`.
   - Endpoints:
     - `GET /api/admin/analytics/overview`
     - `GET /api/admin/analytics/sales`
     - `GET /api/admin/analytics/inventory`
     - `GET /api/admin/analytics/customers`
     - `GET /api/admin/analytics/export/sales`
     - `GET /api/admin/analytics/export/inventory`
2. Access Control & Authorization (R5):
   - In `SecurityConfig.java`:
     - Ensure `/api/admin/**` requires `OWNER`, `MANAGER`, or `ADMIN`.
     - `accessDeniedHandler` returns HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.
     - `authenticationEntryPoint`: check if `request.getRequestURI().startsWith("/api/admin")` -> return HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}` instead of 401.
   - In `GlobalExceptionHandler.java`:
     - Add `@ExceptionHandler(AccessDeniedException.class)` returning HTTP 403 with `ApiResponse.error("Not authorised to perform this action")`.
3. Historical Data Seeding (`DataSeeder.java`):
   - When `orderRepository.count() == 0`, seed a realistic distribution of completed orders over the last 90 days across various products, categories, payment methods (RAZORPAY, COD), coupons, and shipping addresses (Bengaluru, Mumbai, Delhi, Hyderabad, Chennai).
   - This ensures live dashboard and Playwright tests immediately display populated metrics.
4. Unit and Integration Test Specifications:
   - `AnalyticsServiceTest.java`: Mockito tests validating date calculations, aggregations, and edge cases.
   - `AnalyticsControllerTest.java`: MockMvc standalone tests validating endpoint returns and access control.

## Deliverable
Write `handoff.md` with exact controller method annotations, security filter edits, seeder code snippet, and complete test cases.

## 2026-09-04T15:26:21Z
You are teamwork_preview_spec_miner_m1_3.
Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3
You MUST read /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md before starting work (specifically ## Follow-up — 2026-09-04T15:17:07Z).
Also read /Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md and your dispatch file at:
/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3/DISPATCH.md

Design controller endpoints, security hardening (R5), test data seeding in DataSeeder.java, and unit/integration test specifications for Milestone M1:
1. AnalyticsController under /api/admin/analytics/** with @PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')").
2. SecurityConfig and GlobalExceptionHandler updates to return HTTP 403 Forbidden with {"success":false,"message":"Not authorised to perform this action"} for unauthorized/unauthenticated requests to /api/admin/**.
3. DataSeeder updates: seed 30-90 days of historical orders when orderRepository.count() == 0 so live dashboard and Playwright tests immediately display populated metrics.
4. Test specifications: AnalyticsServiceTest and AnalyticsControllerTest ensuring ./mvnw test passes with zero errors.

Write your handoff report to /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3/handoff.md. Regularly update progress.md with your liveness heartbeat. When done, notify caller via send_message.

