# SareeKart Day 7 — DNS Blocker Report
**Technical Domain Routing & Registrar Cutover Telemetry**  
*Authoritative Domain: https://sareekart.com | Date: 2026-09-24*  

---

## 1. Network Telemetry & Empirical Probe Evidence
Probe executed via [`scripts/verify_live_domain.sh`](file:///Users/chaitanyachaitu/Downloads/SareeKart-main/scripts/verify_live_domain.sh):
- **Probe Timestamp**: `2026-09-24T15:16:43Z`
- **Apex Target**: `https://sareekart.com`
- **WWW Target**: `https://www.sareekart.com`
- **Current Apex A Records**: `13.248.169.48`, `76.223.54.146` (Afternic / GoDaddy Parking Network)
- **Current WWW Records**: `13.248.169.48`, `76.223.54.146`
- **HTTP Port 80 Response**: `000TIMEOUT/FAIL` (5s timeout)
- **HTTPS Port 443 Response**: `000TIMEOUT/FAIL` (5s timeout)
- **TLS Certificate Status**: Inactive (host not accepting HTTPS on current parking IP)
- **Cutover Status**: **`[PENDING EXTERNAL REGISTRAR ACTION]`**

---

## 2. Storefront Operational Impact
- **Public Domain Routing**: **`BLOCKED`**
- **Public Storefront Accessibility**: **`BLOCKED`**
- **Policy Compliance**: Zero public traffic, SEO indexing, or apex checkouts are claimed.

---

## 3. Authoritative Target Configuration
The following records must be manually configured in the authoritative domain registrar DNS management console (GoDaddy / Namecheap):

| Record Type | Host / Name | Target Value | Purpose |
|---|---|---|---|
| **A** | `@` (apex) | `76.76.21.21` | Routes root apex to Vercel Global Anycast Edge CDN |
| **CNAME** | `www` | `cname.vercel-dns.com` | Routes www subdomain to Vercel Edge with automatic SSL |

---

## 4. Application-Side Readiness
- **Repository Architecture**: 100% technically ready and green.
- **Frontend SPA**: Production bundle pre-compiled (`dist/assets`), chunk budgets strictly `< 500 kB`, `vercel.json` SPA rewrites configured.
- **Local Services**: Full-stack platform running and verified healthy on localhost (`mysqld` on :3306, Spring Boot on :8081, Vite on :5173).
- **Engineering Guardrail**: No application code changes will be attempted to compensate for pending registrar DNS updates.
