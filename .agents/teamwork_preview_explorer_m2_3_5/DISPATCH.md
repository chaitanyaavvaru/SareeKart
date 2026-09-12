## 2026-09-11T10:37:00Z
You are Explorer 3 for Milestone 2 (Admin Moderation Console & Navigation).

Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_3_5
Read the authoritative user request at: /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md
Read the project blueprint at: /Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md
Also reference the frontend survey report at: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_survey_frontend_5/survey_frontend.md

Your scope for Milestone 2:
1. Admin Moderation Console `frontend/src/pages/Admin/ManageReturns.jsx`:
   - Accessible by roles `OWNER`, `MANAGER`, `ADMIN`.
   - KPI metric summary cards: Total Claims, Pending Review, Pickups Scheduled, Completed Refunds.
   - Filterable claims table with status tabs (`ALL`, `PENDING`, `APPROVED`, `PICKUP_SCHEDULED`, `REJECTED`, `COMPLETED`), search input, customer details, order amount, reason, and photo thumbnails.
   - Inspection Drawer/Modal: High-resolution defect photos lightbox with thumbnail switcher and order details side-by-side.
   - 1-Click Action Controls:
     * Approve Return -> advances to `APPROVED`.
     * Assign Courier & Schedule Pickup -> modal with courier partner dropdown/input (Blue Dart, Delhivery, DTDC, India Post) and AWB tracking number; advances to `PICKUP_SCHEDULED`.
     * Complete Refund -> advances to `COMPLETED`.
     * Reject Return -> modal with mandatory rejection reason; advances to `REJECTED`.
2. Navigation & Routing:
   - Register route `/admin/returns` in `frontend/src/routes/AppRouter.jsx`.
   - Add "Returns & Exchanges" under Commerce in `frontend/src/pages/Admin/AdminDashboard.jsx` sidebar using `RotateCcw` icon.
3. Bundle Budget & Performance:
   - Keep chunks strictly under 500 kB (pure Tailwind and Lucide React, no heavy external chart libraries).
4. Detail exact specifications and component design for the downstream Worker. Do NOT implement the code yourself.
5. Save your report in `/Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_3_5/m2_admin_spec.md` and write `handoff.md`.
6. Send a message to your caller when complete.
