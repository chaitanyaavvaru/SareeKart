# Original User Request

## Initial Request — 2026-09-03T10:22:10Z

Execute comprehensive local verification, automated test suite execution, and production readiness checklist validation for SareeKart, automatically remediating any failing checks strictly within the local offline environment without publishing to the internet.

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: benchmark

## Requirements

### R1. Comprehensive Test Execution & Local Auto-Remediation
Execute all backend JUnit/Spring Boot tests (`./mvnw test`) and frontend Vite/React build and lint suites (`npm run build`). If any test fails or any checklist item is non-compliant, diagnose and fix the issue locally while preserving all existing business workflows and design tokens.

### R2. Complete Pre-Launch & Operations Checklist Audit
Validate all 16 enterprise modules locally—including Security (OWASP ASVS), Core Web Vitals performance budget, Database Schema integrity, API response codes, and Admin Operations telemetry—recording verifiable pass status across every checklist item.

### R3. Strict Local Isolation & Zero Internet Exposure
Keep all build, test, and verification processes strictly bound to `localhost` (`http://localhost:5173`, `http://localhost:8081`, `localhost:3306`). Do not invoke any external publishing, cloud deployment, public CDN sync, or outbound data transmission.

## Acceptance Criteria

### Automated Test Suites & Build
- [ ] Backend test suite (`./mvnw test`) passes with 0 failures and 0 errors.
- [ ] Frontend production bundle (`npm run build`) builds with 0 errors and all chunks under 500 kB.
- [ ] Local server health checks (`http://localhost:8081/api/products` and `http://localhost:5173`) return HTTP 200 OK.

### Checklist Compliance & Remediation
- [ ] All 16 operational checklists (Security, Quality Gate, SRE Performance, Disaster Recovery, Inventory, Finance, and AI Modules) pass with 100% verified status.
- [ ] Any local compilation, test, or lint failure encountered during the run is completely resolved and verified.

### Offline Security & Boundaries
- [ ] Zero outbound deployment, publishing, or remote hosting commands executed.
- [ ] All credentials, database instances, and API interactions remain restricted to local environment.

## Follow-up — 2026-09-04T15:17:07Z

Build a comprehensive Analytics & Reporting Suite for SareeKart covering sales performance, financial telemetry, customer retention cohorts, inventory velocity, and interactive visual reporting.

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: development

## Requirements

### R1. Sales & Financial Telemetry Engine
The backend must provide aggregated analytical telemetry over configurable time intervals (Today, Last 7 Days, Last 30 Days, Last 90 Days, Year-to-Date, and Custom Date Range):
- Gross sales, net revenue, tax, and shipping amounts.
- Average Order Value (AOV) and total completed transactions.
- Payment method distribution (UPI, NetBanking, Cards, COD).
- Coupon/promo discount utilization and campaign ROI.
- Revenue timeline breakdown for trend graphing.

### R2. Inventory Velocity & Stock Telemetry
Provide inventory health and supply chain movement analytics:
- Fast-moving vs slow-moving SKU rankings based on actual unit sales.
- Stock turnover velocity (Days of Inventory Remaining / Run-rate).
- Inventory aging categorization (<30 days, 30–90 days, >90 days).
- Stockout alerts and replenishment priority scoring across warehouses.

### R3. Customer Cohorts & Geographic Analytics
Analyze customer behavior, acquisition, and regional demand:
- Customer Lifetime Value (LTV) distribution.
- New vs. returning customer revenue contribution and repeat purchase rate.
- Regional demand breakdown and top buying cities/states.
- Cart abandonment telemetry and conversion funnel ratios.

### R4. Interactive Admin Analytics Dashboard
A dedicated, responsive analytics console within the SareeKart Admin workspace (`/admin/analytics`):
- High-level KPI summary cards with period-over-period percentage comparisons.
- Interactive visual charts (revenue trends, category revenue breakdown, customer acquisition).
- Fast date-range filter pills (7D, 30D, 90D, YTD, All) with instant dynamic data refresh.
- One-click export to CSV / Excel for sales and inventory velocity reports.
- Seamless integration into the admin sidebar navigation.

### R5. Access Control & Authorization
- Analytics endpoints must be restricted to authorized roles (`OWNER`, `MANAGER`, `ADMIN`).
- Customers and unauthenticated requests must receive HTTP 403 Forbidden with message `"Not authorised to perform this action"`.

## Verification Plan & Acceptance Criteria

### Automated Backend Tests
- Spring Boot service and controller unit tests verifying analytical query aggregation, date filtering logic, and access control.
- Command: `./mvnw test` passing with zero errors.

### Automated End-to-End Playwright Tests
- Playwright E2E test suite in `frontend/tests/analytics.spec.js`:
  - Verify navigation to `/admin/analytics` by authorized staff.
  - Verify customer access denial (redirection and 403 API response).
  - Verify KPI cards display aggregated metrics.
  - Verify date-range filter switching updates telemetry.
  - Verify report export triggers downloadable file.
- Command: `npx playwright test tests/analytics.spec.js --project=chromium` passing with zero errors.
- Full regression command: `npx playwright test --project=chromium` with 100% pass rate.



## Follow-up — 2026-09-06T12:09:27Z

Build a comprehensive Operations & Customer Engagement Suite for SareeKart covering real-time event notifications, multi-warehouse stock transfers with Maker-Checker logistics, customer reviews with verified buyer badges and moderation, and artisan storytelling profiles.

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: development

## Requirements

### R1. Event-Driven Notifications & Dispatch Telemetry
- Implement a centralized notification engine triggering on business events: order placed, shipment dispatched (with tracking number), order delivered, warehouse low-stock alerts, and pending Maker-Checker approval requests.
- Customer and staff navbar notification center widget with an unread badge counter, popover notification list, mark-as-read actions, and link routing to relevant orders or admin queues.
- Simulated notification event audit log in the backend recording in-app, simulated SMS, and email delivery payloads without requiring external telecom/cloud gateways.

### R2. Multi-Warehouse Stock Transfers & Carrier Logistics
- Inter-warehouse stock transfers between regional hubs (`WH-01 Bengaluru Central`, `WH-02 Mumbai West`, `WH-03 Delhi North`).
- Enforce the Maker-Checker protocol: Managers submit transfer requests with target warehouse, SKU, quantity, and reason; Owners review, approve, or reject in the Approval Center; Owners also possess direct instant transfer execution capabilities.
- Order fulfillment logistics: Courier partner assignment (Blue Dart, Delhivery, DTDC, India Post), automated tracking number (AWB) generation, and timeline milestone progression (`Ordered`, `Packed`, `Shipped`, `Out for Delivery`, `Delivered`). Note: Basic tracking fields already exist on Order entity and public /track-order page; ensure integration with warehouse dispatches.

### R3. Verified Customer Reviews & Moderation Console
- Product rating (1–5 stars) and review submission system on product detail pages.
- Highlight purchases with a distinct "Verified Buyer" badge for customers who purchased the item, while allowing general registered customers to contribute feedback.
- Admin review moderation console (`/admin/reviews`) with filterable status tabs (`Pending`, `Approved`, `Rejected`, `Featured`), enabling staff to moderate, publish, or feature customer testimonials.
- Review analytics on product detail pages: aggregate average rating, 5-star distribution breakdown bars, and paginated/filtered customer review lists.

### R4. Artisan Heritage Storytelling Showcase
- Rich artisan storytelling profiles on `/artisans` showcasing artisan biographies, generation heritage, traditional loom techniques (Kanchipuram, Banarasi, Patola, Paithani, Chanderi), and weaver cooperative attribution.
- Direct linking between artisan profiles and the handcrafted saree catalog items they produced.

### R5. Access Control & Authorization (RBAC)
- Public visitors can view published/approved reviews and artisan heritage stories.
- Registered customers can view their notifications, track order shipment milestones, and submit reviews.
- Only authorized staff (`OWNER`, `MANAGER`, `ADMIN`) can manage warehouse stock transfers, fulfill carrier tracking, and moderate customer reviews.
- Unauthorized access attempts must receive HTTP 403 Forbidden with message `{"success":false,"message":"Not authorised to perform this action"}`.

## Acceptance Criteria

### Automated Backend Tests
- [ ] Spring Boot service and controller tests verifying notification triggers, stock transfer balance calculations across warehouses, and review moderation workflows.
- [ ] Command: `./mvnw test` passing with 100% pass rate and zero errors.

### Automated End-to-End Playwright Tests
- [ ] Playwright E2E test suite in `frontend/tests/operations-engagement.spec.js`:
  - Customer receives order notification in navbar bell widget with active counter.
  - Customer submits a review on a product page; verified buyer badge is displayed.
  - Admin/Owner accesses `/admin/reviews` and moderates pending customer reviews.
  - Store Manager submits inter-warehouse transfer request; Owner approves in Approval Center; stock balances update across hubs.
  - Order shipment milestones and carrier AWB tracking are visible on My Orders and Admin Orders.
- [ ] Command: `npx playwright test tests/operations-engagement.spec.js --project=chromium` passing with zero errors.
- [ ] Full regression command: `npx playwright test --project=chromium` with 100% pass rate across all test suites.
- [ ] Production frontend build: `npm run build` passing with zero errors and all bundle chunks strictly under 500 kB.

## Follow-up — 2026-09-11T10:04:03Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: Full multi-agent team

Build a self-service customer returns and exchanges system (SareeKart v3.0 Module 1) with 7-day post-delivery eligibility, condition photo upload, reverse-pickup courier tracking, and an admin moderation console for staff (`OWNER`, `MANAGER`, `ADMIN`).

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: development

---

## Requirements

### R1. Self-Service Doorstep Returns & Exchanges Modal
- Add a "Return / Exchange" action button on delivered order cards in `MyOrders.jsx` (`/orders`).
- Eligibility Gate: Order must have status `DELIVERED`, and delivery timestamp must be within 7 calendar days. Orders outside this window or in any other status (`PENDING`, `SHIPPED`, `CANCELLED`) must display a disabled button with clear explanatory tooltip.
- Multi-step or comprehensive modal (`ReturnRequestModal.jsx`):
  - Return Type: "Return for Refund" vs. "Exchange Saree".
  - Reason Taxonomy: `COLOR_MISMATCH`, `ZARI_DEFECT`, `FABRIC_FEEL`, `INCORRECT_ITEM`, `SIZE_MISMATCH`, `OTHER`.
  - Condition Photo Uploader: Drag-and-drop support for up to 3 defect photos (JPG, PNG, WebP) with client-side preview, validation, and upload.
  - Refund Preference: `ORIGINAL_PAYMENT` (Original payment source), `STORE_CREDIT` (Wallet credit), or `EXCHANGE_DRAPE` (Replacement item).
  - Customer comments field for detailed description of the issue.

### R2. Backend Persistence & Domain Model
- JPA entity `ReturnRequest` and table `return_requests`:
  - `id`, `order_id` (foreign key to `orders`), `user_id` (foreign key to `users`), `type` (`RETURN`/`EXCHANGE`), `reason`, `comments`, `status` (`PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`), `images` (list of photo URLs), `refund_amount`, `refund_mode`, `exchange_sku`, `reverse_courier`, `reverse_tracking_number`, `admin_notes`, `created_at`, `updated_at`.
- Spring Data repository `ReturnRequestRepository` with queries:
  - `findByUserIdOrderByCreatedAtDesc(Long userId)`
  - `findByOrderId(Long orderId)`
  - `findByStatusOrderByCreatedAtDesc(String status)`
  - `findAllByOrderByCreatedAtDesc()`
- Service layer `ReturnService` & `ReturnServiceImpl`:
  - Enforce order ownership (must belong to authenticated user).
  - Enforce `DELIVERED` status and `<= 7 days` cutoff.
  - Reject duplicate return requests for the same order.
  - Provide state transitions: `PENDING` -> `APPROVED` -> `PICKUP_SCHEDULED` (with reverse courier & AWB) -> `COMPLETED`, or `REJECTED` (with mandatory reason).

### R3. REST Endpoints & Role-Based Access Control
- Customer Endpoints:
  - `POST /api/returns`: Submit new return/exchange request.
  - `GET /api/returns/my-requests`: Fetch user's return history.
  - `GET /api/returns/order/{orderId}`: Fetch return status for a specific order.
  - `POST /api/returns/upload-photo`: Authenticated photo upload storing files in `uploads/return-photos/` and serving publicly at `/uploads/**`.
- Admin / Staff Endpoints (`OWNER`, `MANAGER`, `ADMIN`):
  - `GET /api/admin/returns`: Filterable list of all return claims with status tabs (`ALL`, `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`).
  - `PUT /api/admin/returns/{id}/status`: Approve, schedule pickup (assign courier & tracking number), mark complete, or reject.
- Security configuration:
  - `/api/returns/**` requires authenticated customer.
  - `/api/admin/returns/**` requires role `OWNER`, `MANAGER`, or `ADMIN`.
  - Non-authorized attempts receive HTTP 403 `{"success":false,"message":"Not authorised to perform this action"}`.

### R4. Admin Moderation & Reverse Logistics Console (`/admin/returns`)
- Create `ManageReturns.jsx` accessible by `OWNER`, `MANAGER`, and `ADMIN`.
- Register `/admin/returns` route in `AppRouter.jsx` and add "Returns & Exchanges" link under the Commerce section in `AdminDashboard.jsx`.
- Dashboard metrics: Total Claims, Pending Review, Pickups Scheduled, Completed Refunds.
- Filterable claims table with customer details, order amount, return type, reason, and photo thumbnails.
- Inspection Drawer/Modal to view high-resolution defect photos side-by-side with order details.
- 1-Click Action Controls:
  - **Approve Return**: Advances status to `APPROVED`.
  - **Assign Courier & Schedule Pickup**: Modal/input for courier name (e.g. BlueDart Reverse Logistics, Delhivery) and AWB tracking number; advances to `PICKUP_SCHEDULED`.
  - **Complete Refund**: Advances status to `COMPLETED`.
  - **Reject Return**: Modal for mandatory rejection reason; advances to `REJECTED`.

### R5. Order Card Return Telemetry on Storefront
- Order cards on `MyOrders.jsx` show dynamic return telemetry:
  - If return submitted: Show status pill (e.g. `Return: Pending Review`, `Pickup Scheduled - BlueDart (AWB: BDR-89214)`).
  - Include a "View Return Status" link opening a drawer with tracking milestones and admin notes.
  - If rejected: Show `Return Rejected` with the admin's explanation.

---

## Acceptance Criteria

### Automated Backend Tests
- [ ] Unit & integration tests in `ReturnServiceImplTest.java`:
  - Return creation succeeds on delivered order <= 7 days old.
  - Return creation fails on non-delivered order (e.g. `SHIPPED` or `PENDING`).
  - Return creation fails on delivered order > 7 days old.
  - Duplicate return submission on same order is rejected.
  - Unauthorized customer cannot access another customer's return claim.
  - Staff (`OWNER`, `MANAGER`, `ADMIN`) can update status, assign courier AWB, and approve/reject.
- [ ] Command: `./mvnw test -Dtest=ReturnServiceImplTest` passing with 100% pass rate.
- [ ] Full backend test regression: `./mvnw test` passing 100% with zero errors (all 60+ tests passing).

### Production Frontend Build
- [ ] Command: `cd frontend && npm run build` passing with zero errors.
- [ ] All bundle chunks strictly under 500 kB (pure Tailwind/SVG, no heavy external libraries).

### Storage & Resource Constraints
- [ ] Free disk space maintained >= 30% (~70+ GiB available on `/System/Volumes/Data`) via `~/scripts/check_disk_health.sh`.

## 2026-09-17T11:21:27Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: Full multi-agent team

Execute Phase 13 Stage 4 (WhatsApp Production Readiness) for SareeKart luxury e-commerce. Audit, harden, and verify the Meta WhatsApp Cloud API integration, HMAC-SHA256 signature verification, opt-in/STOP/START regulatory compliance, message templates, rate limiting, and failure isolation.

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: development

---

## Requirements

### R1. Meta WhatsApp Webhook Security & Signature Hardening
- Webhook endpoints (`GET /api/webhook/whatsapp` and `POST /api/webhook/whatsapp`) must be fully compliant with Meta Graph API v19.0.
- `GET` verification must validate `hub.verify_token` against `WHATSAPP_WEBHOOK_VERIFY_TOKEN` and return `hub.challenge`.
- `POST` ingestion must strictly validate `X-Hub-Signature-256` using HMAC-SHA256 against `WHATSAPP_APP_SECRET`. Forged, tampered, or missing signatures in production must be rejected with HTTP 401/403.
- In-memory and persistent idempotency: Every incoming message (`wam_id`) must be deduplicated via `WhatsAppIdempotencyService` to prevent duplicate AI agent runs or message replies.

### R2. Regulatory Compliance: Opt-In, STOP, and START Protocol
- Enforce strict customer consent lifecycle:
  - When customer texts `STOP`, `UNSUBSCRIBE`, or `CANCEL`, update `WhatsAppContact.optedIn = false` and suppress all promotional and automated outbound messages.
  - When customer texts `START` or `UNSTOP`, restore `WhatsAppContact.optedIn = true` with a polite welcome confirmation.
  - Outbound order notification triggers (`OrderNotificationService`, `WhatsAppNotificationService`) must verify contact opt-in status before sending marketing or non-critical messages.

### R3. Message Templates & Phone Number Normalization
- Phone normalization: E.164 and Indian mobile formats (`+91`, `91`, leading `0`, 10 digits) must normalize consistently via `WhatsAppIdentityService`.
- Pre-approved Meta HSM message templates (Order Placed, Shipped, Delivered, Return Pickup) must be defined with dynamic parameter substitution and validated against template variables.
- Sensitive data masking: Never transmit JWT tokens, passwords, raw customer credit cards, or internal database primary keys in outbound WhatsApp payloads.

### R4. Failure Isolation, Rate Limiting & Admin Escalation
- Resilient failure domain: If Meta WhatsApp Cloud API is unreachable or rate-limited (HTTP 429 / 5xx), all core ecommerce operations (Order checkout, catalog browsing, cart) must continue uninterrupted.
- Rate limiting & backoff: Outbound messages must be throttled per recipient and adhere to Meta tier throughput limits.
- Human-in-the-loop escalation: When a customer asks for a human agent or sentiment analysis detects high frustration, conversation status transitions from `BOT_HANDLING` to `HUMAN_ESCALATION`, notifying staff via WebSocket on `/topic/admin/inbox`.

### R5. Isolation of Bridal Trousseau WhatsApp Integration
- Verify that Phase 11 Bridal Trousseau WhatsApp collaboration (`TrousseauWhatsAppService`) remains cleanly isolated from standard commerce messaging, using dedicated share tokens without cross-contamination of conversation threads.

---

## Acceptance Criteria

### Automated Backend Verification
- [ ] Comprehensive unit & integration tests authored in `WhatsAppProductionReadinessTest.java`:
  - `GET /api/webhook/whatsapp` returns 200 with challenge on valid token, 403 on invalid.
  - `POST /api/webhook/whatsapp` rejects invalid/forged `X-Hub-Signature-256` HMAC.
  - Duplicate `wam_id` messages are dropped idempotently without duplicate replies.
  - Incoming `STOP` keyword flips contact `optedIn` to `false` and halts automated responses.
  - Incoming `START` keyword restores `optedIn` to `true`.
  - Phone numbers normalize correctly across `+91`, `91`, and 10-digit variants.
  - Outbound notification failure does NOT roll back or abort order placement transactions.
- [ ] Full backend regression: `./mvnw test` passing 100% (478+ tests passing, 0 failures, 0 errors).

### Production Build & Storage Gates
- [ ] Production frontend build passes (`cd frontend && npm run build`) with all chunks strictly `< 500 kB`.
- [ ] System maintains `>= 30%` free disk space verified via `~/scripts/check_disk_health.sh`.
- [ ] No WhatsApp production secrets committed to Git repository.
- [ ] Audit and verification report authored in `docs/phase-13-stage-4-whatsapp-readiness-audit.md`.

## 2026-09-18T15:41:53Z

# Teamwork Project Prompt — Draft

> Status: Launched
> Goal: Craft prompt → get user approval → delegate to teamwork_preview
> Requested team: Full multi-agent team

Deploy SareeKart luxury handlooms publicly with a strict ₹0 budget, orchestrating a zero-cost architecture across TiDB Cloud Serverless (MySQL 8.0 wire-compatible), Vercel static hosting, and free-tier containerized Spring Boot 3 without purchasing AWS EC2, paid hosting, or paid databases.

Working directory: /Users/chaitanyachaitu/Downloads/SareeKart-main
Integrity mode: development

---

## Requirements

### R1. Free-Tier Cloud Database Migration (TiDB Cloud Serverless)
- Connect to the user's free TiDB Cloud Serverless cluster using TLS (`useSSL=true`, `allowPublicKeyRetrieval=true`).
- Execute clean-room Flyway baseline migration:
  Empty DB -> V1 baseline -> 20 base tables + catalog -> V17...V32 -> 37 tables + 368 columns.
- Verify 100% schema parity across all 37 tables and 368 columns.
- Ensure Flyway history records all 17 migrations (`V1`, `V17`–`V32`) with status `SUCCESS`.
- Enforce Hibernate `ddl-auto=validate` during Spring Boot initialization against TiDB Cloud without any schema mutation errors.
- Do NOT alter existing migrations `V17`–`V32`.

### R2. Spring Boot Backend Deployment under 512 MB RAM Constraint
- Package and deploy Spring Boot 3 (Java 17) using genuinely free application/container hosting (Render Web Service free tier / Koyeb).
- Enforce strict memory optimization:
  JAVA_TOOL_OPTIONS="-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
- Configure environment variables securely via provider secret management:
  - `SPRING_PROFILES_ACTIVE=prod`
  - `SPRING_DATASOURCE_URL=jdbc:mysql://<tidb_host>:<tidb_port>/<tidb_db>?useSSL=true&allowPublicKeyRetrieval=true`
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=validate`
  - `SPRING_FLYWAY_ENABLED=true`
  - `NEO4J_ENABLED=false`
  - `JWT_SECRET=<secure_64_char_secret>`
  - `CORS_ALLOWED_ORIGINS=https://sareekart.com,https://*.vercel.app`
- Verify Neo4j graceful degradation: With `NEO4J_ENABLED=false`, the built-in circuit breaker activates `fallbackHydrate()`, serving curated category and trending handlooms directly from MySQL without disrupting commerce or user experience.
- Measure and document cold-start times (expected 40–50s on free container sleep).

### R3. React/Vite Frontend Edge Static Hosting (Vercel)
- Deploy the production React/Vite SPA bundle to Vercel's global Edge CDN for ₹0.
- Verify production build satisfies the bundle budget: all chunk sizes strictly < 500 kB (max chunk: 229 kB).
- Maintain client-side SPA routing via `frontend/vercel.json` rewrites:
  { "rewrites": [{ "source": "/(.*)", "destination": "/index.html" }] }
- Point frontend `VITE_API_BASE_URL` to the public HTTPS backend URL.
- Test SPA routing across `/products`, `/login`, `/cart`, `/wishlist`, and `/admin`.

### R4. Security, HTTPS & Zero-Cost Isolation
- Enforce end-to-end TLS/HTTPS across both frontend and backend.
- Maintain private isolation: Ensure database and internal ports (4000, 3306, 7687, 8081) are not exposed publicly.
- Zero plaintext credentials: No database passwords, JWT secrets, or tokens committed to Git or printed in reports.
- Non-destructive commerce: No real payment transactions; preserve Razorpay in test mode (`rzp_test_...`) and support WhatsApp enquiry / manual order workflow.

---

## Acceptance Criteria

### Automated Regression & Code Quality
- [ ] Backend test suite passes cleanly: 575/575 tests PASS (`mvn test`).
- [ ] Frontend linter passes with 0 errors (`npm run lint`).
- [ ] Frontend production build succeeds with all chunks strictly < 500 kB.
- [ ] Working tree clean, `.gitignore` strictly ignores `.env*` and `.env.tidb`.

### Cloud Database & Migration Parity
- [ ] TiDB TLS connection verified.
- [ ] All 17 Flyway migrations (`V1` baseline -> `V17`..`V32`) applied with 0 errors.
- [ ] 37/37 tables and 368/368 columns verified in TiDB Cloud.
- [ ] Spring Boot boots with `spring.jpa.hibernate.ddl-auto=validate` with zero schema mismatch warnings.

### 15-Point Safe Public Smoke Test Suite
- [ ] Homepage: Luxury handloom branding, hero banner, category showcase load over HTTPS.
- [ ] Product Catalog: Sarees render with images, prices, and fabric tags.
- [ ] Product Detail: Specs, artisan stories, care instructions render.
- [ ] Search: Multi-attribute search queries return matching handloom sarees.
- [ ] Categories: Reference categories load correctly.
- [ ] Customer Auth: Registration and login return valid JWTs.
- [ ] Wishlist: Add/remove items persists across authenticated sessions.
- [ ] Cart: Cart additions and price subtotals calculate accurately.
- [ ] Checkout: Checkout initialization completes without error.
- [ ] Admin Auth: Role-based access control enforces `ADMIN`/`OWNER` privileges.
- [ ] Admin Dashboard: Analytics, catalog management, and orders dashboards load.
- [ ] robots.txt: Search engine crawling directives served over HTTPS.
- [ ] sitemap.xml: SEO sitemap served with valid URLs.
- [ ] Backend Health & Readiness: `/actuator/health` returns `{"status":"UP"}`.
- [ ] Neo4j Safe Fallback: Recommendations serve MySQL curated fallbacks without error.

---

## Execution Guardrails & Cost Compliance
- Total Incurred Cost: Strictly ₹0.
- No AWS EC2 / RDS / Paid VPS: Any step requiring credit card entry or payment must halt immediately as BLOCKED.
- Domain Policy: Use temporary Vercel HTTPS subdomain (`*.vercel.app`) for initial smoke testing; provide exact DNS instructions for `sareekart.com` only after 100% test pass.
- Stage Boundary: HALT immediately after Stage 2B-FREE report is compiled. Do NOT proceed to Stage 3.

## 2026-09-18T15:48:24Z

Continue Phase 0 discovery and proceed through the approved Stage 2B-FREE execution plan.

Do not skip validation.
Do not incur any cost.
Do not expose credentials.
Stop immediately on any blocker.

Once all execution milestones are complete, return the exact
PHASE 14 — STAGE 2B-FREE EXECUTION RESULT
with PASS or BLOCKED and evidence for every required check.

Do not proceed to Stage 3.

## 2026-09-18T15:51:38Z

Continue execution.

Frontend Phase 0 survey is complete.
Finish the remaining DB and backend discovery, synthesize PROJECT.md, then proceed through Milestone 1 and the remaining approved Phase 14 Stage 2B-FREE milestones.

Maintain:
- ₹0 maximum cost
- zero credential exposure
- no paid upgrades
- no real payments
- no production WhatsApp messages
- no changes to frozen phases
- no migration modifications
- stop on any blocker

Do not stop at survey completion.
Continue through actual deployment and verification.

Required final gate:
PHASE 14 — STAGE 2B-FREE EXECUTION RESULT
Status: PASS / BLOCKED

Do not proceed to Stage 3.

## 2026-09-19T09:10:06Z

Server restart recovery. All 3 surveys (frontend, backend, database) in Phase 0 are complete with full reports and handoffs:
- survey_frontend.md (229 kB chunk budget verified, SPA rewrites in place, 15 smoke tests mapped)
- survey_backend.md (512 MB memory constraint, -Xmx384m -XX:+UseSerialGC, Neo4j fallback verified)
- survey_db.md (17 Flyway migrations V1->V32, 37 tables / 368 columns parity verified)

Resume execution immediately: synthesize findings into PROJECT.md, proceed through Milestone 1 and the remaining approved Phase 14 Stage 2B-FREE milestones.

Enforce all guardrails:
- Strict ₹0 cost
- Zero credential leaks
- ddl-auto=validate
- Stop on any blocker
- Provide final audited PHASE 14 — STAGE 2B-FREE EXECUTION RESULT. Do not proceed to Stage 3.
