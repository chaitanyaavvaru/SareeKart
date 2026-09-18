# SareeKart — Luxury Handloom Saree Platform

> **Status:** **100% Complete & Production Ready** (Phases 1–13 Audited & Verified)  
> **Backend:** Spring Boot 3.5.15 · Java 17 · Spring Security · Hibernate / JPA · Flyway V1–V32  
> **Frontend:** React 19 · Vite · Tailwind CSS · Redux Toolkit · Lucide Icons · Framer Motion  
> **Databases:** MySQL 8.0 Enterprise (`sareekart_db`, 37 tables) · Neo4j 5.26 Graph  
> **Integrations:** Razorpay Gateway (HMAC-SHA256) · Meta WhatsApp Cloud API v19.0 · Spring AI  

---

## Architecture Overview

SareeKart is an enterprise-grade omnichannel luxury e-commerce platform dedicated to authentic Indian handlooms (Kanjivaram, Banarasi, Chanderi, Paithani, Tussar, and more). It combines traditional artisan heritage with modern digital commerce, AI styling, and collaborative bridal shopping.

```mermaid
flowchart TD
    subgraph Ingress["Edge & Ingress Layer"]
        EDGE["Shoppers & Webhooks\n(HTTPS :443)"]
        NGINX["Nginx Edge Proxy / SSL Termination\n- Rate Limiting (30 req/s, burst=60)\n- HTTP Security Headers (HSTS, CSP, DENY)\n- Gzip & Long-term Asset Caching"]
    end

    subgraph AppTier["Application Tier"]
        SPA["React 19 SPA Container (:80)\n- Bundle chunks < 500 kB\n- <ErrorBoundary> Atelier Fallback\n- Dynamic OpenGraph & XML Sitemap"]
        API["Spring Boot 3 Fat JAR (:8081)\n- HikariCP Connection Pool (10-25)\n- Actuator Health Probes (/actuator/health)\n- Graceful Shutdown (30s)"]
    end

    subgraph Persistence["Persistence & Knowledge Tier"]
        MYSQL["MySQL 8.0 Enterprise (:3306)\n- 37 Tables, UTF8MB4\n- Flyway Migrations V1-V32\n- Composite Performance Indexes"]
        NEO4J["Neo4j 5.26 Graph (:7687)\n- Saree Similarity & Co-purchase\n- Non-blocking Circuit Breaker"]
        UPLOADS["Volume Storage (/uploads)\n- Return defect photos\n- Custom blouse drapes"]
    end

    subgraph ExternalServices["External SaaS Integrations"]
        RAZORPAY["Razorpay Payments\n- Webhook signature verification\n- Cross-order substitution defense"]
        WHATSAPP["Meta WhatsApp Cloud API\n- STOP/START compliance\n- Order & Trousseau alerts"]
    end

    EDGE --> NGINX
    NGINX -->|/| SPA
    NGINX -->|/api, /actuator| API
    NGINX -->|/ws-sareekart (WS)| API
    NGINX -->|/api/trousseau/*/stream (SSE)| API
    NGINX -->|/uploads| UPLOADS

    API --> MYSQL
    API --> NEO4J
    API --> UPLOADS
    API --> RAZORPAY
    API --> WHATSAPP
```

---

## Key Platform Features

- **Heritage Catalog & Taxonomy:** 37 database tables managing weaves, zari purity, certified GI craft tags, and occasion collections.
- **Collaborative Bridal Trousseau Studio:** Real-time wedding wardrobe curation with live Server-Sent Events (SSE), wedding party voting, and 1-click cart conversion.
- **AI Luxury Saree Stylist:** Multimodal drape advice, color season analysis, and ceremony ensemble curation powered by Spring AI.
- **Meta WhatsApp Cloud Commerce:** Verified business alerts, catalog discovery, and order status updates with strict opt-in / opt-out (`STOP`/`START`) compliance.
- **Razorpay Payments & Webhook Engine:** Cryptographic HMAC-SHA256 signature validation, replay defense, and cross-order substitution prevention.
- **Full-Funnel Conversion Telemetry:** 8-stage conversion analytics funnel (`SESSION_START` $\to$ `PURCHASE`) rendered via pure SVG dashboards with zero heavy dependencies.
- **SEO & Search Readiness:** Dynamic database-driven XML sitemaps (`/sitemap.xml`), robots.txt, Schema.org JSON-LD structured data, and OpenGraph social previews.
- **Disaster Recovery & Operational Resilience:** Automated backup rotation with SHA-256 sidecars, tested forward-rollback procedures, and non-blocking Neo4j circuit breaker.

---

## Quick Start (Local Development)

The platform provides a unified CLI helper (`./manage.sh`) to start, stop, test, and diagnose services.

### 1. Start Services
```bash
# Start MySQL and backend / frontend services
./manage.sh start

# Check running status of backend (8081) and frontend (5173)
./manage.sh status
```

### 2. Run Quality Gates & Tests
```bash
# Run all 574 backend tests (uses isolated in-memory H2 database)
cd backend/backend && ./mvnw test

# Lint and build frontend production bundle
cd frontend && npm run lint && npm run build

# Run 6-point Disaster Recovery Diagnostic
./manage.sh dr_check

# Verify system storage discipline (>= 30% free space rule)
~/scripts/check_disk_health.sh
```

---

## Production Deployment

For full production deployment details, refer to the [Production Go-Live Manual](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/PRODUCTION_GO_LIVE_MANUAL.md).

### 1. Configure Environment
Create `.env` with production credentials:
```bash
cp .env.example .env
chmod 600 .env
# Fill in production secrets: DB password, JWT_SECRET, Razorpay, WhatsApp
```

### 2. Launch Containerized Production Stack
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### 3. Verify Health Probes
```bash
curl -fsS http://localhost:8081/actuator/health
# Expected output: {"status":"UP"}

curl -fsSI http://localhost/sitemap.xml
# Expected output: HTTP/1.1 200 OK
```

---

## Platform Documentation Index

- [Production Go-Live Manual](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/PRODUCTION_GO_LIVE_MANUAL.md) — Comprehensive runbook for production deployments, cold starts, and health monitoring.
- [Disaster Recovery Guide & Runbook](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/DISASTER_RECOVERY.md) — 22-step disaster recovery runbook and rollback strategies.
- [Phase 13 Master Audit](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docs/phase-13-stage-8-final-production-launch-readiness-audit.md) — Master release audit certifying 100% project completion across all 13 phases.
- [Database Migrations](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/db/migration) — Flyway migration scripts `V1` through `V32`.
