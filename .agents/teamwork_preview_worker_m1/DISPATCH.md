# DISPATCH: Milestone M1 - Backend Analytics Implementation

## Mission
Implement the full Backend Analytics Telemetry Engine & Access Control for SareeKart covering R1 (Sales & Financial Telemetry), R2 (Inventory Velocity & Stock Telemetry), R3 (Customer Cohorts & Geographic Analytics), and R5 (Access Control & Authorization Hardening) in `backend/backend/`.

## Authoritative Requirements & Inputs
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md` (specifically ## Follow-up — 2026-09-04T15:17:07Z).
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md`.
- Read the 3 Explorer handoff reports:
  1. `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_1/handoff.md` (Repositories & DTO specifications)
  2. `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2/handoff.md` (Service logic, mathematical algorithms, POI Excel & CSV export)
  3. `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_m1_3/handoff.md` (Controller contracts, Security 403 hardening, DataSeeder historical orders, unit test suites)

## Mandatory Integrity Warning
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

## File Ownership (Exclusively Owned Files)
You exclusively own and may create/modify:
- `backend/backend/src/main/java/com/sareekart/repository/OrderItemRepository.java`
- `backend/backend/src/main/java/com/sareekart/repository/OrderRepository.java`
- `backend/backend/src/main/java/com/sareekart/dto/response/analytics/**`
- `backend/backend/src/main/java/com/sareekart/service/AnalyticsService.java`
- `backend/backend/src/main/java/com/sareekart/service/impl/AnalyticsServiceImpl.java`
- `backend/backend/src/main/java/com/sareekart/controller/AnalyticsController.java`
- `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`
- `backend/backend/src/main/java/com/sareekart/exception/GlobalExceptionHandler.java`
- `backend/backend/src/main/java/com/sareekart/config/DataSeeder.java`
- `backend/backend/src/test/java/com/sareekart/service/AnalyticsServiceTest.java`
- `backend/backend/src/test/java/com/sareekart/controller/AnalyticsControllerTest.java`

## Implementation Tasks
1. Repositories & DTOs:
   - Create `OrderItemRepository.java` for SKU sales aggregations.
   - Enhance `OrderRepository.java` with JPQL queries and projections for date ranges, revenue sums, order counts, payment distributions, geographic grouping, timeline grouping, and customer spend.
   - Create all response DTO classes in `com.sareekart.dto.response.analytics`: `AnalyticsOverviewResponse`, `SalesTelemetryResponse`, `InventoryVelocityResponse`, `CustomerAnalyticsResponse`, and nested item DTOs.
2. Service Layer:
   - Create `AnalyticsService.java` and implement `AnalyticsServiceImpl.java`.
   - Implement date interval handling (`TODAY`, `7D`, `30D`, `90D`, `YTD`, `ALL`, `CUSTOM`) and comparative period calculations with zero-safe percentage delta formulas.
   - Implement financial calculations (Gross sales, Net revenue, 5% GST tax, shipping fee rules, AOV, payment splits, coupon ROI, daily timeline).
   - Implement inventory velocity calculations (daily run-rate, days of inventory remaining, fast/slow SKU rankings, 3-tier aging, stockout alerts with 0-100 priority scoring).
   - Implement customer cohort metrics (Platinum, Gold, Silver LTV tiers, new vs returning revenue contribution, repeat purchase rate, top states/cities, cart conversion funnel).
   - Implement Apache POI Excel (`.xlsx`) and RFC 4180 CSV export generation.
3. Controller & Security (R5):
   - Create `AnalyticsController.java` under `/api/admin/analytics/**` with `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`.
   - Update `SecurityConfig.java`: In `authenticationEntryPoint`, if request path starts with `/api/admin`, return HTTP 403 Forbidden with `{"success":false,"message":"Not authorised to perform this action"}`.
   - Update `GlobalExceptionHandler.java`: Add `@ExceptionHandler(AccessDeniedException.class)` returning HTTP 403 Forbidden with `ApiResponse.error("Not authorised to perform this action")`.
4. Historical Data Seeding:
   - Update `DataSeeder.java` to seed historical orders over the past 90 days when `orderRepository.count() == 0`, using native SQL update to preserve historical `created_at` dates past JPA auditing.
5. Verification:
   - Create `AnalyticsServiceTest.java` and `AnalyticsControllerTest.java`.
   - Run `./mvnw clean test` in `backend/backend/` and verify that ALL tests pass with 0 failures and 0 errors.
   - Restart the backend service (`./manage.sh restart backend` or check `./manage.sh status`) so the live runtime on port 8081 picks up the changes and seeds the historical orders.

## Output Requirements
Write `handoff.md` in your working directory `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_worker_m1/handoff.md` following the Handoff Protocol (Observation, Logic Chain, Caveats, Conclusion, Verification Method). Include `./mvnw test` results. Update `progress.md` with your liveness heartbeat. When done, notify caller via send_message.

## 2026-09-04T15:32:18Z
You are teamwork_preview_worker_m1.
Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_worker_m1
Implement all Milestone M1 backend tasks:
1. Repositories (OrderItemRepository.java, OrderRepository.java enhancements) and DTOs (AnalyticsOverviewResponse, SalesTelemetryResponse, InventoryVelocityResponse, CustomerAnalyticsResponse).
2. Service layer (AnalyticsService.java, AnalyticsServiceImpl.java with date ranges, period-over-period delta math, financial formulas, velocity run-rates, aging, LTV tiers, cohorts, conversion funnel, and Apache POI Excel/CSV generators).
3. Controller (AnalyticsController.java under /api/admin/analytics/**) and Security hardening (SecurityConfig.java, GlobalExceptionHandler.java) enforcing HTTP 403 Forbidden with exact message "Not authorised to perform this action" for unauthorized/unauthenticated requests.
4. Historical order seeding in DataSeeder.java.
5. Unit tests (AnalyticsServiceTest.java, AnalyticsControllerTest.java) and verify with `./mvnw clean test` in backend/backend/.
6. Restart backend via `./manage.sh restart` (or restart backend process) and verify health check `curl http://localhost:8081/api/products` returns 200 OK.
