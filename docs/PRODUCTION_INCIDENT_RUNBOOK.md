# SareeKart Production Observability & Incident Runbook

> **Scope**: Zero-Cost Production Architecture (Vercel Edge CDN + Render/Koyeb Containerized Spring Boot 3 + TiDB Cloud Serverless MySQL)  
> **Authoritative Domain**: `https://sareekart.com`  
> **Backend Port**: `8081`  
> **Revision**: Phase 14 Stage 8 (Production Observability, Reliability & Incident Readiness)

---

## 1. Fast Diagnostic Matrix (The 12 Operational Questions)

| Question | Observability Tool / Metric | Healthy Baseline | Incident Threshold |
|---|---|---|---|
| **1. Is the application alive?** | `GET /actuator/health/liveness` | HTTP 200 `{"status":"UP"}` | HTTP 503 / Connection Refused |
| **2. Is the application ready?** | `GET /actuator/health/readiness` | HTTP 200 `{"status":"UP"}` | HTTP 503 (DB down or initializing) |
| **3. Is the database healthy?** | `GET /actuator/health` | Status `UP`, Hikari active $\le 8$ | Status `DOWN`, Hikari timeout $>20\text{s}$ |
| **4. Is the service degraded?** | App logs: `Neo4jGraphServiceImpl` | Safe fallback hydrations logged | Unhandled exceptions or 500 spikes |
| **5. Are dependencies failing?** | App logs: `PaymentServiceImpl` / `WhatsApp` | Classified `*_ERROR` codes | Unhandled runtime errors |
| **6. Are error rates increasing?** | Filter logs: `[HTTP] status=5xx` | $< 0.1\%$ error rate | Spike in 500s or 502/503 |
| **7. Are payment failures distinct?** | `PAYMENT_GATEWAY_ERROR` vs verification | Error codes logged with `requestId` | Missing correlation ID or unhandled error |
| **8. Are WhatsApp issues distinct?** | Signature 401 vs Opt-Out SUPPRESSED | Constant-time HMAC rejection | Unsolicited sends to opted-out contacts |
| **9. Are AI failures isolated?** | `StylistGroundingService` fallback | Curated fallback sarees served | Blank UI or blocking API timeouts |
| **10. Can incident be debugged safely?** | `X-Request-ID` across frontend/backend | Request ID present, zero secrets/PII | Secrets or card details in logs |
| **11. Can service recover safely?** | Automatic container restart / DB retry | Reconnects without manual reboot | Stuck threads or pool exhaustion |
| **12. Is DR backup verified?** | `scripts/verify_backup_integrity.sh` | 100% SHA-256 match, 7-day retention | Checksum mismatch or empty dump |

---

## 2. Incident Scenarios & Standard Operating Procedures (SOP)

### SOP 01: Storefront / Edge CDN Unavailable (`ERR_CONNECTION_TIMED_OUT` or 404)
1. **Symptoms**: Browser cannot load `https://sareekart.com`.
2. **Immediate Diagnostics**:
   ```bash
   dig +short sareekart.com
   # Expected: 76.76.21.21 (Vercel Anycast IP)
   curl -I https://sareekart.com
   ```
3. **Root Causes**:
   - DNS record at registrar unconfigured or expired.
   - Vercel domain verification pending.
4. **Remediation**:
   - Check registrar DNS matches [`docs/sareekart_dns_configuration.md`](sareekart_dns_configuration.md).
   - In Vercel Dashboard $\to$ Settings $\to$ Domains, verify SSL cert status is "Valid".

---

### SOP 02: Backend Unavailable / Cold-Start Awakening Delay
1. **Symptoms**: API requests time out ($>30\text{s}$) or return 502/504 Bad Gateway; frontend displays "Awakening our boutique atelier...".
2. **Immediate Diagnostics**:
   ```bash
   curl -I https://sareekart-backend.onrender.com/actuator/health/liveness
   ```
3. **Root Causes**:
   - Free-tier container went to sleep after 15 minutes of inactivity (normal on zero-cost tier).
   - Container OOMKilled exceeding 512 MB RAM ceiling.
4. **Remediation**:
   - Wait 35–45 seconds for automatic container boot.
   - Verify GitHub Actions keepalive cron ([`.github/workflows/keepalive.yml`](../.github/workflows/keepalive.yml)) is active (pings `/actuator/health` every 12 mins).
   - If container crashed on memory, check `JAVA_TOOL_OPTIONS`:
     `JAVA_TOOL_OPTIONS="-Xmx384m -Xms128m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"`

---

### SOP 03: Database Outage / Connection Pool Exhaustion
1. **Symptoms**: `/actuator/health/readiness` returns HTTP 503 `{"status":"DOWN"}`; logs show `HikariPool - Connection is not available`.
2. **Immediate Diagnostics**:
   ```bash
   curl -s http://localhost:8081/actuator/health/readiness
   # Search logs for connection timeouts:
   grep -rn "HikariPool" app.log
   ```
3. **Remediation**:
   - Check TiDB Cloud Serverless dashboard cluster status.
   - Verify TLS configuration: `useSSL=true&allowPublicKeyRetrieval=true`.
   - Ensure `HIKARI_MAX_POOL_SIZE` is bounded to `8` in production (`application-prod.yaml`).
   - If connection was stuck, restart backend container to release pooled sockets cleanly.

---

### SOP 04: Slow API Requests / Latency Spike
1. **Symptoms**: API requests take $>2000\text{ms}$.
2. **Immediate Diagnostics**:
   ```bash
   # Filter structured logs for slow operations:
   awk '$NF > 2000' app.log | grep "\[HTTP\]"
   ```
3. **Remediation**:
   - Identify offending URI (e.g. `/api/products` vs `/api/ai/stylist`).
   - If `/api/products`: Ensure query uses index on `category_id`, `active`, or `price`.
   - If `/api/ai/*`: Verify AI fallback is engaging when upstream LLM response exceeds 5 seconds.

---

### SOP 05: Payment Failures (Razorpay Gateway vs Signature Mismatch)
1. **Symptoms**: Customer reports checkout error during payment step.
2. **Immediate Diagnostics**:
   - Find request ID from customer error report or Axios telemetry:
     ```bash
     grep "req-xxxxx" app.log
     ```
   - Check error classification:
     - `PAYMENT_CONFIG_ERROR`: `RAZORPAY_KEY_ID` or `RAZORPAY_KEY_SECRET` missing in environment.
     - `PAYMENT_GATEWAY_ERROR`: Razorpay API unreachable or timed out.
     - `PAYMENT_VERIFICATION_ERROR`: HMAC signature mismatch on callback.
3. **Remediation**:
   - Never retry verification if `PAYMENT_VERIFICATION_ERROR` occurred (signature tampering protection).
   - Check Razorpay status page for upstream outages (`https://status.razorpay.com`).

---

### SOP 06: WhatsApp Webhook Signature Rejection
1. **Symptoms**: Meta webhook dashboard shows failed deliveries (HTTP 401).
2. **Immediate Diagnostics**:
   ```bash
   grep "WhatsAppWebhookSignatureValidator" app.log
   ```
3. **Remediation**:
   - Verify `WHATSAPP_APP_SECRET` matches the App Secret in Meta App Dashboard $\to$ App settings $\to$ Basic.
   - Confirm Meta is sending `X-Hub-Signature-256` header in format `sha256=<hex>`.
   - Verify verification challenge (`GET /api/webhook/whatsapp`):
     ```bash
     curl "http://localhost:8081/api/webhook/whatsapp?hub.mode=subscribe&hub.challenge=test1234&hub.verify_token=sareekart-verify-token"
     # Expected: test1234
     ```

---

### SOP 07: Meta / Instagram Commerce Catalog CSV Issues
1. **Symptoms**: Meta Commerce Manager reports CSV ingestion errors or rejected items.
2. **Immediate Diagnostics**:
   ```bash
   curl -s http://localhost:8081/api/meta/catalog.csv | head -n 5
   ```
3. **Remediation**:
   - Ensure header matches exact 12 fields:
     `id,title,description,availability,condition,price,link,image_link,brand,google_product_category,product_type,additional_image_link`
   - Verify price format contains currency: `18500.00 INR`.
   - Verify links start with `https://sareekart.com/products/` (no ports or localhost).
   - Verify RFC 4180 escaping wraps fields with commas or quotes.

---

### SOP 08: Security Incident / Brute-Force Authentication Attempt
1. **Symptoms**: Surge in `AUTHENTICATION_ERROR` logs from single IP or user-agent.
2. **Immediate Diagnostics**:
   ```bash
   grep "AUTHENTICATION_ERROR" app.log | awk '{print $1, $2, $8}' | sort | uniq -c | sort -nr | head -n 20
   ```
3. **Remediation**:
   - Verify `BadCredentialsException` returns generic message: `"Incorrect email or password. Please try again."` (never reveals whether email exists).
   - If malicious IP identified, add Cloudflare / Edge CDN IP firewall rule.

---

### SOP 09: Database Backup & Non-Destructive Integrity Verification
1. **Running an On-Demand Backup**:
   ```bash
   ./scripts/backup_db.sh
   # Automatically saves to backups/sareekart_db_<timestamp>.sql and updates sareekart_db_latest.sql
   ```
2. **Verifying All Existing Backups**:
   ```bash
   ./scripts/verify_backup_integrity.sh
   # Verifies SHA-256 integrity and DDL/DML structure without touching production
   ```
3. **Safe Dry-Run / Test Restore (Local Only — NEVER on Production)**:
   ```bash
   # In local development test DB only:
   mysql -h 127.0.0.1 -P 3307 -u root -p sareekart_test < backups/sareekart_db_latest.sql
   ```

---

## 3. Log Correlation Guide (`X-Request-ID`)

Every incoming HTTP request is assigned a unique correlation ID:
- **Inbound Header**: `X-Request-ID: req-<timestamp>-<hash>`
- **Response Header**: `X-Request-ID: <id>`
- **SLF4J MDC**: `requestId=<id>`
- **Structured Log Entry**:
  ```text
  2026-09-23 19:04:02.123 [HTTP] method=GET path=/api/products status=200 durationMs=14 requestId=req-mf490x-7k1a9d
  ```

### How to trace an incident:
1. Obtain the `requestId` from the user's error screen or frontend network inspector.
2. Search the server logs:
   ```bash
   grep "req-mf490x-7k1a9d" /var/log/sareekart/app.log
   ```
3. All lines emitted during the processing of that exact request will appear chronologically.

---

## 4. Redaction Checklist — What Must NEVER Appear in Logs or Runbook Tickets

When copying logs into GitHub issues, Slack, or incident reports:
- [ ] **NO JWT Tokens**: Never copy `Bearer eyJhbGci...`
- [ ] **NO Database Passwords**: Never paste JDBC connection strings with passwords.
- [ ] **NO Payment Secrets**: Never paste `RAZORPAY_KEY_SECRET`.
- [ ] **NO Webhook Secrets**: Never paste `WHATSAPP_APP_SECRET`.
- [ ] **NO Customer PII**: Redact customer email, physical address, and phone number.
- [ ] **NO Raw Credit Cards**: Never log card PAN, CVV, or expiry dates (PCI-DSS compliance).

---

## 5. Rollback Procedures (SOP 10: Emergency Rollback Execution)

### 5.1 Frontend Rollback (Vercel Instant Edge Rollback)
1. **Trigger Condition**: Fatal JavaScript syntax error, broken client checkout, or asset rendering regressions.
2. **Procedure**:
   - In the **Vercel Dashboard**, navigate to **Deployments**.
   - Locate the previous successful production deployment.
   - Click the three dots menu `(...)` and select **Promote to Production** (Instant Rollback completes in $< 5\text{ seconds}$).
   - Edge CDN invalidates stale chunks and serves the previous immutable assets globally.

### 5.2 Backend Container Rollback (Render Web Service)
1. **Trigger Condition**: Severe backend regression, container crash loop, or unhandled 500 error spike.
2. **Procedure**:
   - In the **Render Dashboard**, select `sareekart-backend`.
   - Navigate to **Events / Deploys**.
   - Select the previous stable commit SHA (e.g. Stage 10 baseline `a8d73d124d2cbcf15a58af8e080a5671d185ecbe`).
   - Click **Rollback to this deploy**.
   - Container rebuilds and passes `/actuator/health/readiness` before traffic switches over.

### 5.3 Database Disaster Recovery & Rollback
1. **Trigger Condition**: Irrecoverable data corruption or catastrophic migration failure.
2. **Procedure**:
   - Confirm latest backup SHA-256 integrity using `./scripts/verify_backup_integrity.sh`.
   - Restore database from latest point-in-time backup: `mysql -h <host> -u <user> -p <db> < backups/sareekart_db_latest.sql`.
   - Verify schema and table counts using `./scripts/verify_tidb_schema.sh`.
   - Assert all 37 tables and 368 columns match baseline parity.
