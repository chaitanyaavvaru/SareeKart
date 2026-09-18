# Phase 13 · Stage 5 — Analytics & Conversion Tracking Audit

**Repository:** `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
**Branch:** `master`  
**Date:** 2026-09-18  
**Status:** ✅ PHASE 13 — STAGE 5 PASS

---

## 1. Executive Summary

Stage 5 expands SareeKart's behavioral analytics pipeline from a basic 4-stage telemetry overview into a comprehensive, enterprise-grade **8-Stage Luxury eCommerce Conversion Funnel** alongside **Auxiliary Commerce Channel Tracking**, complete privacy and PII protections, and a high-performance, lightweight pure-SVG dashboard UI adhering strictly to the < 500 kB production bundle budget.

All existing Phase 1–12 functionality and legacy tests remain 100% intact and backward-compatible.

---

## 2. Telemetry & Funnel Architecture

### 2.1 The 8-Stage Conversion Funnel

The conversion funnel tracks unique customer sessions through the complete luxury saree purchasing lifecycle:

```
[1. LANDING_PAGE_VIEW] ──> [2. PRODUCT_VIEW] ──> [3. SEARCH_QUERY] ──> [4. CATEGORY_VIEW]
                                                                               │
[8. ORDER_COMPLETED]  <── [7. PAYMENT_ATTEMPT] <── [6. CHECKOUT_INITIATED] <── [5. ADD_TO_CART]
```

1. **Stage 1: Landing Page View (`LANDING_PAGE_VIEW`)**
   - Captured on user entry to the storefront via `HomePage.jsx` with source attribution.
2. **Stage 2: Product View (`PRODUCT_VIEW`)**
   - Captured on viewing drape details (`ProductDetailPage.jsx`).
3. **Stage 3: Search Query (`SEARCH_QUERY`)**
   - Captured when customers search the luxury catalog by motif, zari, fabric, or occasion.
4. **Stage 4: Category View (`CATEGORY_VIEW`)**
   - Captured when browsing curated collections (Kanjivaram, Banarasi, Chanderi, etc.).
5. **Stage 5: Add to Cart (`ADD_TO_CART`)**
   - Captured when an item is added to the shopping cart.
6. **Stage 6: Checkout Initiated (`CHECKOUT_INITIATED`)**
   - Captured when entering `/checkout`.
7. **Stage 7: Payment Attempt (`PAYMENT_ATTEMPT`)**
   - Captured when submitting the order with payment method selection (Razorpay, COD, Wallet), with payment card/CVV redaction.
8. **Stage 8: Order Completed (`ORDER_COMPLETED`)**
   - Captured upon successful order persistence and payment confirmation.

### 2.2 Mathematical Formulations

- **Step Conversion Rate ($CR_{step}$):**
  $$\text{Step Conversion Rate } (\%) = \left( \frac{\text{Sessions at Stage } i}{\text{Sessions at Stage } (i-1)} \right) \times 100$$
  *(For Stage 1, Step Conversion Rate is defined as 100.0%)*

- **Overall Conversion Rate ($CR_{overall}$):**
  $$\text{Overall Conversion Rate } (\%) = \left( \frac{\text{Sessions at Stage } i}{\text{Sessions at Stage 1}} \right) \times 100$$

- **Drop-off Rate:**
  $$\text{Drop-off Rate } (\%) = 100.0 - \text{Step Conversion Rate } (\%)$$

- **Key Transition Metrics:**
  - **Overall Conversion:** $(\text{ORDER\_COMPLETED} / \text{LANDING\_PAGE\_VIEW}) \times 100$
  - **Detail-to-Cart:** $(\text{ADD\_TO\_CART} / \text{PRODUCT\_VIEW}) \times 100$
  - **Cart-to-Checkout:** $(\text{CHECKOUT\_INITIATED} / \text{ADD\_TO\_CART}) \times 100$
  - **Checkout-to-Payment:** $(\text{PAYMENT\_ATTEMPT} / \text{CHECKOUT\_INITIATED}) \times 100$
  - **Payment-to-Order:** $(\text{ORDER\_COMPLETED} / \text{PAYMENT\_ATTEMPT}) \times 100$
  - **Cart Abandonment:** $\left(1.0 - \frac{\text{ORDER\_COMPLETED}}{\text{ADD\_TO\_CART}}\right) \times 100$

### 2.3 Auxiliary Commerce Channels

In addition to the primary purchase funnel, Stage 5 aggregates telemetry across all auxiliary conversion touchpoints:
- **Wishlist Activity:** Additions and removals (`WISHLIST_ADD`, `WISHLIST_REMOVE`).
- **AI Recommendations:** Clicks and conversions from visual similarity and related drapes (`RECOMMENDATION_CLICK`).
- **AI Stylist:** Conversational stylist interactions and advisory sessions (`AI_CHAT_MESSAGE`).
- **WhatsApp Commerce:** Inbound/outbound conversational shopping engagements (`WHATSAPP_COMMERCE_ENGAGE`).
- **Collaborative Bridal Trousseau:** Board interactions, ensemble curations, ceremony voting (`TROUSSEAU_ENGAGE`).
- **Social Share Links:** Product and trousseau link sharing (`SHARE_LINK_ENGAGE`).

---

## 3. Backward Compatibility & Privacy Protection

### 3.1 Dual-Funnel Backward Compatibility
- Phase 6 defined and verified `overview.getFunnel()` containing 4 legacy steps (`PRODUCT_VIEW`, `ADD_TO_CART`, `CHECKOUT_INITIATED`, `ORDER_COMPLETED`).
- `CustomerBehaviorServiceTest.java` (line 464) asserts `assertEquals(4, overview.getFunnel().size())`.
- **Guarantee:** `overview.getFunnel()` continues to emit the exact 4-stage legacy funnel. The new 8-stage funnel is exposed via `overview.getEcommerceFunnel()` and the dedicated endpoint `GET /api/admin/customer-behavior/funnel`.

### 3.2 Database Schema Zero-Migration
- `customer_events.event_type` is stored as `VARCHAR(50)` (configured in migration `V27__customer_behavior_telemetry.sql`).
- All 6 new `CustomerEventType` values map cleanly into the existing column without requiring an ALTER TABLE migration or database restart.

### 3.3 Privacy & PII Hardening
- Payment payloads tracked at `PAYMENT_ATTEMPT` record only the payment method (`RAZORPAY`, `COD`, `WALLET`), order amount, and currency.
- Credit card numbers, expiration dates, CVVs, user passwords, and sensitive auth tokens are strictly scrubbed before persistence or telemetry transmission.

### 3.4 Role-Based Access Control
- `GET /api/admin/customer-behavior/funnel` is protected with `@PreAuthorize("hasAnyRole('OWNER', 'MANAGER', 'ADMIN')")`.
- Wildcard `@CrossOrigin(origins = "*")` was removed from `AdminCustomerBehaviorController.java` to prevent CORS vulnerability.

---

## 4. Frontend Implementation & Bundle Budget

### 4.1 UI Architecture
- Implemented in `frontend/src/pages/Admin/AnalyticsDashboard.jsx`.
- **Pure SVG & CSS Visualizations:** Custom SVG horizontal funnel bars, proportional area fills, chevron stage connectors, and auxiliary channel cards.
- **Zero Heavy Dependencies:** No `chart.js`, `recharts`, or `d3` dependencies were added, preserving strict bundle budgets.

### 4.2 Bundle Verification
```
dist/assets/AnalyticsDashboard-U74QVIKf.js          42.08 kB │ gzip:  9.05 kB
dist/assets/vendor-react-BkBKdGRo.js               229.01 kB │ gzip: 73.54 kB
✓ built in 469ms
```
- Largest chunk: 229.01 kB (`vendor-react`), well below the **500 kB** maximum budget rule.

---

## 5. Test Suite Verification

### 5.1 Unit & Integration Tests

| Test Suite | Tests Run | Failures | Errors | Result |
|---|---|---|---|---|
| `CustomerBehaviorFunnelTest.java` | 9 | 0 | 0 | ✅ PASS |
| `CustomerBehaviorServiceTest.java` | 17 | 0 | 0 | ✅ PASS |
| Full Backend Regression (`./mvnw test`) | 558 | 0 | 0 | ✅ PASS (3 skipped) |

### 5.2 Test Coverage Highlights
- `CustomerBehaviorFunnelTest`:
  - `testCustomerEventTypeTaxonomyIncludesAllEightFunnelStages`: Verifies all 8 stages are declared.
  - `testCustomerEventTypeTaxonomyIncludesAuxiliaryChannels`: Verifies auxiliary channel types.
  - `testEcommerceFunnelMathAndDropOffCalculation`: Verifies accurate stage counts, step conversion rates, drop-off rates, and transition metrics.
  - `testEmptyTelemetryGracefulHandling`: Verifies division-by-zero safety with 0 counts.
  - `testAuxiliaryChannelEngagementAggregation`: Verifies aggregation of Wishlist, Recommendations, AI Stylist, WhatsApp, Trousseau, and Share links.
  - `testLegacyOverviewFunnelBackwardCompatibility`: Guarantees legacy `overview.getFunnel()` remains 4 stages.
  - `testTelemetryPayloadExcludesSensitivePii`: Confirms credit cards, CVVs, passwords, and tokens are scrubbed.
  - `testAdminFunnelEndpointAccess`: Verifies `GET /api/admin/customer-behavior/funnel` returns HTTP 200 with complete funnel payload.
  - `testAdminFunnelEndpointUnauthorizedForCustomers`: Confirms customer role is forbidden (HTTP 403).

---

## 6. Storage & Resource Discipline

- Disk headroom before tests: 46.3% free (105.7 GiB free).
- Disk headroom after full build & test: 46.3% free (105.7 GiB free).
- Target policy: **>= 30% Free Space** (`check_disk_health.sh` [PASS]).
