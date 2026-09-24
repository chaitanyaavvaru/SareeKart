# SareeKart Day 1 Launch Tracker
**Organic Customer Acquisition & Telemetry Record**
*Authoritative Domain: https://sareekart.com | Date: 2026-09-24*

---

## 1. Factual Telemetry Tracking Table

In strict accordance with our zero-data-fabrication policy, all metrics reflect empirically verified values. Where DNS is pending cutover or analytics tracking has not yet received live traffic, metrics are explicitly labeled **`NOT AVAILABLE`**.

| Metric | Baseline | Day 1 Target | Day 1 Actual | Empirical Evidence / Source | Status |
|---|---:|---:|---:|---|---|
| **Website Visits (Unique)** | 0 | 25–50 | NOT AVAILABLE | Pending registrar DNS cutover (`sareekart.com` on Afternic parking IPs) | Blocked by DNS |
| **Product Detail Views (PDP)** | 0 | 40–80 | NOT AVAILABLE | Pending registrar DNS cutover | Blocked by DNS |
| **Search Queries** | 0 | 10–20 | NOT AVAILABLE | Storefront search telemetry | Blocked by DNS |
| **WhatsApp Enquiries (Inbound)** | 0 | 5–10 | NOT AVAILABLE | WhatsApp Cloud API webhook inbox / Business app | Awaiting outbound share |
| **Add to Cart Events** | 0 | 3–6 | NOT AVAILABLE | Redux `cartSlice` telemetry / session storage | Blocked by DNS |
| **Checkout Started** | 0 | 1–3 | NOT AVAILABLE | `/checkout` route initialization | Blocked by DNS |
| **Payment Attempts** | 0 | 1–2 | NOT AVAILABLE | Razorpay gateway order creation / webhook | Blocked by DNS |
| **Completed Orders** | 0 | 1 | NOT AVAILABLE | MySQL `orders` table (`status = PAID`) | Blocked by DNS |
| **Gross Revenue (INR)** | ₹0.00 | ₹8,000–₹18,000 | ₹0.00 | Payment gateway settlement / verified order records | Baseline Verified |
| **WhatsApp Opt-in Contacts** | 0 | 10–20 | 0 | MySQL `whatsapp_contacts` table (`opted_in = true`) | Baseline Verified |
| **Total Incurred Ad Spend** | ₹0.00 | ₹0.00 | ₹0.00 | Zero-budget organic constraint verified | PASS |

---

## 2. Telemetry Channels & Infrastructure Readiness

| Channel | Tracking Mechanism | Technical State | Telemetry Readiness |
|---|---|---|---|
| **Storefront Web** | Vercel Edge Analytics & Client `eventTracker` | Implemented in `frontend/src/utils/eventTracker.js` | Ready for activation upon DNS cutover |
| **Google Search Console** | DNS TXT verification & XML Sitemap (`/sitemap.xml`) | Blueprint configured; verified in Phase 14 Stage 5 | Pending DNS cutover for Googlebot crawl |
| **WhatsApp Cloud API** | Webhook HMAC verification on `/api/webhook/whatsapp` | Fully tested (33/33 tests PASS in Phase 13/14) | Ready for inbound customer messages |
| **Payment Gateway** | Razorpay Webhook HMAC signature verification | Fully tested (`PaymentProductionVerificationTest` PASS) | Test mode verified; prod credentials required for real cards |
| **Order Management** | Spring Boot JPA `/api/admin/orders` | Fully verified across 752 backend tests | Active in container |

---

## 3. Active Blockers & External Dependencies

1. **Primary Blocker**: Domain Registrar DNS Cutover
   - Current DNS: Apex `@` resolves to `76.223.54.146`, `13.248.169.48` (Afternic Parking).
   - Target DNS: Apex `@ -> 76.76.21.21` (Vercel Edge), CNAME `www -> cname.vercel-dns.com`.
   - Impact: Organic social links to `https://sareekart.com` cannot serve live traffic until the registrar records propagate.
   - Fallback for Day 1: 1-to-1 WhatsApp catalog sharing with high-res saree imagery and direct bank transfer / Razorpay payment link.
