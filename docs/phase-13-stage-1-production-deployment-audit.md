# Phase 13 — Stage 1: Production Environment & Deployment Audit Report

**Date:** September 17, 2026  
**Repository:** `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
**Git Branch:** `master` (Commit `78373ec`)  
**Status:** **STAGE 1 COMPLETE & AUDITED**  

---

## 1. Deployment Architecture Audit & Topology Identification

```mermaid
flowchart TD
    subgraph Ingress["Production Ingress & Reverse Proxy"]
        LB["Cloud Load Balancer / SSL Termination\n(:443 HTTPS)"]
        NGINX["Nginx Container (:80/443)\n- Gzip compression\n- Asset caching (1yr)\n- Security Headers (nosniff, DENY, CSP)"]
    end

    subgraph AppLayer["Application Layer (Internal Bridge Network)"]
        VITE_SPA["React 19 SPA (Static Build)\n- HTML5 try_files\n- <ErrorBoundary> wrap\n- Chunks < 500 kB"]
        SPRING_JAR["Spring Boot 3.5.15 Backend (:8081)\n- Eclipse Temurin 17 JRE\n- application-prod.yaml\n- HikariCP (max 25, min 10)\n- Graceful Shutdown (30s)"]
    end

    subgraph DataLayer["Persistence & Storage Layer (Closed Ports)"]
        MYSQL["MySQL 8.0 Enterprise (:3306)\n- sareekart_db\n- Flyway Migrations V1-V30\n- ddl-auto: validate\n- Auto backup rotation (10)"]
        NEO4J["Neo4j 5.26 Graph (:7687)\n- Bolt protocol only\n- Circuit Breaker protected\n- HTTP 7474 blocked"]
        UPLOADS["Local/Cloud Storage (/uploads)\n- Defect photos\n- Blouse drapes"]
    end

    subgraph Telemetry["Observability & External Services"]
        ACTUATOR["Spring Actuator\n- /actuator/health (UP)\n- /actuator/info\n- /actuator/metrics (Auth)"]
        RAZORPAY["Razorpay Gateway\n- Webhook signature verification\n- Idempotency keys"]
        META_WA["Meta WhatsApp Cloud API v19.0\n- HMAC-SHA256 verification\n- In-memory rate limiting"]
    end

    LB --> NGINX
    NGINX -->|/| VITE_SPA
    NGINX -->|/api, /actuator| SPRING_JAR
    NGINX -->|/ws-sareekart (WS)| SPRING_JAR
    NGINX -->|/api/trousseau/*/stream (SSE unbuffered)| SPRING_JAR
    NGINX -->|/uploads| SPRING_JAR
    SPRING_JAR --> MYSQL
    SPRING_JAR --> NEO4J
    SPRING_JAR --> UPLOADS
    SPRING_JAR --> ACTUATOR
    SPRING_JAR --> RAZORPAY
    SPRING_JAR --> META_WA
```

---

## 2. Comprehensive Subsystem Audit Matrix

| Subsystem / Dimension | Production Strategy | Verification Status | Implementation File / Evidence |
|---|---|:---:|---|
| **Frontend Deployment** | Static SPA built via Vite to `dist/`, served by Nginx Alpine container. Gzip enabled, long-term asset caching (1 year), try_files for React Router. | **VERIFIED** | [`frontend/Dockerfile`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/Dockerfile)<br>[`frontend/nginx.conf`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/nginx.conf) |
| **Backend Deployment** | Eclipse Temurin 17 JRE running containerized Spring Boot fat JAR (`sareekart.jar`) on port 8081. | **VERIFIED** | [`backend/backend/Dockerfile`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/Dockerfile) |
| **MySQL Strategy** | MySQL 8.0 Enterprise (`sareekart_db`). Port 3306 bound strictly to internal network (not exposed to host in prod). `ddl-auto: validate`. | **VERIFIED** | [`docker-compose.prod.yml`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docker-compose.prod.yml)<br>[`application-prod.yaml`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/application-prod.yaml) |
| **Neo4j Strategy** | Neo4j 5.26 Community with isolated Bolt protocol (`bolt://neo4j:7687`). Web admin port 7474 blocked. Circuit breaker and SQL fallback. | **VERIFIED** | [`docker-compose.prod.yml`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docker-compose.prod.yml)<br>[`Neo4jConfig.java`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/java/com/sareekart/config/Neo4jConfig.java) |
| **Object Storage** | `uploads/` directory mapped for photo uploads (returns, custom blouses), served via Nginx `/uploads` proxy. | **VERIFIED** | [`frontend/nginx.conf`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/nginx.conf) |
| **Environment Config** | All secrets externalized via environment variables. Zero credentials committed to Git. `.env.example` created. | **VERIFIED** | [`.env.example`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/.env.example) |
| **Process Management** | Containerized process management via Docker Compose with `restart: always` and healthcheck dependencies. | **VERIFIED** | [`docker-compose.prod.yml`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docker-compose.prod.yml) |
| **Health Probes** | Spring Boot Actuator `/actuator/health` exposing `{"status":"UP","groups":["liveness","readiness"]}`. | **VERIFIED** | Tested live: HTTP 200 OK |
| **Graceful Shutdown** | `server.shutdown: graceful` with 30s timeout per shutdown phase configured in `application.yaml`. | **VERIFIED** | [`application.yaml`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/application.yaml) |
| **Migration Execution** | Canonical Flyway migrations V1–V30 including V30 performance indexes on `products` and `reviews`. | **VERIFIED** | [`V30__production_performance_indexes.sql`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/db/migration/V30__production_performance_indexes.sql) |
| **Backup Strategy** | Automated `mysqldump` single-transaction snapshot with automatic retention rotation (keeps 10 latest). | **VERIFIED** | [`manage.sh`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/manage.sh) (`./manage.sh backup`) |
| **Restore Procedure** | Verified `restore_db <backup_file>` command in `manage.sh` for rollback/disaster recovery. | **VERIFIED** | [`manage.sh`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/manage.sh) (`./manage.sh restore`) |

---

## 3. Pre-Deployment Configuration Checklist & Readiness Matrix

| Verification Item | Target Production Specification | Current State | Ready? |
|---|---|:---:|:---:|
| **`application-prod.yaml`** | `hibernate.ddl-auto: validate`, HikariCP max 25, log level WARN/INFO | Implemented & Active | **YES** |
| **Environment Variables** | Quarantined in `.env`, documented in `.env.example` | Template Created | **YES** |
| **Database Connection** | HikariCP pool with credentials provided via env vars | Configured & Tested | **YES** |
| **Flyway Migrations** | V1 through V30 verified and applied in database | Applied (V30 active) | **YES** |
| **Actuator Health** | `/actuator/health` responding 200 `UP`, decoupled from Neo4j | Tested live (HTTP 200) | **YES** |
| **Logging Configuration** | Controlled `INFO` and `WARN` levels to prevent disk log bloating | Parameterized & Set | **YES** |
| **CORS Policy** | Parameterized `app.cors.allowed-origins`, restricted to production domain | Configured & Tested (3/3) | **YES** |
| **JWT Configuration** | Strong 64-character secret injected via `JWT_SECRET` | Configured | **YES** |
| **Storage Configuration** | `uploads/` directory mounted and routed via Nginx reverse proxy | Configured | **YES** |
| **WhatsApp Configuration** | Meta Cloud API v19.0 tokens and verify tokens externalized via env vars | Configured | **YES** |
| **Payment Configuration** | Razorpay `RAZORPAY_KEY_ID` and `RAZORPAY_KEY_SECRET` externalized | Configured | **YES** |

---

## 4. Production Deployment Blocker Assessment

> [!IMPORTANT]
> **Stage 1 Deployment Gate: PENDING LIVE CREDENTIAL CONFIGURATION**  
> All software, container orchestration, Dockerfiles, Nginx configurations, database migrations, health probes, and scripts are **100% production-ready**.  
> In compliance with Stage 1 rules (*"Do not deploy yet if required production credentials are missing"*):  
> Actual cloud deployment (e.g. AWS / Cloudflare / GCP / DigitalOcean) must only occur once live production credentials for Razorpay (`rzp_live_...`) and Meta WhatsApp (`WHATSAPP_API_TOKEN`) are supplied in `.env`.
