# SareeKart Luxury Handlooms — Phase 13 · Stage 7
## Disaster Recovery & Application Rollback Drill Audit Report

**Date:** September 18, 2026  
**Environment:** Production Readiness Validation  
**Authoritative Domain:** `https://sareekart.com`  
**Repository Working Directory:** `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
**Branch:** `master`  
**Commit HEAD:** `78373ec`  
**Status:** **PHASE 13 · STAGE 7 PASS**

---

### 1. Executive Summary

Phase 13 · Stage 7 executed a complete Disaster Recovery (DR) and Rollback drill for the SareeKart enterprise commerce platform. In compliance with strict safety directives:
1. **Zero Destructive Operations Against Production:** The primary database (`sareekart_db`) remained strictly protected and untouched throughout all recovery tests.
2. **Empirically Demonstrated Restoration:** A complete logical database restore was executed against an isolated target schema (`sareekart_recovery_drill_db`).
3. **Automated Schema & Business Validation:** Automated Spring Boot test suite [`DisasterRecoveryDatabaseRestoreDrillTest`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/test/java/com/sareekart/dr/DisasterRecoveryDatabaseRestoreDrillTest.java) validated schema integrity under `ddl-auto: validate` and confirmed 100% record parity across products, orders, inventory, trousseau, and users.
4. **Tooling Hardening:** [`manage.sh`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/manage.sh) was upgraded with strict target safeguards preventing accidental overwrites of `sareekart_db`, automated backup verification (`verify_backup`), SHA-256 checksums, and a 6-point DR diagnostic command (`dr_check`).
5. **Frozen Systems Maintained:** Core commerce systems (checkout, payments, auth, inventory, WhatsApp, AI stylist, SEO) operated without modification or regression.

---

### 2. Recovery Surface Classification (Stage 1)

| Subsystem | State Scope | Recovery Classification | Primary Recovery Mechanism |
|---|---|:---:|---|
| **MySQL Database** | 37 tables: catalog, orders, payments, users, inventory, trousseau, WhatsApp, telemetry | **CRITICAL** | Point-in-time logical restore via `manage.sh restore` |
| **Neo4j Graph** | Nodes (Product, Category, Weave, Motif) & Edges | **REBUILDABLE** | Regenerated from MySQL via `syncFullCatalogFromMySql()` |
| **Object Storage** | Defect photos, custom blouse uploads in `/uploads/` | **IMPORTANT** | Mounted volume snapshots + graceful UI fallback |
| **Configuration** | Environment variables, database credentials, secrets | **CRITICAL** | Externalized container env / secrets vault |
| **App Artifacts** | Spring Boot executable JAR, Vite frontend bundle | **REBUILDABLE** | Git source compilation (`./mvnw package`, `npm run build`) |
| **Sessions / Ephemeral** | WebSocket sessions, SSE emitter maps, in-memory tokens | **EPHEMERAL** | Re-established transparently upon client reconnection |

*Note: In accordance with zero-leakage security rules, no secret values are recorded in this audit report.*

---

### 3. Database Backup Validation & Hardening (Stage 2)

#### 3.1 Hardening Enhancements in `manage.sh`
- **Portability & GTID Safety:** Added `--set-gtid-purged=OFF`, `--routines`, and `--triggers` to `mysqldump` to avoid privilege issues and GTID collision across different MySQL hosts.
- **Storage Constraint Check:** Added disk headroom pre-check; refuses to create backups if free disk space is $< 30\%$.
- **Integrity Fingerprinting:** Computes SHA-256 checksum immediately after dump creation and writes `.sha256` sidecar file.
- **Error Visibility:** Captured stderr into `.backup_err.log` instead of silent suppression (`2>/dev/null`), surfacing non-zero exit codes.
- **Retention Governance:** Preserves strictly the 10 most recent backups, pruning older SQL dumps and checksums.

#### 3.2 Controlled Backup Metrics
- **Executed Command:** `./manage.sh backup`
- **Output File:** `backups/sareekart_db_20260918_171749.sql`
- **Backup Duration:** **0.8 seconds**
- **Dump Size:** **160 KB** (1,324 lines of valid SQL)
- **SHA-256 Hash:** `5cb79557d9a62f8837ac37cfa5cf28061ef15d7d521452a283bfb277de3dd67f`
- **Verification:** `./manage.sh verify backups/sareekart_db_latest.sql` confirmed all 37 table structures declared.

---

### 4. Database Restore Drill Execution (Stage 3)

The restore drill was conducted in an isolated disposable database (`sareekart_recovery_drill_db`).

#### 4.1 Safety Gate Validation
- Executing `./manage.sh restore backups/sareekart_db_latest.sql` without target produced:
  `✖ TARGET DATABASE UNSPECIFIED! Direct restoration requires explicit target database.` (Exit code 1).
- Executing `./manage.sh restore backups/sareekart_db_latest.sql sareekart_db` produced:
  `✖ DESTRUCTIVE OPERATION BLOCKED BY SAFETY GATE! Overwriting production requires --force-production-overwrite.` (Exit code 1).

#### 4.2 Isolated Restoration Results
- **Target Schema:** `sareekart_recovery_drill_db`
- **Restore Command:** `./manage.sh restore backups/sareekart_db_latest.sql sareekart_recovery_drill_db`
- **Restore Duration:** **1.0 second**
- **Restored Table Count:** **37 tables** (100% of schema)

#### 4.3 Data Parity & Business Record Verification
Exact row count comparisons confirmed zero data loss:

| Table | Live Database (`sareekart_db`) | Restored Target (`sareekart_recovery_drill_db`) | Parity Status |
|---|:---:|:---:|:---:|
| `products` | 25 | 25 | **100% MATCH** |
| `categories` | 8 | 8 | **100% MATCH** |
| `users` | 12 | 12 | **100% MATCH** |
| `orders` | 50 | 50 | **100% MATCH** |
| `order_items` | 68 | 68 | **100% MATCH** |
| `inventory_items` | 25 | 25 | **100% MATCH** |
| `trousseau_boards` | 8 | 8 | **100% MATCH** |
| `trousseau_ceremonies` | 16 | 16 | **100% MATCH** |
| `trousseau_items` | 8 | 8 | **100% MATCH** |
| `trousseau_votes` | 14 | 14 | **100% MATCH** |
| `customer_events` | 63 | 63 | **100% MATCH** |
| `carts` | 6 | 6 | **100% MATCH** |
| `whatsapp_contacts` | 4 | 4 | **100% MATCH** |

#### 4.4 Automated Application Boot & Health Check Against Restored DB
- Automated test suite `DisasterRecoveryDatabaseRestoreDrillTest` ran against `sareekart_recovery_drill_db` with `ddl-auto: validate`.
- **Results:**
  - `DR Drill 1: Schema validation succeeds with ddl-auto: validate` — **PASS**
  - `DR Drill 2: Catalog and inventory entities restored with exact counts` — **PASS**
  - `DR Drill 3: Customer and order records restored with exact counts` — **PASS**
  - `DR Drill 4: Trousseau collaborative state restored with exact counts` — **PASS**
  - `DR Drill 5: Application health and catalog endpoints respond against restored database` — **PASS**
- Total test elapsed time: **4.58 seconds**; HTTP `/actuator/health` returned 200 OK with `UP`.
- Cleanup: `sareekart_recovery_drill_db` dropped; `sareekart_db` confirmed 100% untouched.

---

### 5. Flyway Rollback Strategy (Stage 4)

- **Forward-Only Migration Invariant:** Production Flyway migrations are strictly forward-applied. Historical migrations (`V17` through `V32`) in `db/migration/` are immutable.
- **Rollback Decision Matrix:**
  1. *Minor Bug in Application Code:* Roll back application version only (re-deploy previous Docker image). Existing schema remains compatible.
  2. *Schema Reversal Required:* Author a forward-fix migration (e.g. `V33__revert_...sql`) removing or adjusting columns/indexes without downtime.
  3. *Catastrophic Corruption / Destructive DDL:* Provision clean database target, restore verified logical backup prior to corrupting migration, and repoint application.
- **Migration Audit:** Verified migrations V17–V32 are present, with zero duplicate version numbers:
  - `V29__create_collaborative_trousseau_tables.sql` — Verified
  - `V30__production_performance_indexes.sql` — Verified
  - `V31__add_razorpay_order_id_index.sql` — Verified
  - `V32__whatsapp_production_readiness.sql` — Verified

---

### 6. Application Rollback Drill (Stage 5)

- **Git Commit Baseline:**
  - Current HEAD: `78373ec` (Phase 10 WhatsApp AI Assistant)
  - Previous Known-Good Release: `af65830` (Phase 9 AI Stylist)
- **Backward Compatibility Assessment:**
  - All migrations added between `af65830` and `78373ec` (V29–V32) introduced new tables (`trousseau_*`, `return_requests`), composite indexes (`idx_orders_razorpay_order_id`), and columns with default values (`opted_in TINYINT(1) DEFAULT 1`).
  - Because JPA entities in `af65830` map only their specified tables/columns, extra database tables and indexes in MySQL do NOT break earlier application builds.
  - Rollback to `af65830` can be performed safely without requiring database schema reversal.

---

### 7. Auxiliary Subsystem Recovery

#### 7.1 Neo4j Knowledge Graph (Stage 6)
- **Decoupling Invariant:** Neo4j is non-blocking. `Neo4jConfig.java` logs a warning if port 7687 is unreachable and returns a null driver without failing application startup.
- **Reconstruction:** `Neo4jGraphServiceImpl.syncFullCatalogFromMySql()` regenerates all catalog nodes and relationships from MySQL in $< 0.5\text{ s}$.
- **Commerce Continuity:** In drill tests with Neo4j offline, product browsing, cart, checkout, and health probes operated with 100% success.

#### 7.2 Object & Image Storage (Stage 7)
- **Storage:** Uploaded return photos and custom blouse images reside in `/uploads/`.
- **Resilience:** If files are missing from disk or CDN, frontend `onError` handlers render luxury fallback drapes. APIs and checkout transactions never crash due to missing image files.

#### 7.3 Secrets & Configuration (Stage 8)
- Externalized environment variables (`SPRING_DATASOURCE_*`, `JWT_SECRET`, `RAZORPAY_KEY_*`, `WHATSAPP_*`, `OPENAI_API_KEY`, `NEO4J_*`) are managed via deployment configurations.
- In `prod` profile, missing required credentials trigger immediate fail-fast exceptions on boot, preventing corrupted or unauthenticated deployments.

---

### 8. Failure Simulation Matrix (Stage 9)

| Failure Scenario | Simulated Condition | Observed Behavior | Customer Impact | Recovery Procedure |
|---|---|---|---|---|
| **A. MySQL Unavailable** | Port 3306 blocked | Actuator reports DOWN; DB connection pool attempts reconnect | HTTP 503 / Friendly error page | Restart MySQL; pool reconnects automatically |
| **B. Neo4j Unavailable** | Port 7687 offline | `Neo4jConfig` logs WARN; driver returns null | Zero impact; MySQL fallback recommendations active | Start Neo4j; run `syncFullCatalogFromMySql()` |
| **C. Storage Unavailable** | `/uploads/` unmounted | Upload endpoints return HTTP 500 error | Photo uploads fail; browsing & checkout unaffected | Remount `/uploads/` volume with read/write perms |
| **D. WhatsApp Unavailable** | Meta API timeout | Webhook dispatch queues error log | Checkout unaffected; order email/SMS fallback | Resolve API token/connectivity; retry queued messages |
| **E. Application Restart** | Process termination | Graceful shutdown allows in-flight HTTP up to 30s | Brief 1-3s blip if single container | Stateless restart resumes all operations instantly |
| **F. Disk Approaching 30%** | Free space $< 30\%$ | `manage.sh backup` aborts with clear error; rotation prunes older dumps | Zero commerce impact | Prune old log files or increase volume size |
| **G. Invalid Config** | Missing mandatory JWT secret | Spring Boot rejects context startup | Bad container never receives traffic | Restore valid environment variable and restart |
| **H. Corrupted Backup** | Truncated SQL dump | `manage.sh verify` fails checksum/table checks | Blocks invalid restore attempt | Use preceding verified backup |
| **I. Failed Restore** | Invalid syntax in dump | Restore command fails with non-zero exit code | Target DB uncorrupted; live DB untouched | Correct dump error or target clean schema |

---

### 9. Recovery Metrics & Objectives (Stage 10)

| Metric | Target Policy | Measured Drill Performance |
|---|---|:---:|
| **Backup Creation Time** | Standard operational window | **0.8 seconds** |
| **Database Restoration Time** | Bounded recovery | **1.0 second** |
| **Application Boot & Probe Time** | Healthcheck interval | **4.5 seconds** |
| **Total Measured Recovery Time (RTO)** | *Business target not yet defined* | **~6.3 seconds** |
| **Data Loss Window (RPO)** | *Business target not yet defined* | **$< 24\text{ hours}$** |

---

### 10. Automated DR Tooling Verification (Stage 11)

Execution of `./manage.sh dr_check` validated all diagnostic criteria:
```text
=============================================
     SareeKart Disaster Recovery Diagnostic  
=============================================
[1/6] MySQL Database Service: ✔ OPERATIONAL (:3306)
[2/6] Primary Schema Integrity: ✔ HEALTHY (37 tables found)
[3/6] Backup Recency & Availability: ✔ FRESH (0 hours old, 160K)
[4/6] Storage Headroom (>= 30% free): ✔ SUFFICIENT (49% available)
[5/6] Knowledge Graph Degradation Defense: ✔ DEGRADED SAFE (Offline, MySQL deterministic fallback active)
[6/6] Application Artifact Readiness: ✔ READY (Maven wrapper verified)
---------------------------------------------
✔ DR Readiness Check: ALL SYSTEMS READY FOR RECOVERY
=============================================
```

---

### 11. Security Gate Compliance

- [x] No production database destroyed (`sareekart_db` verified untouched).
- [x] No production data modified during destructive testing.
- [x] No secrets exposed in Git, logs, backups, or documentation.
- [x] Restore requires explicit target parameter and affirmative confirmation for production.
- [x] Backup files protected with SHA-256 checksums and automated rotation.
- [x] Flyway migration history intact (V17–V32).
- [x] Production configuration remains externalized.

---

### 12. Final Status

**PHASE 13 — STAGE 7 PASS**
