# SareeKart — Phase 6: Customer Behavior & Telemetry Architecture & Implementation Report

## Executive Summary
Phase 6 implements an enterprise-grade, non-blocking customer behavioral telemetry and analytics pipeline for SareeKart. The system captures, aggregates, indexes, and analyzes every critical micro-interaction across the shopper journey—from anonymous guest browsing to authenticated checkout and purchase completion.

Designed strictly as an asynchronous observer, the telemetry subsystem maintains complete failure isolation: storefront browsing, search queries, cart additions, wishlist updates, payment processing, and inventory orders will never fail or experience latency degradation even if the telemetry ingestion cluster or database is unreachable.

This subsystem provides the immutable event log and behavioral foundation for:
- **Phase 7 — Neo4j Knowledge Graph**: Customer affinity edges (`(:User)-[:EXPLORED]->(:Product)`), co-occurrence graphs, and category weave walks.
- **Phase 8 — AI Recommendations**: Content-based and collaborative filtering feature stores with deterministic signals.
- **Phase 9 — AI Luxury Saree Stylist**: Drape and color profile calibration.
- **Phase 10 — WhatsApp Automated Commerce**: Abandonment remarketing and re-engagement triggers.

---

## 1. Event Taxonomy & Domain Model

The taxonomy defines 11 core event types across 4 primary entities (`PRODUCT`, `SEARCH`, `CATEGORY`, `ORDER`), supporting both web visitors and downstream intelligence systems.

| Event Type | Entity Type | Metadata Captured | Business Function & Downstream Target |
|---|---|---|---|
| `PRODUCT_VIEW` | `PRODUCT` | `name`, `category`, `fabric`, `color`, `price`, `dwellTimeMs`, `referrer` | Drape popularity, viewer retention, fabric affinity calculation (Phase 7 & 8). |
| `SEARCH_QUERY` | `SEARCH` | `query`, `resultCount`, `category`, `fabric`, `occasion`, `colorFamily` | Search intent, trending heritage searches, zero-result demand gap discovery. |
| `CATEGORY_VIEW` | `CATEGORY` | `categorySlug`, `categoryName`, `resultsCount` | Heritage interest categorization (e.g. Kanchipuram vs Banarasi vs Paithani). |
| `ADD_TO_CART` | `PRODUCT` | `productName`, `price`, `quantity`, `fabric`, `color`, `source` | High-intent demand indicator, funnel progression, collaborative filtering. |
| `REMOVE_FROM_CART` | `PRODUCT` | `productName`, `quantity`, `price` | Cart friction tracking, hesitation signals, price drop remarketing candidates. |
| `ADD_TO_WISHLIST` | `PRODUCT` | `productName`, `price`, `source` | Long-term aspiration signal, price sensitivity profiling. |
| `REMOVE_FROM_WISHLIST` | `PRODUCT` | `productName` | Wishlist curation tracking. |
| `CHECKOUT_INITIATED` | `ORDER` | `itemCount`, `cartValue`, `paymentMethod` | Top of purchase checkout funnel, dropoff analysis. |
| `ORDER_COMPLETED` | `ORDER` | `orderId`, `totalAmount`, `itemCount`, `paymentMethod` | Definite conversion event, high-confidence purchase edge (`:PURCHASED`). |
| `AI_STYLIST_ENGAGE` | `CONSULTATION` | `occasion`, `primaryColor`, `blouseStyle`, `sareeName` | Deep taste and aesthetic preference calibration for AI Stylist. |
| `VISUAL_SEARCH_ENGAGE` | `IMAGE` | `extractedWeave`, `extractedColor`, `confidence` | Visual preference embedding signal for computer vision similarity. |

---

## 2. Database Schema & Flyway Migration (V27)

File: `backend/backend/src/main/resources/db/migration/V27__customer_behavior_telemetry.sql`

```sql
CREATE TABLE IF NOT EXISTS customer_events (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    client_event_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    user_id BIGINT NULL,
    event_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NULL,
    entity_id BIGINT NULL,
    metadata JSON NULL,
    ip_address VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_customer_events_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_customer_events_client_event_id UNIQUE (client_event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_customer_events_session ON customer_events (session_id, created_at DESC);
CREATE INDEX idx_customer_events_user_type ON customer_events (user_id, event_type, created_at DESC);
CREATE INDEX idx_customer_events_type_created ON customer_events (event_type, created_at DESC);
CREATE INDEX idx_customer_events_entity ON customer_events (entity_type, entity_id, created_at DESC);
CREATE INDEX idx_customer_events_created ON customer_events (created_at DESC);
```

### Key Schema Characteristics:
1. **Deduplication Safeguard**: `client_event_id` with unique constraint guarantees idempotency if client retries or network double-submits.
2. **Nullable Foreign Key**: `user_id` allows anonymous guest event collection prior to sign in, cascading safely with `ON DELETE SET NULL`.
3. **Compound Performance Indexes**:
   - `(session_id, created_at DESC)`: Fast guest journey reconstruction.
   - `(user_id, event_type, created_at DESC)`: Rapid customer affinity and intent score computation.
   - `(event_type, created_at DESC)`: True conversion funnel aggregation without full table scans.

---

## 3. Client Telemetry Engine (`eventTracker.js`)

The storefront client implements the following operational safeguards:
1. **Client Buffering & Batching**: Events are queued in memory (`maxBufferSize = 10`) and flushed every 1.5 seconds, reducing network request volume by over 80%.
2. **Page Navigation Resilience**: Registers `pagehide`, `beforeunload`, and `visibilitychange` listeners to dispatch buffered events via `navigator.sendBeacon` without blocking browser unloads.
3. **Sensitive Data Redaction**: Automatically scans and strips passwords, auth tokens, secrets, card numbers, and CVVs before transmission.
4. **Resilient Failure Isolation Killswitch**: Evaluates `window.__DISABLE_TELEMETRY__`. When enabled or when network requests fail, errors are caught silently, guaranteeing that the customer UI never crashes or lags.

---

## 4. Session Management & Identity Resolution ($S \to U$)

1. **Anonymous Guest Tracking**:
   - Each browser session receives a random cryptographic UUID prefixed with `sess_`.
   - Stored in both `sessionStorage` (active tab) and `localStorage` (continuity across tabs).
2. **Identity Resolution on Authentication**:
   - When a user logs in or registers, the frontend triggers `POST /api/events/identify` with the guest `sessionId`.
   - The backend links all prior guest events with `session_id = :sessionId AND user_id IS NULL` to the authenticated user ID.
3. **Session Hijacking Protection**:
   - Events already associated with a different `user_id` are never overwritten by another user session, preventing cross-user event pollution.

---

## 5. Deterministic Customer Affinity Engine

The service computes transparent, deterministic behavioral profiles without black-box ML:

### Affinity Weights:
- `ORDER_COMPLETED`: +25
- `ADD_TO_CART`: +10
- `ADD_TO_WISHLIST`: +8
- `PRODUCT_VIEW` (> 10s dwell): +5
- `PRODUCT_VIEW` (standard): +2
- `SEARCH_QUERY`: +2
- `CATEGORY_VIEW`: +1

### Purchase Intent Score (0–100):
Calculated from active window recency, cart activity, and checkout engagement:
- Cart additions: +15 each (max 45)
- Checkout initiated: +25
- Repeated product views: +2 per view (max 20)
- Recent search queries: +2 per search (max 10)
- Total intent score bounded at 100.

### Price Sensitivity Classification:
- **BUDGET**: Average viewed/purchased price $< \text{₹}5,000$.
- **MID_MARKET**: Average price between $\text{₹}5,000$ and $\text{₹}15,000$.
- **LUXURY**: Average price $> \text{₹}15,000$.

---

## 6. Event-Driven 4-Stage Conversion Funnel

The admin console calculates true behavioral conversion across distinct customer sessions:
$$\text{Product View} \xrightarrow{\quad} \text{Add to Cart} \xrightarrow{\quad} \text{Checkout Started} \xrightarrow{\quad} \text{Order Completed}$$

Each stage reports:
- Stage occurrence volume
- Unique sessions reaching that stage
- Step-over-step conversion rate ($\%$)
- Overall end-to-end conversion rate ($\%$)

---

## 7. Verification & Automated Test Results

### Backend Automated Test Suite
- `CustomerBehaviorServiceTest`: 17 comprehensive unit/integration test cases covering ingestion, deduplication, identity resolution, affinity calculation, and funnel math.
- `CustomerBehaviorSecurityAndFailureTest`: 4 test cases testing RBAC access, unauthenticated blocking, and non-blocking failure recovery.
- **Full Backend Regression**: `./mvnw test`
  - **Total Tests**: 278
  - **Failures**: 0
  - **Errors**: 0
  - **Pass Rate**: 100%

### Frontend Playwright E2E Suite
- `phase6-customer-behavior.spec.js`:
  1. Guest session initialization & persistence.
  2. Product view & add to bag telemetry dispatching.
  3. Search query logging without layout shifting.
  4. Identity resolution on user sign in.
  5. Telemetry failure isolation killswitch verification.
  6. Admin behavioral telemetry dashboard and funnel rendering.
- **Production Frontend Build**: `npm run build`
  - Zero compilation errors.
  - All bundle chunks strictly $< 500$ kB (largest chunk 227 kB).
  - Storage headroom maintained $\ge 30\%$ free space.
