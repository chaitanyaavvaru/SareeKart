## 2026-09-11T10:07:17Z
You are the Test & Specification Miner for SareeKart.

Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5
Read the authoritative user request at: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md
Specifically study the latest request in ORIGINAL_REQUEST.md under section "## Follow-up — 2026-09-11T10:04:03Z".

Your task:
1. Inspect the database schema and Flyway migrations in `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/main/resources/db/migration/`:
   - Identify existing migration version numbering and table schema patterns.
   - Design the Flyway migration script (e.g. `V...__create_return_requests_table.sql`) with correct column types, foreign keys, indexes, and constraints.
2. Inspect the backend test infrastructure in `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/src/test/java/com/sareekart/`:
   - Inspect existing service and controller test patterns (`BaseTest`, mockito, test profiles, in-memory H2 configuration).
   - Plan all test cases required for `ReturnServiceImplTest.java`:
     * Return creation succeeds on delivered order <= 7 days old.
     * Return creation fails on non-delivered order (e.g. SHIPPED or PENDING).
     * Return creation fails on delivered order > 7 days old.
     * Duplicate return submission on same order is rejected.
     * Unauthorized customer cannot access another customer's return claim.
     * Staff (OWNER, MANAGER, ADMIN) can update status, assign courier AWB, and approve/reject.
3. Inspect frontend test & build constraints:
   - Check `package.json`, Vite configuration, build commands (`npm run build`), chunk size budget (< 500 kB).
   - Check disk space verification script `~/scripts/check_disk_health.sh`.
4. Save your comprehensive report in `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5/survey_specs.md` and write `handoff.md` in your working directory.
5. Send a message to your caller when complete.
