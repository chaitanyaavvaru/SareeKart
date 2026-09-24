# SareeKart 30-Day Post-Launch Operations Runbook

> **Platform Version:** 3.0.0-PROD (Enterprise Luxury Handloom Marketplace)  
> **Repository:** `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
> **Production Domain:** `https://sareekart.com`  
> **Operational Status:** ACTIVE (₹0 Infrastructure Discipline)  
> **Last Updated:** September 2026  

---

## 1. Zero-Cost Production Topology & Architecture

SareeKart operates under strict ₹0 infrastructure discipline, delivering enterprise reliability, high availability, and data integrity through zero-cost cloud tiers:

```
[User Browser]
       │
       ▼ HTTPS (Anycast Edge CDN)
[Vercel Global Edge] (React 18 / Vite SPA)
       │
       │ Proxied API & WebSocket Requests
       ▼
[Render / Koyeb Free Web Service] (Spring Boot 3 / Java 17)
  ├── Memory Budget: -Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1
  ├── HikariCP Pool: max-pool-size: 8, min-idle: 2, conn-timeout: 20s
  └── Resilience: Neo4j Circuit Breaker -> Deterministic MySQL Fallback Hydration
       │
       ▼ TLS Encrypted Wire Protocol (Port 4000)
[TiDB Cloud Serverless] (MySQL 8.0 Wire-Compatible, 37 Tables / 368 Columns)
```

---

## 2. Daily Operational Cadence (Morning Routine: 04:00–04:30 UTC)

| Task | Objective | Execution Command / Tool | Success Criteria |
|---|---|---|---|
| **1. Keep-Alive Verification** | Verify backend container is warm before morning shopping peak | Check `.github/workflows/keepalive.yml` or run `./scripts/operational_health_check.sh` | HTTP 200 `{"status":"UP"}` from `/actuator/health` |
| **2. Host Disk Headroom** | Ensure host system satisfies storage discipline ($\ge 30\%$ free space) | `~/scripts/check_disk_health.sh` | Available free space $\ge 30\%$ on `/System/Volumes/Data` |
| **3. Error & Crash Telemetry** | Scan application logs for unhandled exceptions or restart loops | `docker logs sareekart_backend --since 24h \| grep -E "(ERROR\|FATAL)"` | Zero unhandled NullPointerExceptions or OutOfMemoryErrors |
| **4. Payment Callback Status** | Verify Razorpay webhook delivery success rate | Check Admin Dashboard (`/admin/finance`) & Razorpay Merchant Console | Webhook success rate $\ge 98\%$, zero orphan captured payments |
| **5. Cold-Start Rate Check** | Monitor user impact during off-peak sleep cycles | Review GA4 / eventTracker `COLD_START_EXPERIENCED` events | Cold start notifications $< 2\%$ of total storefront sessions |

---

## 3. Weekly Maintenance Routine (Mondays: 05:00 UTC)

### 3.1 Automated Backup & Retention Drill
1. **Execute Snapshot**:
   ```bash
   ./scripts/backup_db.sh
   ```
2. **Verify Backup Integrity & Checksums**:
   ```bash
   ./scripts/verify_backup_integrity.sh
   ```
   *Gate*: 100% checksum match with valid DDL/DML table statements.
3. **Enforce Rolling Retention**:
   ```bash
   ./scripts/prune_backups.sh 7
   ```
   *Gate*: Prunes snapshots older than 7 days; maintains host storage headroom.

### 3.2 Slow Query & Index Audit
- Inspect TiDB Cloud / MySQL slow query log (`long_query_time = 0.5s`).
- Verify composite indexes (`idx_products_active_price`, `idx_products_active_category`, `idx_orders_razorpay_order_id`).
- Ensure product catalog queries (`/api/products`) maintain p95 latency $\le 25\text{ms}$.

### 3.3 Cache Hit Telemetry
- Inspect Vercel Analytics for static asset cache hit ratios on `dist/assets/*.js` and `*.css`.
- Target: Edge cache hit ratio $\ge 90\%$.

---

## 4. Monthly Governance & Security Verification

| Cadence | Area | Procedure | Gate |
|---|---|---|---|
| **Monthly (1st)** | **Dependency Vulnerabilities** | `cd frontend && npm audit`<br>`cd backend/backend && ./mvnw dependency-check:check` | 0 Critical, 0 High vulnerabilities |
| **Monthly (5th)** | **SSL/TLS Expiration** | `curl -Iv https://sareekart.com 2>&1 \| grep "expire date"` | Cert validity $> 30$ days remaining |
| **Monthly (10th)** | **Neo4j Fallback Sanity** | Simulate unreachable Neo4j driver; verify `/api/recommendations` | Serves curated MySQL fallback in $< 10\text{ms}$ |
| **Monthly (15th)** | **PII & Data Hygiene** | Audit database user records for unverified dummy accounts; verify zero plaintext cards | Zero raw PANs, CVVs, or unmasked passwords |
| **Monthly (20th)** | **Meta Catalog Sync** | Verify `https://sareekart.com/api/meta/catalog.csv` in Meta Commerce Manager | Zero rejected luxury saree feed items |

---

## 5. Quarterly Disaster Recovery (DR) Simulation Drill

**Target Objectives:**
- **Recovery Time Objective (RTO):** $\le 10$ minutes.
- **Recovery Point Objective (RPO):** $\le 24$ hours (latest verified backup).

### Step-by-Step DR Drill Procedure:
1. **Declare Drill Window**: Announce scheduled simulation drill to stakeholders.
2. **Obtain Latest Verified Snapshot**:
   ```bash
   LATEST_BACKUP=$(ls -1t backups/sareekart_db_*.sql | head -n 1)
   ./scripts/verify_backup_integrity.sh
   ```
3. **Spin Up Isolated Clean Recovery Target**:
   ```bash
   docker run -d --name sareekart_dr_mysql -e MYSQL_ROOT_PASSWORD=recovery123 -e MYSQL_DATABASE=sareekart_recovery -p 3309:3306 mysql:8.0
   sleep 10
   ```
4. **Execute Restoration**:
   ```bash
   mysql -h 127.0.0.1 -P 3309 -u root -precovery123 sareekart_recovery < "${LATEST_BACKUP}"
   ```
5. **Verify Row Counts & Table Parity**:
   ```bash
   TABLE_COUNT=$(mysql -h 127.0.0.1 -P 3309 -u root -precovery123 -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='sareekart_recovery';" -sN)
   echo "Restored Tables: ${TABLE_COUNT}/37"
   ```
6. **Teardown Recovery Container**:
   ```bash
   docker stop sareekart_dr_mysql && docker rm sareekart_dr_mysql
   ```
7. **Document Drill Log**: Record timestamps, table counts, and duration in `docs/dr-reports/`.

---

## 6. Telemetry Thresholds & Emergency Alert Matrix

| Metric / Signal | Warning Threshold | Critical Incident Threshold | Immediate Automated Action |
|---|---|---|---|
| **JVM Heap Consumption** | $> 300\text{ MB}$ (78%) | $> 350\text{ MB}$ (91%) | Trigger SerialGC cycle, alert ops |
| **HikariCP Active Connections** | $\ge 6$ active / 8 pool | 8 active (pool exhausted) | Log connection stack traces, inspect slow queries |
| **Payment Signature Failures** | $> 3$ in 10 minutes | $> 10$ in 10 minutes | Freeze checkout, alert security team for fraud attempt |
| **WhatsApp Ingestion Failures** | $> 5\%$ rejection rate | $> 20\%$ rejection rate | Inspect Meta webhook verify token & HMAC secret |
| **Host Disk Space Free** | $< 35\%$ free space | $< 30\%$ free space | Execute `./scripts/prune_backups.sh 3`, purge npm/pip caches |
| **Backend Latency (p95)** | $> 200\text{ms}$ | $> 500\text{ms}$ | Inspect database connection pool and TiDB latency |

---

## 7. Rollback Vector Quick-Reference

- **Frontend Issue**: Instant rollback via Vercel Dashboard -> Deployments -> Instant Rollback (`< 30s`).
- **Backend Issue**: Deploy previous clean commit tag on Render (`~2-3m`).
- **Database Schema Issue**: Restore verified pre-migration dump via MySQL CLI (`~5-10m`).
