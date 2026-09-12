# DISPATCH: Milestone M1 - Analytics Business Logic & Export Engineering

## Mission
Design the full business logic, aggregation algorithms, period-over-period formulas, inventory velocity metrics, customer cohorts, and Apache POI Excel/CSV export for Milestone M1 (`AnalyticsService` and `AnalyticsServiceImpl`).

## Authority & Scope
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md` (specifically ## Follow-up — 2026-09-04T15:17:07Z).
- Read `/Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md`.
- Working Directory: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2`
- Target Codebase: `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`

## Detailed Investigation
1. Interval & Comparative Range Engine:
   - Handle `TODAY`, `7D`, `30D`, `90D`, `YTD`, `ALL`, `CUSTOM`.
   - Calculate exact prior comparative window `[start - duration, start]` for period-over-period percentage delta:
     $$\Delta\% = \frac{\text{Current} - \text{Prior}}{\text{Prior}} \times 100$$
   - Safeguard against division by zero (e.g. prior = 0 -> delta = 0.0 or 100.0).
2. Financial Telemetry (R1):
   - Gross sales, net revenue, tax (5% GST calculation), shipping (₹150 for orders < ₹5000), AOV, transaction counts, payment method distribution, coupon ROI (`revenueGenerated / discountGiven`), and timeline breakdown.
3. Inventory Velocity & Stock Telemetry (R2):
   - Daily run-rate: `unitsSoldInPeriod / daysInPeriod`.
   - Days of Inventory Remaining: `availableStock / dailyRunRate` (safe fallback for 0 run-rate).
   - Fast vs slow moving SKU rankings.
   - Aging categories: `<30 days`, `30–90 days`, `>90 days`.
   - Stockout alerts & replenishment priority score (Critical 90-100, High 70-89, Med 40-69, Low <40).
4. Customer Cohorts (R3):
   - Customer LTV tiers: Platinum (>₹50,000), Gold (₹15,000 - ₹50,000), Silver (<₹15,000).
   - New vs returning customer revenue and repeat purchase rate.
   - Regional breakdown (top states and cities).
   - Conversion funnel & cart abandonment rate.
5. Report Export (R4):
   - Excel (`.xlsx`) using Apache POI `poi-ooxml 5.3.0` (existing dependency) for Sales Telemetry and Inventory Velocity sheets.
   - CSV streaming export with RFC 4180 format.

## Deliverable
Write `handoff.md` with full service interface specifications, implementation algorithms, edge-case protections, and POI export code templates.

## 2026-09-04T15:26:21Z
Design the full business logic, aggregation algorithms, period-over-period formulas, inventory velocity metrics, customer cohorts, and Apache POI Excel/CSV export for Milestone M1 (AnalyticsService and AnalyticsServiceImpl):
1. Configurable date intervals (TODAY, 7D, 30D, 90D, YTD, ALL, CUSTOM) and comparative period calculation for percentage delta.
2. Financial telemetry formulas (gross sales, net revenue, 5% GST tax, shipping, AOV, payment splits, coupon ROI).
3. Inventory velocity formulas (daily run-rate, days of inventory remaining, fast/slow SKUs, aging categories, stockout alerts and priority scoring).
4. Customer cohort formulas (LTV tiers, new vs returning revenue, repeat rate, regional breakdown, conversion funnel & cart abandonment).
5. Excel (.xlsx) and CSV export utilities using Apache POI (poi-ooxml 5.3.0).

Write your handoff report to /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_2/handoff.md. Regularly update progress.md with your liveness heartbeat. When done, notify caller via send_message.

