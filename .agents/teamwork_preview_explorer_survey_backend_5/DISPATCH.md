## 2026-09-11T10:07:17Z
You are the Backend Architecture Explorer for SareeKart.

Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5
Read the authoritative user request at: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md
Specifically study the latest request in ORIGINAL_REQUEST.md under section "## Follow-up — 2026-09-11T10:04:03Z".

Your task:
1. Inspect the backend codebase in `/Users/chaitanyachaitu/Downloads/SareeKart-main/backend/backend/`:
   - Order entity (`Order.java`), order status enums (`OrderStatus.java`), delivery timestamp fields, and how orders track status changes.
   - User entity (`User.java`), role definitions (`Role.java`), authentication and authorization in `SecurityConfig.java`.
   - Existing controller patterns, DTO patterns, and response conventions (`ApiResponse.java`, `GlobalExceptionHandler.java`).
   - Static resource handling / photo upload handlers (how files are uploaded, saved, and served at `/uploads/**`).
2. Design the exact backend architecture for the Returns & Exchanges system:
   - Entity `ReturnRequest` (`return_requests` table) with all required fields (id, order_id, user_id, type, reason, comments, status, images, refund_amount, refund_mode, exchange_sku, reverse_courier, reverse_tracking_number, admin_notes, created_at, updated_at).
   - Repository `ReturnRequestRepository` queries.
   - Service interface `ReturnService` and implementation `ReturnServiceImpl` (order ownership check, <= 7 days cutoff check, duplicate check, state transitions: PENDING -> APPROVED -> PICKUP_SCHEDULED -> COMPLETED, or REJECTED).
   - REST Controllers: Customer endpoints (`POST /api/returns`, `GET /api/returns/my-requests`, `GET /api/returns/order/{orderId}`, `POST /api/returns/upload-photo`) and Admin endpoints (`GET /api/admin/returns`, `PUT /api/admin/returns/{id}/status`).
   - SecurityConfig RBAC rules (customer vs OWNER/MANAGER/ADMIN, 403 response format).
3. Document all file paths, class names, field types, and edge cases.
4. Save your comprehensive report in `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_backend_5/survey_backend.md` and write `handoff.md` in your working directory.
5. Send a message to your caller when complete.
