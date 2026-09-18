# Handoff Report: Survey Explorer 2 (Requirements R2 & R3)

## 1. Observation
1. **Consent Entity & Persistence (R2):**
   - `WhatsAppContact.java`: Lines 20-40 contain only `id`, `phoneNumber`, `name`, `user`, `createdAt`, `updatedAt`. Does not have `optedIn` or consent timestamps (`optedInAt`, `optedOutAt`).
   - `User.java`: Lines 40-42 define `@Column(name = "whatsapp_opt_in") @Builder.Default private Boolean whatsappOptIn = true;`. Consent is stored only on `User`.
   - `V21__create_whatsapp_dispatch_tables.sql`: Lines 25-30 add `whatsapp_opt_in` to table `users`. Table `whatsapp_contacts` has no `opted_in` column.
2. **Keyword Ingestion (R2):**
   - `WhatsAppAiCommerceServiceImpl.java`: Line 56 checks `if (lower.equals("stop") || lower.equals("unsubscribe"))`. Keyword `"cancel"` is missing.
   - `WhatsAppAiCommerceServiceImpl.java`: Line 67 checks `if (lower.equals("start"))`. Keyword `"unstop"` is missing.
   - `WhatsAppWebhookService.java`: Line 146 checks `if (conversation.getStatus() == ConversationStatus.BOT_HANDLING ...)`. If conversation is `HUMAN_ESCALATION` or `OPEN`, the bot is never invoked (line 158), so incoming `STOP` / `START` messages are not processed for escalated patrons.
3. **Outbound Verification & Suppression (R2):**
   - `WhatsAppNotificationServiceImpl.java`: Line 403 evaluates `boolean isOptedIn = user == null || user.getWhatsappOptIn() == null || user.getWhatsappOptIn();`. When `user == null`, it evaluates to `true`, dispatching messages even if contact previously sent STOP.
4. **Phone Normalization (R3):**
   - `WhatsAppIdentityServiceImpl.java`: Lines 30-41 strip `91` or leading `0` and return 10 digits (`9876543210`).
   - `WhatsAppIdentityServiceTest.java`: Lines 52-56 assert normalization to 10 digits (`9876543210`).
   - Meta WhatsApp Graph API v19.0 `/messages` endpoint requires international recipient format (`919876543210` / `+919876543210`).
5. **Meta HSM Templates (R3):**
   - `WhatsAppNotificationServiceImpl.java`: Line 429 invokes `whatsAppApiClient.sendTextMessage(phone, content)` for all notifications, dispatching plain text rather than Meta HSM template payloads.
   - `WhatsAppApiClient.java`: Lines 33-114 provide `sendTextMessage`, `sendImageMessage`, `sendInteractiveButtonsMessage`, `sendInteractiveListMessage`, but no `sendTemplateMessage`.
   - `WhatsAppMessageRequest.java`: Lines 45-48 define `Template` with only `name` and `language`, omitting `components` and `parameters`.
6. **Sensitive Data Masking (R3):**
   - Outbound notifications transmit raw database primary keys:
     - `WhatsAppNotificationServiceImpl.java:78, 83`: leaks `order.getId()` in `"Your bespoke order *#%d*"` and `https://sareekart.com/orders/%d`.
     - `WhatsAppNotificationServiceImpl.java:201, 206`: leaks `returnRequest.getId()` in `"reverse pickup for return claim *#%d*"` and `REV-AWB-` + `returnRequest.getId()`.
     - `WhatsAppAiCommerceServiceImpl.java:223`: leaks `order.getId()` in `https://sareekart.com/track/` + `order.getId()`.
   - In contrast, `TrousseauWhatsAppServiceImpl.java:57` safely uses UUID `board.getShareToken()`.
   - No passwords, JWTs, or raw credit cards are transmitted.
7. **Test Suite Status:**
   - `./mvnw test -Dtest="WhatsApp*Test"`: Executed 40 tests across 8 test suites, 0 failures, 0 errors.
   - `WhatsAppProductionReadinessTest.java`: Acceptance test file does not exist.
   - `~/scripts/check_disk_health.sh`: Verified 105.0 GiB available (46.0% free space >= 30%).

## 2. Logic Chain
1. Based on Observation 1, because `WhatsAppContact` lacks an `optedIn` field, opt-in/opt-out status can only be recorded on a `User` entity.
2. Therefore, when a guest shopper or unlinked patron texts `STOP`, their opt-out cannot be saved to their contact record.
3. Combining this with Observation 3, where `WhatsAppNotificationServiceImpl` defaults `isOptedIn` to `true` when `user == null`, guest users who text `STOP` will continue receiving outbound notifications.
4. From Observation 2, because keyword interception resides inside `WhatsAppAiCommerceServiceImpl` rather than at the webhook ingestion gate, any customer whose conversation has been marked `HUMAN_ESCALATION` or `OPEN` will have their `STOP` or `START` commands ignored.
5. From Observation 4, Meta Graph API v19.0 requires country codes for recipient phone numbers (`91XXXXXXXXXX`), but `WhatsAppIdentityService` strips country codes down to 10 digits (`XXXXXXXXXX`), which will cause API rejections in production.
6. From Observation 5, Meta Business policies prohibit freeform text messages for business-initiated updates outside the 24-hour customer window; dispatching via `sendTextMessage` will fail with Meta error 131047.
7. From Observation 6, internal database primary keys are exposed in customer messages and URLs, violating Requirement R3's sensitive data masking mandate.

## 3. Caveats
- Production Meta Cloud API live credentials were not tested against Meta servers (using mock / simulated token mode in accordance with test environment constraints).
- The web frontend does not contain WhatsApp opt-in toggle components; preference management is exposed via REST endpoint `PUT /api/whatsapp/preference`.

## 4. Conclusion
Requirements R2 and R3 require targeted refactoring before Phase 13 Stage 4 production sign-off:
1. Refactor `WhatsAppContact` to include `optedIn`, `optedInAt`, `optedOutAt`, backed by a Flyway migration.
2. Intercept `STOP`, `UNSUBSCRIBE`, `CANCEL`, `START`, and `UNSTOP` synchronously in `WhatsAppWebhookService` before conversation routing.
3. Enhance `WhatsAppIdentityService` to support standard E.164 (`+91...`) and Meta recipient format (`91...`).
4. Upgrade `WhatsAppMessageRequest` and `WhatsAppApiClient` to support HSM template payloads with component/parameter structures and validation.
5. Mask internal database primary keys in all notification messages and URLs using public tracking numbers / reference masks.
6. Author comprehensive test suite `WhatsAppProductionReadinessTest.java` verifying all acceptance criteria.

## 5. Verification Method
1. Inspect full report at:
   `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_2/survey_report.md`
2. Run WhatsApp test suite to confirm baseline passes:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test -Dtest="WhatsApp*Test"
   ```
3. Run disk health check:
   ```bash
   ~/scripts/check_disk_health.sh
   ```
4. Invalidation conditions:
   - If `WhatsAppContact.java` already contained `optedIn` (verified absent).
   - If `CANCEL` or `UNSTOP` keywords were handled elsewhere (verified absent across codebase).
   - If `WhatsAppApiClient.java` supported HSM templates (verified absent).
