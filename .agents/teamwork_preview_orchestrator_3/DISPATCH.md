## 2026-09-04T15:18:07Z

Build a comprehensive Analytics & Reporting Suite for SareeKart covering:
- R1. Sales & Financial Telemetry Engine (configurable time intervals: Today, 7D, 30D, 90D, YTD, Custom; gross sales, net revenue, tax, shipping, AOV, transaction counts, payment method distribution, coupon utilization, revenue timeline trend).
- R2. Inventory Velocity & Stock Telemetry (fast vs slow moving SKUs, stock turnover velocity / Days of Inventory Remaining, aging categories, stockout alerts and replenishment priority).
- R3. Customer Cohorts & Geographic Analytics (LTV distribution, new vs returning revenue contribution & repeat rate, regional breakdown, cart abandonment & conversion funnel ratios).
- R4. Interactive Admin Analytics Dashboard (/admin/analytics responsive console with KPI summary cards, period-over-period percentage comparisons, interactive visual charts, date-range filter pills, one-click CSV/Excel export, admin sidebar integration).
- R5. Access Control & Authorization (restricted to OWNER, MANAGER, ADMIN; customers/unauthenticated get HTTP 403 Forbidden with "Not authorised to perform this action").

Verification Plan & Acceptance Criteria:
- Automated Backend Tests: Spring Boot service & controller unit tests (./mvnw test) passing with zero errors.
- Automated End-to-End Playwright Tests: tests/analytics.spec.js passing with zero errors, and full regression passing with 100% pass rate.

Maintain your plan.md, progress.md, and BRIEFING.md inside /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_orchestrator_3/.
When all tasks are complete and verified, report completion to the sentinel.
