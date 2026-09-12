# Handoff Report: SareeKart Return Requests Specification & Test Survey

**Agent Workspace**: `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5`  
**Role**: Test & Specification Miner  
**Target Milestone**: SareeKart v3.0 Module 1 (Self-Service Customer Returns & Exchanges)  

---

## 1. Observation

1. **Storage Health Verification**:
   - Command: `/Users/chaitanyachaitu/scripts/check_disk_health.sh`
   - Output:
     ```
     Mount Point:          /System/Volumes/Data
     Total Storage:        228.3 GiB
     Used Storage:         114.3 GiB (50.1%)
     Available Free Space: 78.5 GiB (34.4%)
     Target Policy:        >= 30% Free Space
     Status: [PASS] Healthy Storage Headroom (34.4% >= 30%)
     ```

2. **Flyway Migration & Database Schema State**:
   - In `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/`:
     * `application.yaml` (lines 9-23) configures `jdbc:mysql://localhost:3306/sareekart_db` with `jpa.hibernate.ddl-auto: update`.
     * `db/migration/` did not exist in `Downloads/SareeKart-main/`.
   - In reference repository `/Users/chaitanyachaitu/SareeKart/backend/backend/src/main/resources/db/migration/`:
     * Migrations span `V1__create_initial_schema.sql` through `V16__order_shipment_tracking_and_returns.sql`.
     * `V16__order_shipment_tracking_and_returns.sql` (lines 2-13) added raw tracking and return columns (`return_status`, `return_reason`, `return_comment`, `return_requested_at`, `return_resolved_at`, `return_admin_notes`) directly to `orders`.
   - In `backups/sareekart_db_latest.sql`:
     * `orders` table (lines 212-232) has `id`, `user_id` (FK to `users`), `status enum('CANCELLED','CONFIRMED','DELIVERED','PENDING','SHIPPED')`, `total_amount decimal(10,2)`, `updated_at datetime(6)`.
     * `users` table (lines 340-352) has `id`, `email`, `role enum('ADMIN','CUSTOMER')` (and `Role.java` supports `CUSTOMER`, `MANAGER`, `OWNER`, `ADMIN`).

3. **Backend Test Infrastructure Baseline**:
   - `backend/backend/src/test/resources/application-test.yaml`:
     * H2 in-memory URL: `jdbc:h2:mem:sareekart_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`.
     * Dialect: `org.hibernate.dialect.H2Dialect`, `ddl-auto: create-drop`.
   - Existing service test patterns (`ApprovalServiceTest.java`, `CartServiceTest.java`, `OperationsEngagementSuiteTest.java`):
     * Use `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.
   - Full test run command: `./mvnw test` in `backend/backend`
     * Result: `Tests run: 65, Failures: 0, Errors: 0, Skipped: 0. Total time: 11.128 s. BUILD SUCCESS.`

4. **Security & Exception Handling Patterns**:
   - `SecurityConfig.java` (lines 81-83):
     * `.requestMatchers("/api/admin/**").hasAnyRole("OWNER", "MANAGER", "ADMIN")`
     * `.anyRequest().authenticated()`
   - `GlobalExceptionHandler.java`:
     * Lines 28-33: `BadRequestException` -> HTTP 400 `ApiResponse.error(ex.getMessage())`.
     * Lines 21-26: `ResourceNotFoundException` -> HTTP 404 `ApiResponse.error(ex.getMessage())`.
     * Lines 106-111: `AccessDeniedException` -> HTTP 403 `{"success":false,"message":"Not authorised to perform this action"}`.

5. **Frontend Build & Chunk Budget**:
   - `frontend/vite.config.js`:
     * `chunkSizeWarningLimit: 480`
     * Configured `manualChunks`: `vendor-framer-motion`, `vendor-lucide`, `vendor-react`, `vendor-redux`.
   - Build run command: `npm run build` in `frontend`
     * Output: `✓ built in 267ms`. Largest chunk is `vendor-react` (227.44 kB), second largest is `vendor-framer-motion` (132.83 kB). All chunks strictly < 500 kB budget.

---

## 2. Logic Chain

1. **Database Schema Strategy**:
   - Because `~/SareeKart` already reaches `V16`, introducing the new `return_requests` table as `V17__create_return_requests_table.sql` (or `V1` for clean-room installations) maintains exact migration sequence integrity.
   - The table requires a unique constraint `CONSTRAINT uk_return_requests_order UNIQUE (order_id)` to enforce the business rule that duplicate return claims on the same order are strictly rejected.
   - Linking `order_id` -> `orders.id` and `user_id` -> `users.id` with `ON DELETE RESTRICT` guarantees referential integrity without orphan records.
   - Text/JSON storage for `images` accommodates 1 to 3 condition defect photo URLs.

2. **Test Specification Logic for `ReturnServiceImplTest.java`**:
   - **T1 (Success on <= 7 days)**: If an order has `status == OrderStatus.DELIVERED` and `updatedAt` / delivery timestamp is within 7 calendar days, `createReturnRequest` must persist a `ReturnRequest` entity in `PENDING` status and return an `ApiResponse` with HTTP 201/200.
   - **T2 (Non-delivered order)**: If `status != OrderStatus.DELIVERED` (e.g. `SHIPPED` or `PENDING`), attempting return creation must throw `BadRequestException("Order must be DELIVERED to request a return or exchange")`.
   - **T3 (Delivered > 7 days)**: If `status == OrderStatus.DELIVERED` but delivery was > 7 days ago, `createReturnRequest` must throw `BadRequestException("Return window has expired (7 days cutoff from delivery)")`.
   - **T4 (Duplicate submission)**: When `returnRequestRepository.existsByOrderId(orderId)` or `findByOrderId` returns an existing claim, throwing `BadRequestException("A return or exchange request already exists for this order")` prevents multiple active return claims.
   - **T5 (Customer access control)**: A customer querying or modifying another customer's claim must be rejected with an authorization error.
   - **T6–T10 (Staff workflow)**: Staff (`OWNER`, `MANAGER`, `ADMIN`) can transition status to `APPROVED`, `PICKUP_SCHEDULED` (attaching courier partner and AWB number), or `REJECTED` (requiring mandatory explanation). Rejecting without a reason throws `BadRequestException`.

3. **Frontend Implementation Logic**:
   - In `MyOrders.jsx`, the return button should only be active if `order.status === 'DELIVERED'` and difference between current time and delivery date is <= 7 days; otherwise it should be disabled with an informative tooltip.
   - `/admin/returns` should be registered in `AppRouter.jsx` under the protected `/admin` route and linked in `ADMIN_NAV` under the Commerce group.
   - Pure Tailwind CSS and existing `lucide-react` icons ensure chunk size remains well under 500 kB (current largest is 227 kB).

---

## 3. Caveats

1. In `Downloads/SareeKart-main/backend/backend/src/main/resources/application.yaml`, Flyway is not currently wired as a default active starter in `pom.xml`, relying on Hibernate's `ddl-auto: update`. Creating `db/migration/V17__create_return_requests_table.sql` (or adding Flyway migration script) ensures database migrations can run via Flyway when enabled or as the reference SQL schema.
2. In the existing `Order` entity, delivery date is tracked via `updatedAt` when transitioned to `DELIVERED`. `Order` also has `estimatedDeliveryDate`. For return eligibility calculation, `updatedAt` (or a dedicated `deliveredAt` timestamp) should be used.

---

## 4. Conclusion

All specifications, database schema definitions, backend test infrastructure matrices, and frontend build constraints have been comprehensively mined, verified against the running codebases, and documented in:
- `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5/survey_specs.md`

The plan for `ReturnServiceImplTest.java` is fully specified with 10 targeted test cases covering all 6 core functional requirements, error paths, and edge cases.
All baseline tests pass (65/65 backend tests pass; frontend builds in 267ms with all chunks < 230 kB; disk space is healthy at 34.4% free).

---

## 5. Verification Method

To independently reproduce and verify this survey:

1. **Verify Storage Discipline**:
   ```bash
   /Users/chaitanyachaitu/scripts/check_disk_health.sh
   # Expected: >= 30% free space (currently 34.4%)
   ```

2. **Verify Backend Baseline**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend
   ./mvnw test
   # Expected: 65 tests passing, 0 failures, 0 errors
   ```

3. **Verify Frontend Build & Chunk Budget**:
   ```bash
   cd /Users/chaitanyachaitu/Downloads/SareeKart-main/frontend
   npm run build
   # Expected: 0 errors, all chunks under 500 kB
   ```

4. **Inspect Survey Report**:
   ```bash
   cat /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5/survey_specs.md
   ```
