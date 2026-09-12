# Execution Plan — Successor Project Orchestrator (Generation 2)

## Goal
Verify Milestone 2 frontend implementation and complete Milestone 3 full regression, ensuring 100% test pass rate, strict < 500 kB bundle budget, >= 30% free disk space, and clean forensic integrity audit.

---

## Phase 1: Milestone 2 Frontend Quality Gate Verification
1. **Reviewers (2 parallel)**:
   - `teamwork_preview_reviewer_m2_1`: Inspect component structure, state management, 7-day eligibility gate, accessible tooltips, refund mode mapping, photo upload preview/deletion, and RBAC admin route protection.
   - `teamwork_preview_reviewer_m2_2`: Inspect error handling, offline mock fallback, AWB tracking display with copy-to-clipboard feedback, inspection drawer lightbox, and Lucide React icon integration.
2. **Challengers (2 parallel)**:
   - `teamwork_preview_challenger_m2_1`: Execute frontend build (`npm run build`), verify all chunks < 500 kB, run ESLint (`npx eslint`), test bundle metrics.
   - `teamwork_preview_challenger_m2_2`: Adversarially test eligibility calculations, edge cases (0 days, 7 days, 8 days, invalid status, missing delivery date), photo uploader boundaries (3 photos, oversized, wrong MIME).
3. **Forensic Auditor (1)**:
   - `teamwork_preview_auditor_m2`: Verify authentic integration, detect any hardcoded returns, fake mock shortcuts, bypasses, or dummy implementations.
4. **Gate Evaluation**:
   - Collect verdicts in `GATE_STATUS.md`.
   - All Reviewers APPROVE, all Challengers PASS, Auditor CLEAN.

---

## Phase 2: Milestone 3 Full Regression & Production Hardening
1. **Full Backend Test Regression**:
   - Execute full Maven test suite (`./mvnw test`) covering 139+ tests across all domain areas.
   - Verify 0 failures, 0 errors, 100% pass rate.
2. **Production Bundle Verification**:
   - Execute clean production build (`npm run build`).
   - Verify 0 errors and all chunks strictly under 500 kB budget.
3. **System Storage Health**:
   - Verify disk headroom using `~/scripts/check_disk_health.sh` (must be >= 30% free space).
4. **Final Adversarial & Forensic Verification**:
   - Final Challenger test and Forensic Auditor across backend + frontend.
5. **Gate Evaluation**:
   - Record Milestone 3 PASS in `GATE_STATUS.md` and `PROJECT.md`.

---

## Phase 3: Victory Notification to Sentinel
1. Synthesize final status report.
2. Send comprehensive handoff and victory audit notification to the Sentinel (`b4ebefaf-d767-4463-af5a-ce3e70365b04`) via `send_message`.
