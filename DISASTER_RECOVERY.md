# SareeKart — Disaster Recovery & Operational Runbook

**Authoritative Domain:** `https://sareekart.com`  
**Primary Database:** `sareekart_db` (MySQL 8.0 / :3306)  
**Auxiliary Graph:** Neo4j Community (:7687)  
**Static & Uploads:** `/uploads/` + Cloud CDN  
**Last Drill Executed:** September 18, 2026 (Phase 13 · Stage 7)

---

## 1. Emergency Recovery Sequence (22-Step Incident Runbook)

When an unrecoverable production failure or data corruption incident occurs, follow this sequence:

```text
 1. DECLARE INCIDENT: Notify incident commander, start incident log, and record timestamp.
 2. DETERMINE SCOPE: Identify failed subsystems (MySQL, Neo4j, Host, App, Storage).
 3. PROTECT CURRENT STATE: Snapshot failed container/disk logs and metrics for post-mortem.
 4. STOP UNSAFE WRITES: Route ingress (Nginx) to maintenance page if data corruption is active.
 5. VERIFY LATEST BACKUP: Run `./manage.sh verify backups/sareekart_db_latest.sql`.
 6. PROVISION ISOLATED RECOVERY TARGET: Create staging/target schema (`sareekart_recovery_drill_db` or secondary host).
 7. RESTORE DATABASE: Run `./manage.sh restore backups/sareekart_db_latest.sql <target_db>`.
 8. VALIDATE SCHEMA & FLYWAY: Verify all 37 tables and run Hibernate schema check (`ddl-auto: validate`).
 9. DEPLOY KNOWN-GOOD APPLICATION: Deploy canonical release build (JAR/container).
10. RESTORE / VERIFY OBJECT STORAGE: Verify `/uploads/` directory mount and permissions.
11. RESTORE / REBUILD NEO4J: Trigger `syncFullCatalogFromMySql()` to repopulate graph from MySQL.
12. VERIFY HEALTH: Probe `/actuator/health` to confirm Spring Boot reports UP.
13. VERIFY AUTHENTICATION: Test customer login and JWT token issuance.
14. VERIFY CATALOG: Verify `/api/products` returns active drapes with pricing.
15. VERIFY CART: Test cart retrieval and item addition.
16. VERIFY ORDERS: Confirm order history lookup for existing customers.
17. VERIFY INVENTORY: Confirm stock decrements and inventory status.
18. VERIFY PAYMENT STATE: Confirm webhook signature validator and Razorpay gateway readiness.
19. VERIFY WHATSAPP: Confirm webhook signature validation and opt-in settings.
20. RESUME TRAFFIC: Point Nginx reverse proxy to restored application backend.
21. MONITOR: Watch application logs (`backend.log`), error rates, and latency for 30 minutes.
22. DOCUMENT INCIDENT: Finalize incident post-mortem with root cause and RTO/RPO metrics.
```

---

## 2. DR Command Reference (`manage.sh`)

SareeKart provides built-in disaster recovery and diagnostic tooling in `./manage.sh`:

| Command | Purpose | Safety Safeguard |
|---|---|---|
| `./manage.sh backup` | Creates timestamped logical dump in `backups/` with SHA-256 checksum and auto-rotates to 10 latest. | Pre-checks disk space ($\ge 30\%$ free required). |
| `./manage.sh verify <file>` | Inspects SQL dump header, verifies SHA-256 checksum, and confirms 37 critical tables. | Read-only inspection. |
| `./manage.sh restore <file> <target_db>` | Restores backup into specified target database. | Requires explicit target. Blocks restoring to `sareekart_db` unless `--force-production-overwrite` is passed. |
| `./manage.sh dr_check` | Automated 6-point DR diagnostic (MySQL, schema, backup recency, disk headroom, Neo4j, artifact). | Read-only diagnostic probe. |
| `./manage.sh status` | Checks process status of MySQL, Backend, and Frontend. | Read-only status probe. |

---

## 3. Subsystem Recovery Procedures

### 3.1 MySQL Database
- **Live Database:** `sareekart_db` on port 3306.
- **Backup Location:** `backups/sareekart_db_YYYYMMDD_HHMMSS.sql`.
- **Restoration to Isolated Target (Recommended for Verification):**
  ```bash
  ./manage.sh restore backups/sareekart_db_latest.sql sareekart_recovery_drill_db
  ```
- **Production Overwrite (Emergency Only):**
  ```bash
  ./manage.sh restore backups/sareekart_db_latest.sql sareekart_db --force-production-overwrite
  ```

### 3.2 Neo4j Knowledge Graph
- **Role:** Auxiliary cache and traversal layer. MySQL is the immutable single source of truth.
- **Offline Tolerance:** If Neo4j goes down, `Neo4jConfig` logs a warning and commerce continues with deterministic MySQL fallbacks.
- **Reconstruction:** Call `Neo4jGraphServiceImpl.syncFullCatalogFromMySql()` to regenerate all nodes (`Product`, `Category`, `Color`, `Fabric`, `Occasion`, `Motif`) and edges directly from MySQL. Duration $< 0.5\text{s}$.

### 3.3 Object & Image Storage
- **Location:** Local mount `/uploads/` (`saree-photos`, `return-photos`) and external CDN URLs.
- **Degradation Defense:** If an image is missing, frontend components (`ProductCard`, `ProductDetailPage`, `ItemCard`) render graceful fallback drapes. APIs and checkout operations never fail due to missing images.

### 3.4 Secret & Configuration Recovery
- **Enforcement:** Never store secrets in Git or logs.
- **Categories Required:**
  - `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD`
  - `JWT_SECRET`
  - `RAZORPAY_KEY_ID` / `RAZORPAY_KEY_SECRET`
  - `WHATSAPP_API_TOKEN` / `WHATSAPP_WEBHOOK_APP_SECRET` / `WHATSAPP_WEBHOOK_VERIFY_TOKEN`
  - `OPENAI_API_KEY`
  - `NEO4J_PASSWORD`

---

## 4. Rollback Framework

### 4.1 Application Rollback
- Re-deploy previous known-good commit (e.g. `af65830`).
- Because all Flyway migrations (`V17`–`V32`) are additive (new tables, nullable columns, performance indexes), older application versions run safely against the current schema without DDL reversion.

### 4.2 Database Rollback (Forward-Only Policy)
- Flyway migrations are forward-only in production. Do NOT manually edit `flyway_schema_history` or rewrite historical scripts.
- To reverse a schema change:
  1. Author a corrective forward migration (e.g. `V33__revert_...sql`).
  2. If data corruption occurred, restore a verified point-in-time backup to a clean schema.

---

## 5. Measured Drill Performance (Phase 13 · Stage 7)

| Metric | Target Policy | Measured Drill Performance |
|---|---|:---:|
| **Backup Creation Time** | Under 60s | **0.8 seconds** (160 KB) |
| **Database Restore Time** | Under 120s | **1.0 second** (37 tables) |
| **Application Boot & Health Probe** | Under 30s | **4.5 seconds** |
| **Total Restore-to-Health (RTO)** | Business target not yet defined | **~6.3 seconds** |
| **Data Loss Window (RPO)** | Business target not yet defined | **$< 24\text{ hours}$ (Automated daily)** |
