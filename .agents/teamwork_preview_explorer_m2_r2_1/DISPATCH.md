## 2026-09-11T14:20:04Z
You are Explorer 1 for Milestone 2 remediation in SareeKart.
Your working directory is: /Users/chaitanyachaitu/Downloads/SareeKart-main/.agents/teamwork_preview_explorer_m2_r2_1
Project root: /Users/chaitanyachaitu/Downloads/SareeKart-main

CRITICAL: Read /Users/chaitanyachaitu/Downloads/SareeKart-main/ORIGINAL_REQUEST.md and /Users/chaitanyachaitu/Downloads/SareeKart-main/PROJECT.md before starting work.

Previous Gate Failure Feedback:
- Challenger 2 reported: In `frontend/src/pages/MyOrders.jsx:95-98`, `getReturnEligibility` uses `daysSinceDelivery = Math.floor(diffMs / (1000 * 60 * 60 * 24))` and `if (daysSinceDelivery > 7)`. For an order delivered 7.1 days ago, `Math.floor(7.1)` is 7, so `7 > 7` is false. The order is incorrectly marked eligible with active "Return / Exchange" button showing "0d left", but backend strictly enforces `LocalDateTime.now().isAfter(deliveryTime.plusDays(7))` and rejects with HTTP 400!
- Reviewer 1 reported: `ReturnRequestModal.jsx` allows submitting `type == EXCHANGE` without ensuring `exchangeSku` is populated, but backend `ReturnServiceImpl.java:73-76` throws `BadRequestException("Exchange SKU is required for exchange requests.")`.

Your Task:
Investigate `frontend/src/pages/MyOrders.jsx` and `frontend/src/components/orders/ReturnRequestModal.jsx`.
Formulate exact, verified code replacement recommendations:
1. Exact fix for `getReturnEligibility` in `MyOrders.jsx` ensuring that any order older than 7.0 days (`diffDays > 7`) is marked ineligible (`Return window expired (7 days cutoff from delivery)`) and `daysRemaining` calculation correctly reflects remaining days.
2. Exact fix for `ReturnRequestModal.jsx` ensuring that when `type === 'EXCHANGE'`, either an exchange saree is selected and its SKU passed, or clear validation prevents submitting with null/empty `exchangeSku`.

Write your findings, exact line numbers, and recommended diff in `handoff.md` in your working directory.
Communicate completion via `send_message`. DO NOT implement changes in source code yourself.
