## 2026-09-11T10:12:39Z

User Request:
You are Explorer 3 for Milestone 1 (REST Controllers, Security RBAC & Unit Tests).

Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_3_5
Read the authoritative user request at: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md
Read the project blueprint at: /Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md
Also reference the survey report at: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/survey_backend.md and /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_spec_miner_survey_tests_5/survey_specs.md

Your scope for Milestone 1:
1. Customer REST Controller (`ReturnController.java` under `/api/returns`):
   - `POST /api/returns`: Submit new return/exchange request.
   - `GET /api/returns/my-requests`: Fetch user's return history.
   - `GET /api/returns/order/{orderId}`: Fetch return claim by order ID.
   - `POST /api/returns/upload-photo`: Authenticated photo upload storing in `uploads/return-photos/` and returning accessible URL.
2. Admin REST Controller (`AdminReturnController.java` under `/api/admin/returns`):
   - `GET /api/admin/returns`: Filterable list of all return claims with status filter (`ALL`, `PENDING`, `APPROVED`, etc.).
   - `PUT /api/admin/returns/{id}/status`: Moderate status (approve, schedule pickup with courier/AWB, complete refund, reject).
3. Security RBAC in `SecurityConfig.java`:
   - `/api/returns/**` requires authenticated customer.
   - `/api/admin/returns/**` requires role `OWNER`, `MANAGER`, or `ADMIN`.
   - Unauthorized attempts receive HTTP 403 `{"success":false,"message":"Not authorised to perform this action"}`.
4. Comprehensive test plan and exact test methods for `ReturnServiceImplTest.java`:
   - Return creation succeeds on delivered order <= 7 days old.
   - Return creation fails on non-delivered order (e.g. `SHIPPED` or `PENDING`).
   - Return creation fails on delivered order > 7 days old.
   - Duplicate return submission on same order is rejected.
   - Unauthorized customer cannot access another customer's return claim.
   - Staff (`OWNER`, `MANAGER`, `ADMIN`) can update status, assign courier AWB, and approve/reject.
5. Produce exact controller mappings, security configuration rules, and complete `ReturnServiceImplTest` code structure for the Worker. Do NOT implement the code yourself.
6. Save your report to `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m1_3_5/m1_controller_test_spec.md` and write `handoff.md`.
7. Send a message to your caller when complete.
