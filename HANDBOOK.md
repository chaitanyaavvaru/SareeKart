# SareeKart Enterprise Developer & Operations Handbook

Welcome to the definitive operational reference for **SareeKart**, a luxury Indian handloom e-commerce platform built with Spring Boot, React, and MySQL.

---

## 1. Quick Start & Unified Lifecycle (`./manage.sh`)

SareeKart provides a single, unified controller script for all local management:

| Command | Action | Description |
| :--- | :--- | :--- |
| `./manage.sh start` | **Start Services** | Boots MySQL, launches Spring Boot on `:8081`, and Vite on `:5173`. |
| `./manage.sh stop` | **Stop Services** | Gracefully terminates backend and frontend processes. |
| `./manage.sh restart` | **Restart** | Full process recycling and port cleanup. |
| `./manage.sh status` | **Health Check** | Real-time PID and port status report. |
| `./manage.sh test` | **Full Test Suite** | Runs 18 backend JUnit tests + 26 frontend Playwright E2E tests. |
| `./manage.sh loadtest` | **Stress & Load Test** | Simulates 100 concurrent virtual shoppers; measures RPS & p95 latency. |
| `./manage.sh backup` | **Database Dump** | Generates a timestamped SQL snapshot in `backups/`. |

---

## 2. Architecture & Ports

```
[ Browser / Playwright Tests ]
            │
            ▼
    http://localhost:5173  (React 19 + Vite 8 + Tailwind CSS)
            │
    (Reverse Proxy /api/**)
            ▼
    http://localhost:8081  (Spring Boot 3.5.15 + Java 17 + JPA Hibernate)
            │
            ▼
    localhost:3306         (MySQL 9.6: sareekart_db)
```

- **Frontend**: `http://localhost:5173` (listening on `0.0.0.0:5173` for IPv4 & IPv6).
- **Backend API**: `http://localhost:8081` (`/api/products`, `/api/categories`, `/api/admin/**`).
- **Database**: MySQL on `localhost:3306` (Credentials: `root` / `root123`).

---

## 3. Demo Accounts & Access

Pre-seeded accounts available on the login page via **1-Click Quick Access**:

| Role | Email | Password | Access Level |
| :--- | :--- | :--- | :--- |
| **Customer** | `customer@sareekart.com` | `customer123` | Storefront, Wishlist, Cart, Checkout, My Orders |
| **Admin** | `admin@sareekart.com` | `admin123` | Full Enterprise Operations & AI Studio (`/admin`) |

---

## 4. Enterprise Admin & AI Studio Directory

Accessible at `http://localhost:5173/admin` when authenticated as Admin:

### Store Operations
- `/admin` — **Sales Overview & Telemetry**: Gross sales, recent orders, inventory alerts.
- `/admin/products` — **Manage Sarees**: Catalog CRUD, stock alerts, pricing, categories.
- `/admin/orders` — **Order Fulfillment**: 10-stage delivery pipeline, printable AWB shipping labels.
- `/admin/users` — **Customer Identity**: Registered buyers, lifetime spend telemetry, account controls.

### Commerce & Finance
- `/admin/coupons` — **Coupons & Promotions**: Discount rule validation, minimum cart spend.
- `/admin/inventory` — **Multi-Warehouse Inventory**: Stock allocation, low-stock PO triggers.
- `/admin/finance` — **Finance & Taxation**: GST HSN 5007 5% breakdown, artisan payout escrow ledger.
- `/admin/cms` — **Storefront CMS & SEO**: Dynamic editorial banners, announcement tickers, Schema.org.

### SRE & Quality
- `/admin/security` — **Security & Compliance**: OWASP ASVS Level 2, RBAC, audit log trails.
- `/admin/performance` — **SRE Performance**: Core Web Vitals telemetry, sub-500ms p95 latency.
- `/admin/qa` — **Quality Gate**: Test pyramid (18 unit + 14 E2E), zero warning gates.
- `/admin/devops` — **DevOps & Disaster Recovery**: Container health, RTO/RPO SLAs, rollback triggers.
- `/admin/operations` — **Go-Live Operations Vault**: 4-milestone approval gate, RB-01 to RB-03 runbooks.

### AI Studio
- `/admin/ai-recommendations` — **AI Recommendations**: Hybrid scoring, 4 algorithms, revenue attribution.
- `/admin/ai-demand` — **AI Demand Forecasting**: 30-day time-series projections, weaver guild fill rates.
- `/admin/ai-pricing` — **AI Dynamic Pricing**: What-If price elasticity slider, 25% margin floor guardrail.
- `/admin/ai-visual-search` — **AI Visual Search**: 512-dim embedding similarity matching telemetry.
- `/admin/ai-stylist` — **AI Fashion Stylist**: 4-piece ensemble bundling (Saree + Blouse + Jewelry + Potli).

---

## 5. Automated Verification & Testing Commands

To run tests independently:

```bash
# 1. Backend JUnit Tests (18 tests)
cd backend/backend && ./mvnw test

# 2. Frontend Production Build & Bundle Audit
cd frontend && npm run build

# 3. Frontend ESLint Check (0 errors)
cd frontend && npm run lint

# 4. Playwright End-to-End Test Suite (26 tests: 14 Desktop + 12 Mobile)
cd frontend && npx playwright test --project=chromium

# 5. Mobile-Only Viewport Suite (12 tests: iPhone 13 & Pixel 5)
cd frontend && npx playwright test tests/mobile.spec.js --project=chromium

# 6. Unified Test (Runs everything sequentially)
./manage.sh test

# 7. High-Concurrency Stress & Load Test (100 shoppers, 30s)
./manage.sh loadtest
```

---

## 6. Docker Containerization (Optional)

To run the full stack inside Docker containers:

```bash
docker compose up --build
```
