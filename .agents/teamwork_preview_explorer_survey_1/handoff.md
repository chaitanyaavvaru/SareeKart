# Handoff Report: Requirement R1 Survey (WhatsApp Webhook Security & Signature Hardening)

**Date**: 2026-09-17  
**Agent**: Survey Explorer 1  
**Working Directory**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_1`  
**Report Artifact**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_1/survey_report.md`  

---

## 1. Observation

1. **Webhook Controller**:
   - Location: `backend/backend/src/main/java/com/sareekart/controller/WhatsAppWebhookController.java`
   - Line 26: `@GetMapping` handles `hub.mode`, `hub.verify_token`, and `hub.challenge`.
   - Line 34: `if ("subscribe".equals(mode) && verifyToken.equals(token)) { return ResponseEntity.ok(challenge); } return ResponseEntity.status(HttpStatus.FORBIDDEN).build();`
   - Line 43: `@PostMapping` takes `@RequestHeader(value = "X-Hub-Signature-256", required = false) String signature` and `@RequestBody(required = false) byte[] payloadBytes`.
   - Lines 48–55:
     ```java
     if (payloadBytes == null || payloadBytes.length == 0) {
         return ResponseEntity.ok().build();
     }

     if (!signatureValidator.isValid(payloadBytes, signature)) {
         log.warn("Unauthorized WhatsApp Webhook: Invalid HMAC signature");
         return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
     }
     ```
   - Line 60: Invokes `webhookService.processWebhook(payload)`. Returns HTTP 200 OK on success or catch block.

2. **Signature Validator & Dev Bypass**:
   - Location: `backend/backend/src/main/java/com/sareekart/security/WhatsAppWebhookSignatureValidator.java`
   - Line 23: `@Value("${whatsapp.webhook.app-secret:sareekart-meta-secret-2026}") private String appSecret;`
   - Lines 34–42:
     ```java
     if (signatureHeader == null || !signatureHeader.startsWith(PREFIX)) {
         // Allow dev/test requests where signature header is absent if secret is default test key
         if ("sareekart-meta-secret-2026".equals(appSecret) || appSecret == null || appSecret.isBlank()) {
             log.debug("Signature header missing, allowing in development mode");
             return true;
         }
         log.warn("Missing or invalid X-Hub-Signature-256 header format: {}", signatureHeader);
         return false;
     }
     ```
   - Lines 50–65: Uses `Mac.getInstance("HmacSHA256")`, generates lowercase hex, and verifies using `MessageDigest.isEqual(...)`.

3. **Application Properties & Environment Variables**:
   - Location: `backend/backend/src/main/resources/application.yaml`
   - Lines 71–73:
     ```yaml
       webhook:
         verify-token: ${WHATSAPP_WEBHOOK_VERIFY_TOKEN:sareekart-verify-token}
     ```
   - `whatsapp.webhook.app-secret` is absent from `application.yaml`.
   - `backend/backend/src/main/resources/application-prod.yaml` contains no `whatsapp` section.

4. **Spring Security Configuration**:
   - Location: `backend/backend/src/main/java/com/sareekart/config/SecurityConfig.java`
   - Line 112:
     ```java
     .requestMatchers("/api/webhook/whatsapp/**", "/api/payments/webhook", "/ws-sareekart/**").permitAll()
     ```

5. **Idempotency Architecture**:
   - Location: `backend/backend/src/main/java/com/sareekart/service/impl/WhatsAppIdempotencyServiceImpl.java`
   - Line 24: `private final Map<String, Long> activeLockMap = new ConcurrentHashMap<>();`
   - Lines 36–48: `putIfAbsent` with 15-minute TTL, followed by `messageRepository.findByWamId(wamId)`.
   - Location: `backend/backend/src/main/java/com/sareekart/service/WhatsAppWebhookService.java`
   - Line 70: `if (wamId != null && !idempotencyService.tryAcquireLock(wamId)) { return; }`
   - Redis is not used or present in `pom.xml`.

6. **Existing Tests & Missing Deliverable**:
   - Existing tests:
     - `WhatsAppWebhookControllerTest.java` (5 unit tests, Mockito only, no Spring context)
     - `WhatsAppWebhookSignatureValidatorTest.java` (5 unit tests)
     - `WhatsAppIdempotencyServiceTest.java` (5 unit tests)
   - Executed `./mvnw test -Dtest=WhatsAppWebhookControllerTest,WhatsAppWebhookSignatureValidatorTest,WhatsAppIdempotencyServiceTest`: 15 tests executed, 0 failures.
   - `src/test/java/com/sareekart/WhatsAppProductionReadinessTest.java` does NOT exist in the repository.

---

## 2. Logic Chain

1. From Observation 1 (`WhatsAppWebhookController.java:34`), `GET` verification validates `hub.verify_token` against `verifyToken` and returns `hub.challenge` or HTTP 403, satisfying the GET verification requirement of R1.
2. From Observation 3 (`application.yaml:71–73`), `verify-token` is mapped to `${WHATSAPP_WEBHOOK_VERIFY_TOKEN:sareekart-verify-token}`, but `app-secret` is omitted from `application.yaml`.
3. From Observation 2 (`WhatsAppWebhookSignatureValidator.java:23`), the validator expects `@Value("${whatsapp.webhook.app-secret:sareekart-meta-secret-2026}")`. Because `application.yaml` does not map `app-secret: ${WHATSAPP_APP_SECRET:...}`, setting `WHATSAPP_APP_SECRET` in environment variables will not bind to the field.
4. From Observation 2 (`WhatsAppWebhookSignatureValidator.java:36–39`), because `appSecret` defaults to `"sareekart-meta-secret-2026"`, any request missing `X-Hub-Signature-256` returns `true`. Consequently, in any environment where `WHATSAPP_APP_SECRET` is set as an env var, unsigned requests will be accepted.
5. From Observation 4 (`SecurityConfig.java:112`), `/api/webhook/whatsapp/**` permits requests, but R1 requires `/api/webhook/**` to prevent URL matching edge cases without trailing slashes.
6. From Observation 5 (`WhatsAppIdempotencyServiceImpl.java` and `WhatsAppWebhookService.java`), deduplication combines in-memory atomic `putIfAbsent` with relational DB check on `wam_id`, dropping duplicate requests before message parsing, persistence, WebSocket push, or AI commerce execution.
7. From Observation 6, `WhatsAppProductionReadinessTest.java` required by the Phase 13 Stage 4 Acceptance Criteria is not yet authored, and existing tests lack MockMvc / Spring Security integration verification.

---

## 3. Caveats

1. **Multi-Node Clustering**: The in-memory sliding window in `WhatsAppIdempotencyServiceImpl` is node-local (`ConcurrentHashMap`). If SareeKart were scaled horizontally across multiple application servers, deduplication would rely on the database layer (`findByWamId` and unique constraint on `wam_id`), as Redis is not configured in this stack.
2. **Requirements R2–R5**: Scope of this survey was strictly restricted to Requirement R1 (Webhook Security & Signature Hardening). Requirements R2–R5 (Opt-in/STOP/START, templates, rate limiting, and trousseau isolation) are covered by peer survey explorers.

---

## 4. Conclusion

Requirement R1 is functionally ~70% implemented, but has critical hardening gaps:
1. **Binding Gap**: `application.yaml` must map `app-secret: ${WHATSAPP_APP_SECRET:sareekart-meta-secret-2026}` so the Meta secret env var is honored.
2. **Security Vulnerability**: The dev-mode bypass in `WhatsAppWebhookSignatureValidator` must be tightened to strictly reject missing signatures in production mode.
3. **Spring Security**: `SecurityConfig.java` should explicitly permit `/api/webhook/**`.
4. **Deliverable Gap**: `WhatsAppProductionReadinessTest.java` must be authored to validate GET 200/403, POST 401 on tampered/missing HMAC, and duplicate `wam_id` suppression.

---

## 5. Verification Method

To independently verify these findings:
1. **Verify property omission**:
   ```bash
   grep -n "app-secret" /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/application.yaml
   ```
   (Returns no matches, confirming the binding gap).
2. **Verify dev bypass in signature validator**:
   Inspect lines 34–42 of `WhatsAppWebhookSignatureValidator.java`.
3. **Verify absence of required test file**:
   ```bash
   find /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/test -name "WhatsAppProductionReadinessTest.java"
   ```
   (Returns empty).
4. **Verify current unit test execution**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend && ./mvnw test -Dtest=WhatsAppWebhookControllerTest,WhatsAppWebhookSignatureValidatorTest,WhatsAppIdempotencyServiceTest
   ```
   (Executes 15 tests, confirming existing baseline passes).
5. **Verify disk health**:
   ```bash
   bash ~/scripts/check_disk_health.sh
   ```
   (Confirms free space >= 30%).
