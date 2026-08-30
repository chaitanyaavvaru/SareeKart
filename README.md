# SareeKart

Premium Indian saree e-commerce platform — Spring Boot 3 / Java 17 backend,
React 19 + Vite storefront, MySQL 8, JWT auth with strict CUSTOMER/ADMIN
separation.

## Quick Start (local development)

Prerequisites: Java 17, Docker (for MySQL), Node 20.

```bash
# 1. Database — any MySQL on :3306 works; example uses a container on :3307
docker run -d --name sareekart-mysql-dev \
  -e MYSQL_DATABASE=sareekart -e MYSQL_ROOT_PASSWORD=sareekart123 \
  -p 3307:3306 mysql:8.0

# 2. Backend (Flyway migrations run automatically on boot)
cd backend/backend
SPRING_DATASOURCE_URL='jdbc:mysql://localhost:3307/sareekart?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true' \
SPRING_DATASOURCE_PASSWORD=sareekart123 \
./mvnw spring-boot:run

# 3. Frontend (Vite proxies /api -> localhost:8081)
cd frontend/frontend && npm install && npm run dev   # http://localhost:5173
```

### Demo accounts (seeded at startup)

| Role     | Email                    | Password    |
|----------|--------------------------|-------------|
| Admin    | admin@sareekart.com      | admin123    |
| Customer | customer@sareekart.com   | customer123 |

Seeding is idempotent and disabled in production via `APP_SEED_ENABLED=false`.

## Payments (Razorpay)

Online payments use Razorpay Orders + server-side HMAC signature
verification. The backend is the **only** pricing authority: the amount sent
to Razorpay is recomputed from the stored SareeKart order, never taken from
the browser.

### Test-mode setup

1. Create a Razorpay account → Dashboard → *Settings → API Keys* → generate
   **Test Mode** keys.
2. Add to `.env` (compose) or your shell (local dev):
   ```bash
   APP_RAZORPAY_ENABLED=true        # false ⇒ COD-only mode; no keys required
   RAZORPAY_KEY_ID=rzp_test_xxxxxxxx
   RAZORPAY_KEY_SECRET=xxxxxxxx     # never leaves the backend
   RAZORPAY_WEBHOOK_SECRET=xxxxxxxx # from Dashboard → Webhooks
   ```
3. With `APP_RAZORPAY_ENABLED=true`, missing credentials **fail startup**
   (`IllegalStateException`) — a half-configured money path must not boot.

### Payment flow

```
checkout ──POST /orders (paymentMethod=RAZORPAY)──▶ order PENDING / payment PENDING
        ──POST /payments/create-order/{orderId}──▶ gateway order minted & stored
                                                   (idempotent: retries reuse it)
        ──Razorpay Checkout modal (amount = server value)──
        ──POST /payments/verify {orderId, rzpOrderId, rzpPaymentId, signature}──
             HMAC-SHA256(orderId|paymentId, KEY_SECRET) verified server-side
             PAID ⇒ inventory committed once ⇒ status PROCESSING
webhook ──POST /payments/webhook (X-Razorpay-Signature over RAW body)──
             payment.captured / payment.failed handled idempotently via a
             unique event-id ledger (webhook_events)
```

Idempotency guarantees: repeated verify/webhook deliveries can neither charge
twice, duplicate orders, nor double-decrement stock — transitions are guarded
by a pessimistic row lock plus explicit state checks, and webhook replays are
absorbed by the event ledger.

### Test commands

```bash
# full suite incl. payment unit + integration tests (needs SPRING_DATASOURCE_*)
./mvnw test
# integration flow only
./mvnw test -Dtest=PaymentFlowIntegrationTest
```

## Coupons & Discounts

The backend is the **absolute pricing authority**. The browser may send a
coupon *code* — never amounts, percentages, or validity.

### Pricing formula (all BigDecimal, HALF_UP 2dp)

```
subtotal   = Σ cart line totals
discount   = PERCENTAGE : subtotal × value/100      → capped by maximumDiscountAmount if set
             FIXED_AMOUNT: value
             clamped so 0 ≤ discount ≤ subtotal     ⇒ payable can never go negative
taxable    = subtotal − discount
tax        = taxable × 5%
shipping   = FREE when pre-discount subtotal ≥ ₹5,000, else ₹150  (existing rule)
total      = taxable + tax + shipping               ← stored; Razorpay paise = total × 100
```

### Lifecycle & concurrency

```
preview  POST /coupons/preview {code}   advisory quote against the caller's LIVE cart;
                                        re-validated server-side at order time
reserve  at order placement            row-lock on coupon (SELECT..FOR UPDATE) serializes
                                        same-code checkouts; quota check counts held
                                        reservations; INSERT guarded by UNIQUE(order_id)
confirm  payment commit (online PAID / COD placement) → redeemed=true, counted permanently
release  failed online payment OR cancellation of an UNCOMMITTED order → row deleted;
                                        committed usage is never returned on refund/cancel
```

Validation rejects: unknown code · inactive · not started · expired · minimum
order unmet · global limit exhausted · per-user limit exhausted · malformed
format (`^[A-Z0-9_-]+$`) · duplicate reservation on the same order.

Historical pricing is snapshotted per order (`coupon_code`, `discount_amount`,
`subtotal`, `tax_amount`, `shipping_amount`, `total_amount`) — later coupon
edits never rewrite old orders.

### Admin surface

`POST|PUT|GET /admin/coupons` (ROLE_ADMIN) manage codes with type, value,
min-order, max-cap, window, and both usage limits; the list exposes usage
counters that customer previews never see.

### Testing

```bash
./mvnw test -Dtest=CouponServiceTest            # pricing-rule units
./mvnw test -Dtest=CouponFlowIntegrationTest    # full HTTP+MySQL lifecycle
./mvnw test -Dtest=CouponConcurrencyTest        # parallel limit race (real DB)
```

## Refunds

Admin-only lifecycle over captured payments. Three distinct status axes are
kept deliberately separate:

| Axis | Values | Meaning |
|---|---|---|
| `refunds.status` | PENDING / SUCCESS / FAILED | one gateway refund movement |
| `orders.paymentStatus` | PAID → PARTIALLY_REFUNDED → REFUNDED | aggregate of successful refunds |
| `orders.status` | PROCESSING/SHIPPED/… | fulfillment truth — **never changed by refunds** |

### Flow

```
POST /admin/orders/{id}/refund {amount?, reason}   (ROLE_ADMIN; absent amount ⇒ FULL)
  → server derives full = captured − Σ(successful refunds)
    (partial amounts validated against the same remainder)
  → gateway POST /payments/{id}/refund via RazorpayGateway port
  → refunds row persisted with provider_refund_id (UNIQUE ⇒ idempotent)
webhook refund.processed / refund.failed
  → signature-verified, event-ledger deduped, transitions the row
  → recompute flips order.paymentStatus when cumulative SUCCESS ≥ captured
```

### Inventory rule

Automatic restock **exactly once**, guarded by `orders.inventory_restocked`,
only when ALL hold: cumulative successful refunds = captured amount AND
order not yet SHIPPED/DELIVERED/RETURNED. After fulfillment the refund is
financial-only — physical returns are a manual workflow.

### Coupon rule

Unchanged from Phase 7: committed coupon usage is never returned by a
refund. Proven by tests (`couponUsageInvariant`, concurrency suite).

### Concurrency & idempotency

Initiation, webhook confirmation and sweeper expiry all lock the same
payment row (pessimistic), so they fully serialize. Duplicate webhooks,
replayed events and repeated admin clicks collapse via: event-id ledger +
`uk_refunds_provider_refund_id` + PENDING-guard + durable restock flag.

### Reconciliation

Late gateway captures on sweeper-expired orders trigger an automatic full
gateway refund (`SYSTEM:RECONCILE`) without resurrecting the order; failures
surface as FAILED rows in `GET /admin/refunds` plus CRITICAL logs.
Dashboard-initiated refunds missing a local row are reconstructed from
`refund.processed` webhooks.

### Test commands

```bash
./mvnw test -Dtest=RefundServiceTest            # math/validation units
./mvnw test -Dtest=RefundFlowIntegrationTest    # full HTTP+MySQL lifecycle
./mvnw test -Dtest=RefundConcurrencyTest        # parallel duplicate webhooks
```

## Refund Reconciliation

When webhooks are lost, duplicated, or delayed, refunds stuck in local
`PENDING` are reconciled against the gateway's authoritative state.

### Scheduler

`RefundReconciliationJob` runs at `APP_REFUND_RECONCILIATION_INTERVAL_MS`
(default 120s, initial 30s). Candidates: PENDING refunds older than
`APP_REFUND_RECONCILIATION_STALE_MINUTES` (default 15) whose retry backoff
has elapsed. Max 50 per pass (`BATCH_SIZE`).

### Per-refund outcomes

| Gateway state | Local action |
|---|---|
| processed | SUCCESS + WEBHOOK_MISSING anomaly (INFO) + aggregate recompute |
| failed | FAILED (financial-only; no restock) |
| still pending | attempt/backoff bump; max-attempts parks row + REFUND_STUCK_PENDING WARNING |
| unknown id | UNKNOWN_REFUND CRITICAL anomaly; parked |
| amount mismatch | GATEWAY_LOCAL_STATE_MISMATCH CRITICAL; money state NOT mutated |

### Retry / backoff

Each reconciliation attempt increments `attempt_count` and sets
`next_retry_at = now + min(cap, base × 2^attempt)` (base 60s, cap 1h).
After `max_attempts` (10) the refund is parked and a REFUND_STUCK_PENDING
anomaly raised. Network/5xx errors are treated as temporary; gateway 404 as
terminal.

### Anomalies

Persisted in `reconciliation_anomalies` with structured codes:
REFUND_AMOUNT_EXCEEDS_CAPTURE · PAYMENT_NOT_FOUND ·
ORDER_ALREADY_CANCELLED_BUT_CAPTURED · UNKNOWN_REFUND ·
GATEWAY_LOCAL_STATE_MISMATCH · REFUND_STUCK_PENDING · WEBHOOK_MISSING.
Admin views via `GET /admin/reconciliation/anomalies`, resolves via PATCH.

### Manual trigger

`POST /admin/reconciliation/refunds/run` (ROLE_ADMIN) returns
`{examined, reconciled, stillPending, failed, anomalies}`.

## File Storage

Product images are stored via a pluggable `FileStorageService` abstraction.
Domain code never depends on AWS SDK classes.

### Providers

| Provider | Config | Use case |
|---|---|---|
| `local` | `APP_STORAGE_PROVIDER=local` + `UPLOAD_DIR` | Development / testing |
| `s3` | `APP_STORAGE_PROVIDER=s3` + AWS vars below | Production |

### S3 setup

```bash
APP_STORAGE_PROVIDER=s3
AWS_S3_BUCKET=your-bucket
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=...        # or use IAM role in production
AWS_SECRET_ACCESS_KEY=...
AWS_CDN_BASE_URL=            # optional CloudFront domain
```

IAM policy: `s3:PutObject`, `s3:GetObject`, `s3:DeleteObject` on the bucket.
Bucket must be read-only for public access (no write permissions).

For MinIO/LocalStack testing, set `AWS_S3_ENDPOINT=http://localhost:9000`.

### Object key strategy

Keys are server-generated: `products/{productId}/{uuid}.{ext}` — immutable,
collision-free, CDN-cacheable (`Cache-Control: max-age=31536000, immutable`).
Client filenames are never used as storage paths.

### Validation

Content-type allow-list (JPEG, PNG, WebP, GIF), magic-byte sniffing (not
just Content-Type header), 5 MB max, empty-file check.

## Abandoned-Checkout Sweeper

Online orders left in `PENDING/PENDING` longer than
`APP_SWEEPER_STALE_MINUTES` (default 30) are expired automatically every
`APP_SWEEPER_INTERVAL_MS` (default 60s, fixed-delay, initial 20s):

```
order    PENDING → CANCELLED   (cancel_reason = EXPIRED, cancelled_at set)
payment  PENDING → FAILED      (history source SWEEPER:EXPIRED)
coupon   reservation released  (uncommitted orders only)
inventory untouched             (unpaid online orders never committed any)
```

Safety properties:
- **Serialized against late payments** via the same pessimistic payment-row
  lock as verify/webhook; verification of an expired/cancelled order is
  rejected with 400 and captured-webhook replays are logged for manual refund.
- **Idempotent**: candidate query excludes non-PENDING; per-order atomic tx;
  repeated passes are no-ops.
- **Bounded**: max `APP_SWEEPER_BATCH_SIZE` (100) per pass, fixed-delay so
  passes never overlap themselves.
- **Ops trigger**: `POST /admin/sweeper/run` (ROLE_ADMIN) returns
  `{examined, expired}`.
- COD and PAID orders are excluded by construction (query + re-check under lock).

## Full Stack via Docker Compose

```bash
cp .env.example .env        # fill DB_PASSWORD + JWT_SECRET
docker compose up --build   # storefront at http://localhost:5173
```

nginx serves the SPA and reverse-proxies `/api` to the backend container;
the database is never exposed beyond the compose network (host port is for
debugging only).

## Testing

```bash
# Backend — unit tests run anywhere; integration tests need the env below
SPRING_DATASOURCE_URL=... SPRING_DATASOURCE_PASSWORD=... ./mvnw test

# Frontend build
cd frontend/frontend && npm run build
```

CI (`.github/workflows/ci.yml`) runs backend tests against a MySQL service,
builds the frontend, and builds both Docker images on every push/PR.

---

## Architecture & Decision Records

**ADR-1 · Backend owns all pricing.** Cart lines carry unit prices from
catalog state at add-time, but order totals (subtotal, GST 5%, shipping
₹150 flat / free ≥ ₹5,000) are recomputed server-side at order creation.
The checkout UI mirrors the same formula for display only.

**ADR-2 · JWT in localStorage.** Chosen for simplicity and to match the
existing axios interceptor design. Known trade-off: XSS exposure. Mitigated
by React's output escaping and a strict no-`dangerouslySetInnerHTML-with-user-data`
policy; HttpOnly-cookie migration is a tracked follow-up.

**ADR-3 · Strict role isolation.** `/admin/**` requires `ROLE_ADMIN`;
customer surfaces (`/cart`, `/orders`, `/wishlist`, `/addresses`) require
`ROLE_CUSTOMER`. Verified by integration tests. Admins cannot shop with an
admin token by design.

**ADR-4 · Order addresses are immutable JSON snapshots**
(`AddressSnapshot`). Later edits to a user's address book never rewrite
historical invoices. Snapshot field names (`fullName`, `streetAddress`)
mirror the frontend contract to keep rendering dumb.

**ADR-5 · Soft-delete products.** Deactivation preserves referential
integrity of past `order_items`; catalog endpoints filter `active = true`.

**ADR-6 · Flyway-only schema evolution.** Hibernate runs with
`ddl-auto: validate`, so any entity drift fails fast at boot rather than
silently mutating production schema. Migrations V1–V7 are append-only.

**ADR-7 · Context-path awareness.** The API lives under `/api`
(`server.servlet.context-path`). Spring Security matchers therefore exclude
the prefix — a previous mismatch silently disabled all rules. Integration
tests now assert public catalog access to guard this invariant.

**ADR-8 · Frontend contract adaptation over rewrites.** Where mock-era
frontend shapes were reasonable (string image URL arrays, `product.price`,
wishlist items keyed by product id), the backend DTOs were adapted instead
of rewriting working components.

**ADR-9 · Razorpay via thin REST adapter, no SDK.** Orders are created with
Spring `RestClient` + Basic auth behind a `RazorpayGateway` port; signatures
are verified with JDK HMAC-SHA256 in constant time. This keeps the money
path fully unit-testable (mock the port) with zero extra dependencies.
The Key Secret never appears in responses, logs, or frontend code; logs carry
only masked provider ids (`***last6`).

**ADR-10 · Inventory commits at the payment boundary.** COD orders commit
stock atomically at creation (rollback preserves cart on shortfall); online
orders commit exactly once when verification/webhook flips payment to PAID —
guarded by pre-mutation committed-state capture plus a pessimistic lock on
the payment row. Cancellation restocks only what was actually committed.

**ADR-11 · Webhook dedup ledger.** `webhook_events.event_id` is UNIQUE and
inserted in a REQUIRES_NEW transaction, absorbing Razorpay's at-least-once
delivery without poisoning the caller's transaction (rollback-only trap).

## Known Debt / Roadmap

- Razorpay live mode: flow is test-mode complete; production keys + webhook
  endpoint registration in the Razorpay dashboard remain operational steps.
- Coupon validation endpoint absent; `couponCode` accepted but ignored.
- ~60 pre-existing frontend lint findings (strict react-hooks rules +
  legacy `/legacy` monolith in `App.jsx`); lint not yet a CI gate.
- Image uploads write to local disk (`app.upload.dir`); S3-compatible
  storage behind a `FileStorageService` interface is the planned upgrade.
- Payment refunds (`refund.processed`) are not yet handled; `REFUNDED`
  states exist in the model but no automated transition.
- A gateway capture racing sweeper expiry is accepted but logged for manual
  refund reconciliation (sweeper never resurrects the order).
- One coupon per order; coupon stacking is out of scope.
