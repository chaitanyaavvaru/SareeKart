# DISPATCH: Backend Architecture & Domain Telemetry Survey

## Mission
Survey the SareeKart backend codebase to map existing domain models, repositories, controllers, security configuration, and data structures required to build R1 (Sales & Financial Telemetry Engine), R2 (Inventory Velocity & Stock Telemetry), R3 (Customer Cohorts & Geographic Analytics), and R5 (Access Control & Authorization).

## Authority & Inputs
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md` (specifically ## Follow-up — 2026-09-04T15:17:07Z).
- Working directory: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend`
- Backend code: `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`

## Investigation Scope
1. Domain Entities & Fields:
   - Orders, OrderItems, OrderStatus, payment info, shipping, tax, discounts, timestamps.
   - Products, SKUs, inventory stock, warehouse/stock tracking, categories.
   - Users, Roles (OWNER, MANAGER, ADMIN, CUSTOMER), addresses, creation dates.
   - Coupons, discounts, cart/cart items.
2. Repositories & Query Capabilities:
   - Existing Spring Data JPA repositories and custom JPQL/native queries.
   - How date filtering and aggregations can be implemented (Today, 7D, 30D, 90D, YTD, Custom).
3. Controllers & Endpoints:
   - Current admin API controllers (e.g. `AdminController.java`).
   - What analytics endpoints already exist vs what needs to be created.
4. Security & Access Control (R5):
   - Current Spring Security config, JWT/session filters, role authorization rules.
   - How to enforce OWNER, MANAGER, ADMIN access while returning HTTP 403 Forbidden with `{"message": "Not authorised to perform this action"}` (or standard error response) for unauthorized roles/customers.

## Output Requirements
Write `handoff.md` in your working directory with detailed findings, file paths, entity relationships, query recommendations, and endpoint designs. Update `progress.md` with your liveness heartbeat.
