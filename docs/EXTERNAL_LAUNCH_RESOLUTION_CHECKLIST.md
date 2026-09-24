# SareeKart — External Launch Resolution Checklist & Operational Wait-State Protocol
**Authoritative Operational Runbook for External Trigger Events**  
*Repository: `/Users/chaitanyachaitu/Downloads/SareeKart-main` | Authoritative Domain: `https://sareekart.com`*  
*Budget Expended: ₹0.00 | Operational Disposition: FORMAL WAIT STATE*  

---

## Executive Wait-State Declaration

The SareeKart technical and operational launch sequence (Days 1–8) is **officially concluded and paused**. 

All internal software engineering, backend persistence, frontend UI components, 11-point product quality control checklists, packaging standard operating procedures, and 1-to-1 customer qualification workflows are **100% complete and verified**:
- **Full-Stack Local Application Stack**: Active and healthy (`mysqld` on :3306, Spring Boot on :8081, React/Vite on :5173).
- **Automated Regression Suite**: 101/101 PASS (`node --test frontend/tests/*.test.mjs`).
- **Cumulative Workspace Tests**: 1,226 / 1,226 PASS.
- **Production Bundle Hygiene**: All chunks strictly `< 500 kB` (largest chunk: 229 kB).
- **System Storage Headroom**: 42.5% free space (96.9 GiB available); Whisper cache: 0 bytes.

**No Day 9 report, no additional feature development, no refactoring, and zero advertising spend will be generated.** The project is now in a **clean, zero-cost, zero-fabrication operational wait state**. 

Operational activity will resume **ONLY** upon the empirical occurrence of one of two external events:
1. **The apex domain `sareekart.com` cutover is propagated by the registrar.**
2. **An authentic customer payment receipt (UTR) is verified for `Lead-002`.**

*See companion operational runbook: [`docs/TRIGGER_BASED_RESUMPTION_PROTOCOL.md`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docs/TRIGGER_BASED_RESUMPTION_PROTOCOL.md).*

---

## 1. Operational Resume Decision Tree

```text
========================================================================
                 SAREEKART WAIT-STATE RESUME DECISION TREE              
========================================================================

                       DNS becomes LIVE?
                               │
                          ┌────┴────┐
                          NO        YES
                          │          │
                        WAIT      Public smoke test
                                     │
                                 PASS?
                               ┌────┴────┐
                               NO        YES
                               │          │
                              FIX       LIVE
                                          │
                                 Customer payment?
                                    ┌────┴────┐
                                    NO        YES
                                    │          │
                                   WAIT     Verify UTR
                                                │
                                           Confirm order
                                                │
                                           11-Point QC
                                                │
                                        Dispatch & Tracking
========================================================================
```

---

## 2. Gate 1 — Authoritative Domain & DNS Cutover Checklist

> [!IMPORTANT]
> **Critical DNS Guard**:
> Target DNS records must always be cross-referenced with the active Vercel project deployment dashboard at the exact moment of cutover rather than assuming static values. Currently confirmed targets:
> - `A @` $\longrightarrow$ `76.76.21.21`
> - `CNAME www` $\longrightarrow$ `cname.vercel-dns.com`

- [ ] **Step 1.1 — Registrar Configuration**: Domain owner logs into authoritative registrar (GoDaddy / Namecheap) and applies the active hosting DNS records.
- [ ] **Step 1.2 — Empirical DNS Propagation Probe**:
  ```bash
  ./scripts/verify_live_domain.sh
  ```
  Verify that `sareekart.com` and `www.sareekart.com` no longer resolve to Afternic parking nameservers (`13.248.169.48`, `76.223.54.146`) and resolve to the authoritative edge CDN.
- [ ] **Step 1.3 — TLS Certificate Handshake**: Probe HTTPS port 443 to confirm active, valid TLS termination (Let's Encrypt / Vercel SSL).
- [ ] **Step 1.4 — 12-Point Public Storefront Smoke Test Suite**:
  Execute live HTTP/HTTPS verification across all 12 commercial journeys:
  1. **Homepage Luxury Branding**: `https://sareekart.com/` (HTTP 200, hero assets render)
  2. **Product Catalog**: `https://sareekart.com/products` (HTTP 200, 24 sarees render)
  3. **Product Detail Page**: `https://sareekart.com/product/3` (HTTP 200, 6.3m length & tested zari render)
  4. **Instant Search & Debounce**: `https://sareekart.com/search` (HTTP 200, search queries function)
  5. **Category Taxonomy**: `https://sareekart.com/categories` (HTTP 200, curated handloom categories load)
  6. **Customer Auth & Login**: `https://sareekart.com/login` (HTTP 200, stateless JWT auth slice active)
  7. **Wishlist Persistence**: `https://sareekart.com/wishlist` (HTTP 200, localStorage persistence verified)
  8. **Cart Subtotals**: `https://sareekart.com/cart` (HTTP 200, pricing calculations accurate)
  9. **Checkout Initialization**: `https://sareekart.com/checkout` (HTTP 200, payment gateway routing active)
  10. **SEO Directives**: `https://sareekart.com/robots.txt` (HTTP 200, private customer routes protected)
  11. **XML Sitemap**: `https://sareekart.com/sitemap.xml` (HTTP 200, valid luxury handloom URL set)
  12. **Backend API Health**: `https://sareekart.com/actuator/health` (HTTP 200, `{"status":"UP"}`)
- [ ] **Strict Quality Rule**: **Do NOT mark Gate 1 passed from DNS configuration alone.** Full public HTTPS smoke test pass is mandatory.

---

## 3. Gate 2 — First Customer Payment & Order Fulfillment Checklist

> [!WARNING]
> **Strict Financial Recognition Rule**:
> Verbal customer intent or messages ("I will pay tonight") do **NOT** constitute revenue or an order. Gross revenue remains **₹0.00** until an authentic, independently verifiable bank settlement receipt is verified.

- [ ] **Step 2.1 — Receipt Verification**: Receive authentic bank UTR (Unique Transaction Reference) / IMPS / PhonePe payment confirmation from `Lead-002`.
- [ ] **Step 2.2 — Amount & Account Audit**: Verify settled amount matches catalog retail price of **₹8,667** (₹0 shipping, 0 unauthorized discounts).
- [ ] **Step 2.3 — Customer & Order Relationship Match**: Verify payment belongs to `Lead-002` (Hyderabad) for SKU #3 (*Venkatagiri Fine Cotton Jamdani Saree*, Midnight Black).
- [ ] **Step 2.4 — Order Creation**: Log official order in backend system (`POST /api/orders`) and transition order status to `ORDER CONFIRMED`.
- [ ] **Step 2.5 — Revenue Recognition**: Record recognized gross revenue of ₹8,667 in official ledgers.
- [ ] **Step 2.6 — Inventory Allocation**: Deduct 1 unit from SKU #3 stock (updating uncommitted stock from 8 to 7).
- [ ] **Step 2.7 — 11-Point Product QC Inspection**:
  Execute [`docs/day_5_product_qc_checklist.md`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/docs/day_5_product_qc_checklist.md) on light table:
  - SKU & title match (SKU #3, *Venkatagiri Fine Cotton Jamdani*)
  - 6.3m dimensions (5.5m body + 0.8m running blouse piece, 46in width)
  - Pure combed 100s fine cotton fabric
  - Tested metallic electroplated zari
  - Zero weave tears, loose threads, or handling stains
  - Visual match against official catalog imagery
- [ ] **Step 2.8 — Protective Packaging SOP**:
  - Fold saree in traditional 4-fold orientation with pallu protected inward.
  - Seal inside virgin moisture-barrier poly sleeve / butter paper wrap.
  - Place in heavy-duty corrugated shipping box.
  - Apply tamper-evident H-tape sealing across all seams.
  - Affix waterproof shipping label containing recipient address, pin code, sender info, and barcode.
  - *Collateral boundary:* Do NOT promise physical printed "artisan authenticity cards" or luxury rigid boxes until physical print stock is procured.
- [ ] **Step 2.9 — Courier Dispatch & Tracking Notification**:
  - Hand over package to domestic courier counter (Speed Post / registered surface).
  - Obtain consignment tracking number (AWB).
  - Transmit WhatsApp Message E (Tracking Shared) to customer.

---

## 4. Gate 3 — Customer Pipeline Maintenance Protocols

Maintain the baseline 8-lead cohort with strict communication hygiene and zero pressure:

- **Lead-002 (Hyderabad — Festive/Pooja)**:
  - Item: SKU #3 (*Venkatagiri Fine Cotton Jamdani*, ₹8,667)
  - Protocol: Status held as `CHECKOUT_STARTED`. **No repeated follow-ups.** Allow customer full space to complete transfer.
- **Lead-001 (Bangalore — Bridal/Wedding)**:
  - Item: SKU #1 (*Royal Banarasi Zardozi Brocade*, ₹18,999)
  - Protocol: Await family/mother consensus on Kadwa weave and Ruby Red shade against venue lighting. Zero high-pressure sales calls.
- **Lead-003 (Chennai — Daily/Office)**:
  - Item: SKU #6 (*Organic Handloom Linen*, ₹9,800)
  - Protocol: Await customer feedback regarding unstitched blouse piece inclusion and daily office drape comfort.
- **Lead-004 (Delhi — Corporate/Housewarming Gift)**:
  - Item: SKU #8 (*Gadwal Cotton-Silk*, ₹13,200)
  - Protocol: Await gifting committee budget sign-off for Pune destination.
- **Lead-005 (Mumbai — Party/Cocktail)**:
  - Item: SKU #5 (*Hand-Embroidered Organza Pastel*, ₹14,500)
  - Protocol: Await customer evaluation of full 6.3m pure silk cut vs 5.5m boutique alternative.
- **Lead-006 (Pune — Milestone Anniversary)**:
  - Item: SKU #4 (*Pochampally Double Ikat Silk*, ₹24,111)
  - Protocol: Deliver scheduled polite touch base on Saturday post-travel regarding late-October celebration.
- **Lead-008 (Bangalore — Festive Drapes)**:
  - Item: SKU #3 (Cotton, ₹8,667) vs SKU #11 (Tissue, ₹11,800)
  - Protocol: Await drape preference for upcoming family celebration.
- **Lead-007 (Kolkata — Wholesale Reseller)**:
  - Status: `NOT_INTERESTED`
  - Protocol: **DO NOT CONTACT.** Permanent non-contact boundary strictly enforced. Zero outreach to protect direct-to-consumer artisanal retail margins.

---

## 5. Local Services Maintenance Runbook

The local full-stack platform remains available for live demonstrations and staging verification:

```bash
# Check service health:
./manage.sh status

# Probe backend health:
curl -i http://localhost:8081/actuator/health

# Access local storefront:
open http://localhost:5173

# Stop local services when not in use:
./manage.sh stop
```

---

## 6. Strict Quality Safeguards
- **Zero Paid Spend**: Maintain ₹0 marketing and infrastructure spend.
- **Zero Data Fabrication**: Never fabricate DNS propagation, online visits, orders, payments, reviews, or revenue.
- **Zero Artificial Urgency**: Never manufacture countdown timers, fake stock scarcity, or fake discounts.
- **Privacy & Security**: Zero customer PII or API secrets committed to version control.

*(Protocol active. System resting in verified wait state.)*
