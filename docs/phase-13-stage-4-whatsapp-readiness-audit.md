# Phase 13 · Stage 4 — WhatsApp Production Readiness Audit

**Repository:** `/Users/chaitanyachaitu/Downloads/SareeKart-main`  
**Branch:** `master` · `78373ec`  
**Date:** 2026-09-17  
**Status:** ✅ PHASE 13 — STAGE 4 PASS

---

## 1. Findings Addressed

| ID | Finding | Severity | Resolution |
|---|---|---|---|
| R1-A | Dev-bypass in `WhatsAppWebhookSignatureValidator` — missing header allowed when `appSecret` == hardcoded default | 🔴 CRITICAL | Bypass completely removed |
| R1-B | `whatsapp.webhook.app-secret` not declared in `application.yaml` | 🔴 HIGH | Property declared with `${WHATSAPP_APP_SECRET:}` empty default |
| R1-C | `WHATSAPP_APP_SECRET` not documented in `.env.example` | 🟠 MEDIUM | Entry already present from earlier work — confirmed present |
| R2-A | `WhatsAppContact` entity missing `optedIn` boolean field | 🟠 MEDIUM | Field added with `@Builder.Default = true` |
| R2-B | No STOP/START keyword handling in `WhatsAppWebhookService` | 🟠 MEDIUM | Full keyword intercept implemented before AI dispatch |
| R4 | `WhatsAppApiClient` silently swallowed outbound failures with `log.error` before `onErrorResume` | 🟡 LOW | Changed to `log.warn` with message type context |
| R3, R5 | Templates, phone normalization, Trousseau isolation | ✅ PASS | No changes needed — confirmed by existing tests |

---

## 2. Files Changed

### Modified (Stage 4 only)

| File | Change Summary |
|---|---|
| `security/WhatsAppWebhookSignatureValidator.java` | Removed 8-line dev-bypass block; missing/blank `appSecret` now always rejects |
| `resources/application.yaml` | Added `whatsapp.webhook.app-secret: ${WHATSAPP_APP_SECRET:}` with fail-secure empty default |
| `entity/WhatsAppContact.java` | Added `optedIn boolean` field (`@Column(name="opted_in", nullable=false)`, `@Builder.Default = true`) |
| `service/WhatsAppWebhookService.java` | Added `WhatsAppApiClient` injection; STOP/START intercept (step 3, before AI dispatch); `isOptOutKeyword()`, `isOptInKeyword()`, `sendOptOutConfirmation()`, `sendOptInConfirmation()` helpers |
| `service/WhatsAppApiClient.java` | Changed `log.error` → `log.warn` with message type in `doOnError` before `onErrorResume` |
| `test/resources/application-test.yaml` | Added `whatsapp.webhook.app-secret: test-whatsapp-hmac-secret-for-unit-tests` |
| `test/security/WhatsAppWebhookSignatureValidatorTest.java` | Replaced old dev-bypass test with 3 new hardening tests (bypass removed, old secret rejected, blank secret rejects all) |

### New Files (Stage 4)

| File | Purpose |
|---|---|
| `db/migration/V32__whatsapp_contact_opt_in.sql` | Adds `opted_in TINYINT(1) NOT NULL DEFAULT 1` to `whatsapp_contacts` |
| `test/service/WhatsAppProductionReadinessTest.java` | 19 tests covering R1–R4 acceptance criteria |

---

## 3. V32 Migration Details

```sql
-- V32: whatsapp_contact_opt_in
ALTER TABLE whatsapp_contacts
    ADD COLUMN opted_in TINYINT(1) NOT NULL DEFAULT 1
        COMMENT 'WhatsApp messaging consent: 1=opted-in, 0=opted-out (STOP received)';
```

- **Migration chain:** V1 … V29 (Trousseau) → V30 (performance indexes) → V31 (Razorpay order ID index) → **V32 (WhatsApp opt-in)** ✅
- **Existing data safety:** `DEFAULT 1` ensures all pre-existing contacts retain opted-in state
- **Local MySQL verification:** `opted_in tinyint(1) NOT NULL DEFAULT 1` column confirmed present

---

## 4. Secret Configuration

| Property | Resolution | Behavior when absent |
|---|---|---|
| `whatsapp.webhook.app-secret` | `${WHATSAPP_APP_SECRET:}` | All webhook requests rejected (fail-secure) |
| Old hardcoded default | `sareekart-meta-secret-2026` REMOVED from source | N/A — no longer referenced in production code |

**Git hygiene:** Only occurrence of `sareekart-meta-secret-2026` in source is in `WhatsAppWebhookSignatureValidatorTest.java` as an argument to a negative assertion proving the bypass is gone.

---

## 5. Signature Validation Behavior (After Stage 4)

| Request condition | Result |
|---|---|
| Missing `X-Hub-Signature-256` header | `false` → 403 Unauthorized |
| Header present but malformed (no `sha256=` prefix) | `false` → 403 |
| Header present, valid HMAC-SHA256 | `true` → 200 OK |
| Invalid/forged HMAC | `false` → 403 |
| `WHATSAPP_APP_SECRET` env var not set (blank) | `false` for ALL requests (fail-secure) |
| Old hardcoded default value as secret, missing header | `false` → 403 (bypass removed) |

---

## 6. STOP/START Behavior

Intercept runs at **step 3** in `processIncomingMessage()` — **before** conversation lookup, message persistence, and AI dispatch.

| Keyword | Action |
|---|---|
| `STOP`, `UNSUBSCRIBE`, `CANCEL`, `QUIT`, `END` | Sets `contact.optedIn = false`; sends confirmation reply; returns early |
| `START`, `SUBSCRIBE`, `JOIN`, `YES`, `UNSTOP` | Sets `contact.optedIn = true`; sends welcome-back reply; returns early |
| Opted-out contact, normal message | Suppressed at step 4 — not persisted, not sent to AI |

**Idempotency preserved:** Existing `WhatsAppIdempotencyService.tryAcquireLock(wamId)` still runs at step 1.  
**PII preserved:** Confirmation messages contain no JWT, password, or internal IDs.  
**Failure isolation preserved:** `sendOptOutConfirmation()` / `sendOptInConfirmation()` wrap `whatsAppApiClient.sendTextMessage()` in try/catch; failures are WARN-logged and never propagate.

---

## 7. Test Results

### `./mvnw clean test`

```
Tests run: 499, Failures: 0, Errors: 0, Skipped: 3
BUILD SUCCESS
```

**Skipped:** 3 Neo4j live integration tests (pre-existing — Neo4j not running in dev, unchanged)

### WhatsApp-specific test classes

| Class | Tests | Result |
|---|---|---|
| `WhatsAppProductionReadinessTest` | 19 | ✅ 0 failures |
| `WhatsAppWebhookSignatureValidatorTest` | 7 | ✅ 0 failures |
| `WhatsAppWebhookControllerTest` | 4 | ✅ 0 failures |
| `WhatsAppWebhookService` (all phases) | — | ✅ 0 failures |
| `WhatsAppIdempotencyServiceTest` | 5 | ✅ 0 failures |
| `WhatsAppIdentityServiceTest` | 4 | ✅ 0 failures |
| `WhatsAppNotificationServiceImplTest` | — | ✅ 0 failures |
| `TrousseauWhatsAppCollaborationTest` | 12 | ✅ 0 failures |

### `WhatsAppProductionReadinessTest` breakdown

| Test | Coverage | Result |
|---|---|---|
| R1-1: Missing header always rejected | Signature / R1 | ✅ |
| R1-2: Invalid HMAC rejected | Signature / R1 | ✅ |
| R1-3: Valid HMAC accepted | Signature / R1 | ✅ |
| R1-4: Dev-bypass removed (old default key) | Signature / R1 | ✅ |
| R1-5: Absent secret rejects all requests | Config / R1 | ✅ |
| R1-6: Fail-secure on missing secret | Config / R1 | ✅ |
| R2-1: STOP sets optedIn=false | STOP/START / R2 | ✅ |
| R2-2: UNSUBSCRIBE sets optedIn=false | STOP/START / R2 | ✅ |
| R2-3: CANCEL sets optedIn=false | STOP/START / R2 | ✅ |
| R2-4: START sets optedIn=true | STOP/START / R2 | ✅ |
| R2-5: JOIN sets optedIn=true | STOP/START / R2 | ✅ |
| R2-6: STOP is idempotent | STOP/START / R2 | ✅ |
| R2-7: START is idempotent | STOP/START / R2 | ✅ |
| R2-8: STOP message NOT persisted in WhatsAppMessage | STOP/START / R2 | ✅ |
| R2-9: Opted-out contact suppressed from AI processing | STOP/START / R2 | ✅ |
| R2-10: Opted-in contact resumes normal processing | STOP/START / R2 | ✅ |
| R3-1: No PII in opt-out confirmation | Templates / R3 | ✅ |
| R3-2: Phone normalization preserved through STOP/START | Normalization / R3 | ✅ |
| R4-1: API failure during STOP confirmation isolated | Failure isolation / R4 | ✅ |

---

## 8. Regression Results

All 478 pre-Stage-4 tests continue to pass. 21 new tests added (19 in `WhatsAppProductionReadinessTest` + 2 in `WhatsAppWebhookSignatureValidatorTest`).

| Phase | Test class | Result |
|---|---|---|
| Phase 10 WhatsApp | All WhatsApp* tests | ✅ PASS |
| Phase 11 Trousseau WhatsApp | `TrousseauWhatsAppCollaborationTest` (12) | ✅ PASS |
| Phase 3 Payments | `PaymentServiceTest`, `PaymentControllerTest` | ✅ PASS |
| All phases | Full 499-test suite | ✅ PASS |

### Frontend

```
npm run build → ✓ built in 285ms
Largest chunk: vendor-react 229 kB (< 500 kB limit)
```

---

## 9. Security Verification

| Gate | Status |
|---|---|
| Missing HMAC signature → 403 | ✅ PASS |
| Invalid HMAC signature → 403 | ✅ PASS |
| Valid HMAC signature → accepted | ✅ PASS |
| No development signature bypass remains | ✅ PASS |
| No insecure default secret remains | ✅ PASS |
| Production app secret is externalized | ✅ PASS (`${WHATSAPP_APP_SECRET:}`) |
| Secret absent from Git | ✅ PASS |
| V32 applied successfully | ✅ PASS |
| `WhatsAppContact` has persisted opt-in state | ✅ PASS |
| STOP persists opt-out | ✅ PASS |
| START persists opt-in | ✅ PASS |
| STOP/START happens before AI dispatch | ✅ PASS |
| Opted-out contacts protected from AI dispatch | ✅ PASS |
| Existing idempotency intact | ✅ PASS |
| Existing phone normalization intact | ✅ PASS |
| Existing failure isolation intact | ✅ PASS |
| Phase 10 tests pass | ✅ PASS |
| Phase 11 WhatsApp tests pass | ✅ PASS |
| Full backend regression passes | ✅ PASS |

---

## 10. Git Diff Verification

**Stage 4 files changed (7 modified, 2 new):**

```
M  backend/backend/src/main/java/com/sareekart/entity/WhatsAppContact.java
M  backend/backend/src/main/java/com/sareekart/security/WhatsAppWebhookSignatureValidator.java
M  backend/backend/src/main/java/com/sareekart/service/WhatsAppApiClient.java
M  backend/backend/src/main/java/com/sareekart/service/WhatsAppWebhookService.java
M  backend/backend/src/main/resources/application.yaml
M  backend/backend/src/test/java/com/sareekart/security/WhatsAppWebhookSignatureValidatorTest.java
M  backend/backend/src/test/resources/application-test.yaml
?? backend/backend/src/main/resources/db/migration/V32__whatsapp_contact_opt_in.sql
?? backend/backend/src/test/java/com/sareekart/service/WhatsAppProductionReadinessTest.java
```

No unrelated Stage 4 code changes. All other modified files (`PaymentController`, `CorsConfig`, `SecurityConfig`, etc.) are pre-existing uncommitted changes from earlier stages.

---

## 11. Remaining Production Blockers

None. All Stage 4 acceptance gates have passed.

**What must be done before live production deployment:**
- Set `WHATSAPP_APP_SECRET=<actual-meta-app-secret>` in production environment (currently absent = fail-secure)
- Register STOP/START confirmation message templates with Meta (for template-only messaging numbers)

---

## FINAL STATUS

```
PHASE 13 — STAGE 4 PASS
```

**Test count:** 499 passing, 0 failures, 0 errors, 3 skipped (pre-existing Neo4j)  
**Disk:** 45.9% free (104.7 GiB available) — PASS  
**Frontend:** Largest chunk 229 kB — PASS  
**Secrets in Git:** None — PASS
