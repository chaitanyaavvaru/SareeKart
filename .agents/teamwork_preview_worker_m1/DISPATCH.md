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

## 2026-09-17T11:30:28Z
You are Worker M1: WhatsApp Backend Hardening & Implementation Specialist.
Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_worker_m1

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Context & References:
- Authoritative User Request: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md (Phase 13 Stage 4 starting at line 250)
- Project Scope Document: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_orchestrator_7/PROJECT.md
- Survey 1 Report (Webhook & Idempotency): /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_1/survey_report.md
- Survey 2 Report (Consent, Normalization & Templates): /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_2/survey_report.md
- Survey 3 Report (Failure Isolation & Escalation): /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_3/survey_report.md

Task:
Implement and harden the SareeKart backend at /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend across all Requirements R1 through R5:

1. Requirement R1: Webhook Security & Idempotency
   - In `backend/backend/src/main/resources/application.yaml`, bind `whatsapp.webhook.app-secret: ${WHATSAPP_APP_SECRET:sareekart-meta-secret-2026}`.
   - In `backend/backend/src/main/java/com/sareekart/security/WhatsAppWebhookSignatureValidator.java`:
     - Harden signature verification. If the signature header is missing, malformed, or doesn't match the HMAC-SHA256 of the payload, return `false`. Do not allow requests with missing signatures in standard runs.
   - In `backend/backend/src/main/java/com/sareekart/controller/WhatsAppWebhookController.java`:
     - GET `/api/webhook/whatsapp`: strictly validate `hub.mode=subscribe` and `hub.verify_token`, return `hub.challenge` with 200, else 403.
     - POST `/api/webhook/whatsapp`: validate `X-Hub-Signature-256` signature using the validator. If missing or invalid, return 401 UNAUTHORIZED (or 403). Only valid signed non-empty payloads proceed to processing.
   - In `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`: ensure `/api/webhook/**` is permitted without authentication filter blockage.
   - Confirm `WhatsAppIdempotencyService` properly deduplicates incoming `wam_id` (in-memory sliding window + database check).

2. Requirement R2: Regulatory Compliance (Opt-In, STOP, START Protocol)
   - In `backend/backend/src/main/java/com/sareekart/entity/WhatsAppContact.java`:
     - Add fields `private Boolean optedIn = true;` and `private LocalDateTime optInUpdatedAt;` with getters/setters.
   - In `backend/backend/src/main/resources/db/migration/`:
     - Add migration `V32__whatsapp_production_readiness.sql` adding `opted_in` (BOOLEAN DEFAULT TRUE), `opt_in_updated_at` (TIMESTAMP) to `whatsapp_contacts`.
   - In `backend/backend/src/main/java/com/sareekart/service/impl/WhatsAppWebhookServiceImpl.java`:
     - When receiving incoming customer messages, evaluate keywords:
       - If message text matches `STOP`, `UNSUBSCRIBE`, `CANCEL` (case-insensitive, trimmed): update contact `optedIn = false`, update `optInUpdatedAt`, save contact, and send polite opt-out confirmation: "You have been unsubscribed from WhatsApp notifications. Text START to resume." Suppress further promotional/bot handling.
       - If message text matches `START`, `UNSTOP` (case-insensitive, trimmed): update contact `optedIn = true`, update `optInUpdatedAt`, save contact, and send polite welcome back confirmation: "Welcome back! You are now subscribed to SareeKart updates on WhatsApp."
   - In `backend/backend/src/main/java/com/sareekart/service/impl/WhatsAppNotificationServiceImpl.java` (and `OrderNotificationServiceImpl.java` if applicable):
     - Before sending non-critical/promotional WhatsApp notifications, verify recipient's `optedIn` status. If `optedIn == false`, suppress the notification send.

3. Requirement R3: Message Templates & Phone Number Normalization
   - In `backend/backend/src/main/java/com/sareekart/service/impl/WhatsAppIdentityServiceImpl.java`:
     - Standardize Indian phone normalization to handle `+91`, `91`, leading `0`, and 10-digit formats into consistent international format (e.g. `+919876543210` or `919876543210`). Ensure compatibility with Meta Graph API and database queries.
   - In `WhatsAppApiClient.java` and `WhatsAppMessageRequest.java` / templates:
     - Define and support pre-approved Meta HSM message templates (Order Placed, Shipped, Delivered, Return Pickup) with dynamic parameter substitution.
     - Sensitive data masking: Ensure outbound messages and URLs never transmit JWT tokens, passwords, raw customer credit cards, or internal database primary keys (use orderNumber / trackingNumber instead).

4. Requirement R4: Failure Domain Isolation, Rate Limiting & Admin Escalation
   - Failure Isolation: In `OrderServiceImpl.java` and `OrderNotificationServiceImpl.java`, ensure outbound WhatsApp calls are wrapped with robust error handling (or executed asynchronously) so that Meta 429, 5xx, or network timeouts never abort, roll back, or disrupt order placement or checkout.
   - Rate limiting & backoff: In `WhatsAppApiClient.java` or `WhatsAppRateLimiter`, implement per-recipient throttling and backoff on 429 responses.
   - Human-in-the-loop escalation:
     - Ensure `ConversationStatus` enum includes `HUMAN_ESCALATION`.
     - In customer message processing (e.g. in `WhatsAppAiCommerceServiceImpl.java` / webhook service), detect requests for a human agent (keywords: "talk to stylist", "human", "agent", "support", or button `btn_human` / `💬 Talk to Stylist`) or frustration sentiment, transition conversation status to `HUMAN_ESCALATION`, and send WebSocket alert to `/topic/admin/inbox`.

5. Requirement R5: Isolation of Bridal Trousseau WhatsApp Integration
   - Verify `TrousseauWhatsAppServiceImpl` remains isolated with dedicated share tokens without cross-contaminating standard commerce messaging threads.

Execution & Verification:
- Implement all changes cleanly in the codebase.
- Compile and run existing affected unit/integration tests (`./mvnw test -Dtest=WhatsApp*Test,Trousseau*Test,Return*Test`). Ensure no regressions.
- Update any existing tests that need adjustments for the new fields or stricter signature validation.
- Output a detailed handoff report in your working directory at `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_worker_m1/handoff.md` including:
  - Exact files modified/created
  - Logic implemented
  - Maven test execution commands and results
- Send a message back to the orchestrator when completed.

