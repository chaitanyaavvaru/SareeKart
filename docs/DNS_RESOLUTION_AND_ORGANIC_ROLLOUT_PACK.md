# SareeKart — DNS Cutover Resolution & Organic Rollout Pack
**Authoritative Operational Runbook for Resolving Gate 1 & Launching Organic Operations**  
*Repository: `/Users/chaitanyachaitu/Downloads/SareeKart-main` | Authoritative Domain: `https://sareekart.com`*  
*Budget: Strictly ₹0.00 | Current State: WAITING FOR EXTERNAL TRIGGER*  

---

## 1. Gate 1 Blocker Root-Cause Diagnosis

A fresh authoritative nameserver query (`dig NS sareekart.com`) conducted on **2026-09-25 at 15:21 IST** revealed the exact technical blocker preventing DNS cutover:

```text
;; QUESTION SECTION:
;sareekart.com.         IN  NS

;; ANSWER SECTION:
sareekart.com.  86400   IN  NS  ns1.afternic.com.
sareekart.com.  86400   IN  NS  ns2.afternic.com.
```

### The Root Cause:
The domain `sareekart.com` is currently configured to use **Afternic Parking Nameservers** (`ns1.afternic.com`, `ns2.afternic.com`). Afternic is GoDaddy's domain parking and resale network, which overrides standard DNS records and routes all traffic to their parked lander IPs (`76.223.54.146`, `13.248.169.48`).

Until the nameservers are changed in the registrar dashboard, standard A or CNAME record additions will have no effect.

---

## 2. Step-by-Step Registrar Cutover Guide (GoDaddy)

To resolve Gate 1 and connect `sareekart.com` to the live Vercel deployment:

### Step 1: Switch Nameservers Away from Afternic
1. Log into your registrar account (e.g. [GoDaddy Domain Portfolio](https://dcc.godaddy.com/control/portfolio)).
2. Select **`sareekart.com`** and navigate to **DNS** $\longrightarrow$ **Nameservers**.
3. If nameservers are set to *"I'll use my own nameservers"* pointing to `ns1.afternic.com`:
   - Click **Change Nameservers**.
   - Select **GoDaddy Recommended Nameservers (Default)** to manage DNS records directly in GoDaddy, OR enter custom Vercel nameservers if preferred.
   - Click **Save**.

### Step 2: Configure Vercel Edge Hosting Records
Once default DNS management is active, configure these two DNS records in the DNS Management panel:

| Type | Name / Host | Target / Value | TTL | Purpose |
|---|---|---|---|---|
| **A** | `@` | `76.76.21.21` | `1/2 Hour` (or `600s`) | Routes apex `sareekart.com` to Vercel global edge |
| **CNAME** | `www` | `cname.vercel-dns.com` | `1/2 Hour` (or `600s`) | Routes `www.sareekart.com` to Vercel edge |

*(Remove any leftover parked `A` records pointing to `76.223.54.146` or `13.248.169.48`)*.

### Step 3: Run the Verification Probe
Once saved, verify from the terminal:
```bash
./scripts/verify_live_domain.sh
```
When `CUTOVER STATUS: [LIVE_ACTIVE]` is reported, execute the 12-point storefront smoke test to formally unlock Gate 1.

---

## 3. Week 1 Day 1 Organic Content Pack (Ready to Publish)

While Gate 1 cutover is underway, publish **Day 1 of the 30-Day Organic Content Plan** on Instagram to build awareness with zero ad spend.

### Theme: The Art of the Kadwa Weave
- **Featured Drape**: SKU #1 — *Royal Banarasi Zardozi Brocade Silk Saree* (Ruby Red | ₹18,999)
- **Visual Asset**: Macro video or high-res photo of the gold zari Kadwa floral vines catching natural morning sunlight.
  - Image URL: `https://kankatala.com/cdn/shop/files/1216740117_1.webp?v=1786342018&width=1070`
  - Secondary: `https://images.unsplash.com/photo-1610030470298-3aa5d2c49c71?auto=format&fit=crop&fm=webp&w=800&q=80`

### Ready-to-Copy Instagram Caption:

```text
Look at the reverse side of your Banarasi saree. That is where its true story is told.

In mass-market powerloom drapes, you will often find loose, floating threads on the reverse that catch on your jewellery and feel scratchy against your skin.

In authentic handloom Kadwa weaving from Varanasi, each floral motif is individually woven by hand using a separate shuttle. When the artisan completes the motif, the thread is hand-locked into the warp. Zero floating threads. Pure comfort against your skin.

Featured: Royal Banarasi Zardozi Brocade (SKU #1)
• Fabric: Pure Mulberry Silk
• Weave: Traditional Kadwa Floral Vines
• Color: Deep Ruby Red with Tested Gold Zari
• Dimensions: Full 6.3m (includes 0.8m running blouse piece)
• Price: ₹18,999 (Complimentary insured domestic shipping)

Curious how this drape reflects natural daylight? Send us a message on WhatsApp (+91 9059564499) for a 1-to-1 video walkthrough with our draping stylist.

#SareeKart #BanarasiSilk #KadwaWeave #HandloomSaree #PureSilkSaree #IndianTextiles #VaranasiWeaves #HandwovenIndia #SustainableFashion
```

---

## 4. Saturday Touchpoint Preparation: Lead-006

As documented in the active Lead Tracker, `Lead-006` requested follow-up on Saturday post-travel regarding their 25th milestone anniversary.

- **Customer**: Lead-006 (Pune)
- **Occasion**: 25th Silver Wedding Anniversary (late October)
- **Item**: SKU #4 — *Pochampally Double Ikat Silk Saree* (Emerald Green | ₹24,111)
- **Scheduled Time**: Saturday morning (10:30 AM IST)

### Pre-Drafted Message (Polite, Non-Pushy, Value-Add):

```text
Namaste [Name]! Hope your travels were pleasant and you had a restful week.

As promised, I am checking in briefly regarding your 25th anniversary celebration in late October. 

I took a detailed close-up video of the Pochampally Double Ikat in Emerald Green (SKU #4) under natural morning sunlight today so you can see how the geometric diamond symmetry and subtle woven zari borders catch the light. 

Sharing it here for you and your family to review: [Video Link / Asset]

Please take all the time you need to discuss together. Whenever you are ready or if you have any questions about the 6.3m drape, I am right here to help! Wishing you a wonderful weekend. 🙏
```

---

## 5. Summary of System Safeguards

- **Zero Engineering Churn**: Local and remote git branches remain locked on `main`.
- **Zero Paid Spend**: All operations rely on organic social posting, WhatsApp, and manual relationship management.
- **Strict Factual Accuracy**: Every specification (6.3m length, combed 100s cotton, pure mulberry silk, unstitched blouse piece) matches verified catalog truth.

*(Document active. Standing by for registrar DNS update or customer payment receipt.)*
