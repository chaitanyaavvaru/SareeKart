# SareeKart Production DNS Configuration & Domain Linking Guide

## Overview
This document specifies the exact DNS records required to connect your custom domain `sareekart.com` to the zero-cost production infrastructure (Vercel Edge CDN for Frontend and Render Web Service for Backend).

Currently, visiting `sareekart.com` in a browser returns `ERR_CONNECTION_TIMED_OUT` because DNS records have not yet been pointed to the public cloud hosting providers.

---

## 1. Domain Architecture Topology

```
sareekart.com (Apex Domain)       ──[ A Record: 76.76.21.21 ]──────────► Vercel Edge CDN (Frontend SPA)
www.sareekart.com (Subdomain)     ──[ CNAME: cname.vercel-dns.com ]────► Vercel Edge CDN (Frontend SPA)
api.sareekart.com (Optional)      ──[ CNAME: sareekart-backend.onrender.com ]──► Render Web Service (Spring Boot)
```

---

## 2. Exact DNS Records to Configure

Navigate to the DNS Management console at your domain registrar (e.g., GoDaddy, Namecheap, Google Domains / Squarespace, Cloudflare) and add the following records:

### Record 1: Apex Domain (Root `@`)
| Field | Value | Purpose |
|---|---|---|
| **Type** | `A` | Maps apex domain to Vercel Global Edge IP |
| **Name / Host** | `@` (or leave blank depending on registrar) | Root domain `sareekart.com` |
| **IPv4 Address / Target** | `76.76.21.21` | Vercel Anycast Edge IP |
| **TTL** | `3600` (or Auto / 1 Hour) | Cache lifetime for DNS resolvers |

### Record 2: `www` Subdomain
| Field | Value | Purpose |
|---|---|---|
| **Type** | `CNAME` | Maps www subdomain to Vercel Edge |
| **Name / Host** | `www` | Subdomain `www.sareekart.com` |
| **Target / Value** | `cname.vercel-dns.com.` | Vercel Anycast CNAME Target |
| **TTL** | `3600` (or Auto / 1 Hour) | Standard TTL |

### Record 3: Backend API Custom Subdomain (Optional)
If you wish to route API requests through `api.sareekart.com` rather than the default Render subdomain (`https://sareekart-backend.onrender.com`):
| Field | Value | Purpose |
|---|---|---|
| **Type** | `CNAME` | Directs API requests to Render container |
| **Name / Host** | `api` | Subdomain `api.sareekart.com` |
| **Target / Value** | `<your-service-name>.onrender.com.` | Your Render service URL |
| **TTL** | `3600` | Standard TTL |

---

## 3. Vercel Dashboard Domain Verification Steps

1. In the **Vercel Dashboard**, select your project (`sareekart`).
2. Navigate to **Settings** $\to$ **Domains**.
3. Add `sareekart.com` and `www.sareekart.com`.
4. Vercel will automatically verify the `A` and `CNAME` records once DNS propagation begins (typically 5–30 minutes).
5. Automatic Let's Encrypt TLS/SSL certificates will be provisioned by Vercel at ₹0 cost, enabling HTTPS across the entire storefront.

---

## 4. Verification & Troubleshooting

Once added, verify DNS resolution using `dig` or `nslookup`:

```bash
# Verify Apex Domain
dig +short sareekart.com
# Expected output: 76.76.21.21

# Verify WWW Subdomain
dig +short www.sareekart.com
# Expected output: cname.vercel-dns.com. (or Vercel edge IPs)
```

After propagation completes, `ERR_CONNECTION_TIMED_OUT` will resolve and `https://sareekart.com` will serve the luxury handloom boutique.

---

## 5. Google Search Console & Dynamic Sitemap Submission

### Step A: Google Search Console (GSC) Domain Ownership Verification
To claim authoritative ownership of `sareekart.com` in Google Search Console:

1. **Option 1: DNS TXT Record (Recommended for Domain Property)**:
   Add a TXT record at your domain registrar:
   | Field | Value | Purpose |
   |---|---|---|
   | **Type** | `TXT` | Domain ownership verification |
   | **Name / Host** | `@` (or root) | Root domain `sareekart.com` |
   | **Value** | `google-site-verification=<your-unique-token>` | Provided by Google Search Console |
   | **TTL** | `3600` | Standard TTL |

2. **Option 2: HTML Meta Tag (URL Prefix Property)**:
   Set `VITE_GSC_VERIFICATION=<your-unique-token>` in your Vercel Environment Variables. SareeKart's `SEO.jsx` component and `index.html` will dynamically inject:
   `<meta name="google-site-verification" content="<your-unique-token>" />`.

### Step B: Dynamic Sitemap Submission
Once verified in Google Search Console:
1. Navigate to **Sitemaps** in the GSC left navigation panel.
2. In **Add a new sitemap**, enter: `sitemap.xml` (Full URL: `https://sareekart.com/sitemap.xml`).
3. Click **Submit**.
4. SareeKart's dynamic XML generator projects all active categories and sarees with ISO-8601 `<lastmod>` timestamps and weekly change frequency, accelerating Googlebot crawling and indexation.

---

## 6. Meta (Facebook & Instagram) Domain Verification

To enable Instagram Shopping, Facebook Shop, and product catalog linking under Meta Commerce Manager, you must verify domain ownership of `sareekart.com`.

### Option 1: DNS TXT Record (Recommended)
Add a TXT record in your DNS provider's management console:
| Field | Value | Purpose |
|---|---|---|
| **Type** | `TXT` | Meta domain verification |
| **Name / Host** | `@` (or leave blank depending on registrar) | Root domain `sareekart.com` |
| **Value** | `facebook-domain-verification=<your-meta-verification-code>` | Provided by Meta Business Manager |
| **TTL** | `3600` | Standard TTL |

### Option 2: HTML Meta Tag (Edge CDN Injected)
Alternatively, set the environment variable in Vercel:
```bash
VITE_META_DOMAIN_VERIFICATION=<your-meta-verification-code>
```
SareeKart's `SEO.jsx` will dynamically inject into `<head>`:
```html
<meta name="facebook-domain-verification" content="<your-meta-verification-code>" />
```
Once deployed, click **Verify Domain** inside Meta Business Manager $\to$ **Business Settings** $\to$ **Brand Safety and Suitability** $\to$ **Domains**.

