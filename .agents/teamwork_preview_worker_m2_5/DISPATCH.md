## 2026-09-11T10:40:37Z

You are the Milestone 2 Frontend Implementation Worker for SareeKart.

Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_worker_m2_5
Read the authoritative user request at: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md (specifically section "## Follow-up — 2026-09-11T10:04:03Z").
Read the project blueprint at: /Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md.
Read the 3 detailed specifications prepared by the Milestone 2 Explorers:
1. Modal & Upload Service Spec: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_1_5/m2_modal_spec.md
2. Order Card Eligibility & Telemetry Drawer Spec: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_2_5/m2_telemetry_spec.md
3. Admin Moderation Console & Navigation Spec: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_3_5/m2_admin_spec.md

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A teamwork_preview_auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.

Your Exclusive Write Ownership:
You own and must implement the following files in `/Users/chaitanyachaitu/Downloads/SareeKart-main/frontend/`:
1. `src/services/returnService.js`: API client with endpoints `createReturnRequest`, `getMyReturns`, `getReturnByOrderId`, `uploadConditionPhoto` (multipart), `getAllReturns`, and `updateReturnStatus`.
2. `src/components/orders/ReturnRequestModal.jsx`: Self-service returns modal with Return vs. Exchange toggle, 6-reason taxonomy (`COLOR_MISMATCH`, `ZARI_DEFECT`, `FABRIC_FEEL`, `INCORRECT_ITEM`, `SIZE_MISMATCH`, `OTHER`), drag-and-drop defect photo uploader (up to 3 photos, client preview, deletion), refund preference selection (`ORIGINAL_PAYMENT`, `STORE_CREDIT`, `EXCHANGE_DRAPE`), and comments validation.
3. `src/components/orders/ReturnStatusDrawer.jsx`: 6-stage milestone tracker (`Requested`, `Approved`, `Pickup Scheduled`, `Picked Up`, `In Transit`, `Completed / Refunded`), courier partner details, copyable AWB code with visual feedback, admin notes, and photo gallery.
4. `src/pages/MyOrders.jsx`: 7-day post-delivery eligibility gate (active button with remaining days badge, or disabled button wrapped in an accessible CSS tooltip explaining why), return status telemetry pill (`Return: Pending Review`, `Pickup Scheduled - ...`, etc.), rejection alert banner with admin notes, and "View Return Status" link opening drawer.
5. `src/pages/Admin/ManageReturns.jsx`: Admin moderation console for `OWNER`, `MANAGER`, `ADMIN` with 4 KPI summary cards (Total Claims, Pending Review, Pickups Scheduled, Completed Refunds), filterable claims table (`ALL`, `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`), search filter, side-by-side defect photo inspection lightbox, and 1-click action controls (Approve, Schedule Pickup with courier and AWB, Complete Refund, Reject with mandatory reason).
6. `src/routes/AppRouter.jsx`: Register route `/admin/returns` under `ProtectedRoute adminOnly={true}`.
7. `src/pages/Admin/AdminDashboard.jsx`: Add "Returns & Exchanges" link under Commerce in `ADMIN_NAV` using `RotateCcw` icon.

Verification Requirements:
1. Run `npm run build` in `frontend/` to verify clean build with 0 errors and all chunks strictly under 500 kB.
2. Run `npm run lint` in `frontend/` to verify zero ESLint errors.
3. Run `/Users/chaitanyachaitu/scripts/check_disk_health.sh` to verify storage health >= 30%.
4. Record exact commands and execution outputs.
5. Save your 5-component `handoff.md` in your working directory.
6. Send a message to your caller when complete.
