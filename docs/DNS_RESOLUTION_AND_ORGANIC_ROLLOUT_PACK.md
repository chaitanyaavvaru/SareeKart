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
| **A** | `@` | `<Vercel Apex IP>` (Check Vercel Dashboard) | `1/2 Hour` (or `600s`) | Routes apex `sareekart.com` to Vercel global edge |
| **CNAME** | `www` | `cname.vercel-dns.com` | `1/2 Hour` (or `600s`) | Routes `www.sareekart.com` to Vercel edge |

> [!IMPORTANT]
> **Dynamic Vercel IP Verification**: Do not blindly copy `76.76.21.21` without verification. Check the active IP address assigned in your Vercel Project Dashboard (`Project Settings` $\to$ `Domains`) at the exact moment of cutover, as Vercel occasionally assigns alternate Anycast edge endpoints. Configure whatever active IP Vercel specifies.
> 
> *(Remove any leftover parked `A` records pointing to `76.223.54.146` or `13.248.169.48`)*.

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
Look at the reverse side of a fine Banarasi drape. That is where its weave story is revealed.

In authentic Varanasi Kadwa weaving, each floral vine motif is individually hand-woven into the mulberry silk warp using dedicated shuttles. The zari motifs are woven directly into the fabric rather than running continuous loose floats across the back, ensuring a smooth, graceful drape.

Featured: Royal Banarasi Zardozi Brocade (SKU #1)
• Fabric: Pure Mulberry Silk
• Weave: Traditional Kadwa Floral Vines
• Color: Ruby Red with Tested Gold Zari
• Dimensions: Full 6.3m (includes 0.8m running blouse piece)
• Price: ₹18,999 (Complimentary insured domestic shipping)

Curious how this drape reflects natural daylight? Send us a message on WhatsApp (+91 9059564499) for a 1-to-1 video walkthrough with our studio team.

#SareeKart #BanarasiSilk #KadwaWeave #HandloomSaree #PureSilkSaree #IndianTextiles #VaranasiWeaves #HandwovenIndia #SustainableFashion
```

---

### Day 2: [Education] The 6.3-Meter Standard — Why 5.5m Commercial Sarees Fall Short
- **Format**: 3-Slide Educational Carousel / Infographic
- **Educational Topic**: Why true luxury handlooms require a full 6.3 meters (5.5m body drape + 0.8m running blouse piece) and a 46-inch width, contrasted against mass-market sarees shortened to 5.2m–5.5m to cut fabric costs.
- **Visual Asset**: Measuring tape on loom showing full 6.3m running length ([Asset Link](https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=1200&q=80))
- **Ready-to-Copy Caption**:
```text
Have you ever draped a saree only to find that you ran out of fabric for the pallu, or had to sacrifice front pleats?

In commercial mass-production, many brands cut costs by reducing saree lengths to 5.2 or 5.5 meters, or omitting the running blouse piece entirely.

At SareeKart, every handloom drape adheres to the uncompromising 6.3-Meter Standard:
• 5.5 Meters of continuous body drape for full, generous front pleats.
• 0.8 Meters of unsevered running blouse piece crafted from the same warp.
• 46-Inch loom width (1.17m) ensuring proper floor-skimming length for all heights.

When you invest in authentic artisan weaving, you never have to compromise on drape grace or pleat fullness.

Explore our collection of authentic 6.3m pure silk and fine cotton handlooms on WhatsApp (+91 9059564499).

#SareeKart #TextileEducation #HandloomSaree #SareeDraping #SareeLength #IndianHandlooms #SustainableFashion #PureSilk
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

## 5. Week 1 Days 3, 4, and 5 Organic Publishing Packs

### Day 3: [Story] The Varanasi Looms — The Art of the Kadwa Floral Vine
- **Format**: Behind-the-scenes storytelling post / reel script
- **Featured Drape**: SKU #1 (*Royal Banarasi Zardozi Brocade*, Ruby Red | ₹18,999)
- **Visual Asset**: Artisan at loom / macro shuttle movement ([Asset Link](https://images.unsplash.com/photo-1621184455862-c163dfb30e0f?auto=format&fit=crop&fm=webp&w=1200&q=80))
- **Ready-to-Copy Caption**:
```text
The beauty of traditional Varanasi weaving lies in the deliberate pace of the handloom.

When you look at the Kadwa floral vines on this Ruby Red Banarasi drape, each petal is individually hand-intertwined using pure mulberry silk and gold zari thread. 

In traditional Kadwa weaving, the artisan uses an independent wooden spool for each floral motif. Rather than printing or stamping patterns onto finished cloth, the design is born directly on the loom as warp and weft meet.

Because each motif is individually woven into the warp, the reverse side remains remarkably neat and flat, feeling gentle against your skin throughout long festive celebrations.

Featured: Royal Banarasi Zardozi Brocade (SKU #1 — ₹18,999)
• 6.3m Total Length (includes 0.8m running blouse piece)
• Pure Mulberry Silk with Tested Gold Zari
• Zero shipping charge across India

Inquire directly on WhatsApp (+91 9059564499) for daylight video clips and drape walkthroughs.

#SareeKart #VaranasiWeavers #KadwaSaree #HandloomHeritage #IndianHandicrafts #ArtisanCraft #BanarasiSilk
```

---

### Day 4: [Product] Venkatagiri Fine Cotton Jamdani — Lightweight Festive Comfort
- **Format**: Single image daylight showcase & drape flow clip
- **Featured Drape**: SKU #3 (*Venkatagiri Fine Cotton Jamdani*, Midnight Black | ₹8,667)
- **Visual Asset**: Saree flowing in morning breeze on clean wooden floor ([Asset Link](https://kankatala.com/cdn/shop/files/1216719148_5.webp?v=1786345549&width=1070))
- **Ready-to-Copy Caption**:
```text
Festive sarees do not have to feel heavy or stiff.

Woven in the historic handloom cluster of Venkatagiri, Andhra Pradesh, this Midnight Black drape is crafted from lightweight fine cotton. It is naturally breathable for afternoon ceremonies and retains its crisp drape from morning rituals to evening dinner.

Embellished with delicate hand-woven gold buttas and a traditional Jamdani pallu, it delivers royal presence with effortless ease of wear.

Product Details:
• SKU #3 — Venkatagiri Fine Cotton Jamdani Saree
• Fabric: Fine Cotton Handloom
• Color: Midnight Black with Fine Gold Buttas & Jamdani Pallu
• Dimensions: Full 6.3m length (includes 0.8m running blouse piece)
• Price: ₹8,667 (Direct fair-trade artisan price, free domestic shipping)

Message us on WhatsApp (+91 9059564499) to view high-resolution daylight draping clips.

#SareeKart #VenkatagiriCotton #JamdaniWeave #CottonHandloom #BlackSaree #SustainableLuxury #HandwovenIndia
```

---

### Day 5: [Education] How to Care for Luxury Handloom Drapes at Home
- **Format**: 3-Slide Infographic / Educational Carousel
- **Educational Topic**: Simple, verified practices to preserve pure silk, fine cotton, and zari luster for generations.
- **Ready-to-Copy Caption**:
```text
Handloom sarees are living heirlooms. With proper care, their colors stay vibrant and their zari luster remains pristine for decades:

1. Breathable Storage:
Store pure silk and fine cotton sarees in breathable unbleached cotton or muslin bags. Avoid plastic zippered covers, which can trap ambient moisture and cause zari oxidation over time.

2. Periodic Refolding:
Every 3 to 4 months, take your sarees out, air them in a shaded, well-ventilated room for an hour, and refold along new creases. This prevents permanent strain on the silk threads along fold lines.

3. Protecting the Zari:
Always fold the saree with the embroidered zari pallu facing inwards. This shields delicate metallic threads from rubbing against harsh surfaces or jewellery during storage.

Every SareeKart drape arrives inside a protective moisture-barrier sleeve and includes our verified 7-day post-delivery inspection policy.

Have questions about caring for a specific weave? Chat with our textile stylists on WhatsApp (+91 9059564499).

#SareeKart #TextileEducation #HandloomCare #SareeMaintenance #PureSilkCare #HandloomPreservation #SareeCareTips
```

---

### Day 6: [Product] Pochampally Double Ikat — Geometric Precision in Silk
- **Format**: High-resolution macro detail reel / single-image focus
- **Featured Drape**: SKU #4 (*Pochampally Double Ikat Silk*, Emerald Green | ₹24,111)
- **Visual Asset**: Macro photograph of the tie-dyed warp and weft diamond junctions ([Asset Link](https://kankatala.com/cdn/shop/files/1216730670_1.webp?v=1786097422&width=1070))
- **Ready-to-Copy Caption**:
```text
Look closely at these Emerald Green diamond motifs.

In traditional Pochampally Double Ikat, both the vertical warp threads and the horizontal weft threads are individually mapped, tied, and resist-dyed before they are mounted on the loom.

As the artisan operates the shuttle, the dyed warp and weft threads align at exact intersections to form crisp geometric structures across the pure mulberry silk drape.

Product Details:
• SKU #4 — Pochampally Double Ikat Silk Saree
• Fabric: Pure Silk Handloom
• Color: Regal Emerald Green with Woven Framing Borders
• Dimensions: Full 6.3m (includes 0.8m running blouse piece)
• Price: ₹24,111 (Direct fair-trade artisan price, free domestic shipping)

Inquire on WhatsApp (+91 9059564499) for a 1-to-1 video drape walkthrough.

#SareeKart #PochampallySilk #DoubleIkat #GeometricSaree #PureSilkSaree #IndianTextiles #HandloomHeritage
```

---

### Day 7: [Story] The 11-Point Light Table QC — How We Protect Your Unboxing
- **Format**: Behind-the-scenes packaging & quality assurance carousel
- **Theme**: SareeKart's verified pre-dispatch inspection protocol
- **Visual Asset**: Flat-lay inspection table with tape measure, pallu detail, and moisture-barrier wrap ([Asset Link](https://images.unsplash.com/photo-1605721911519-3dfeb3be25e7?auto=format&fit=crop&fm=webp&w=1200&q=80))
- **Ready-to-Copy Caption**:
```text
Before any SareeKart drape leaves our studio for your doorstep, it undergoes our verified 11-point inspection:

1. Tape Measure Confirmation: Full 6.3 meters confirmed (5.5m body drape + 0.8m unstitched running blouse, 46-inch width).
2. Warp & Weft Inspection: Flat light-table review ensuring weave continuity and zero reed defects.
3. Zari Finish: Metallic zari borders examined for consistent luster and finish.
4. Selvage Cleanliness: 360-degree review ensuring pristine selvages with zero handling soilage.
5. Protective Packaging: Sealed in moisture-barrier sleeves and heavy-duty cartons with tamper-evident sealing.

We treat every handloom saree as an heirloom—and we inspect it accordingly.

All purchases are backed by our verified 7-day post-delivery inspection policy.

Chat directly with our team on WhatsApp (+91 9059564499) to find the perfect drape for your celebration.

#SareeKart #QualityAssurance #HandloomCare #BehindTheScenes #UnboxingExperience #LuxuryPackaging #HandwovenIndia
```

---

## 6. Complete Customer Consultation Playbooks (The 7 Active Leads)

### Lead-001 (Bangalore — Bridal Kadwa Consultation)
- **Status**: `PRODUCT_SHARED` (SKU #1, ₹18,999)
- **Context**: Awaiting mother & bride consensus on Ruby Red shade against wedding hall lighting.
- **Advisory Touchpoint Script**:
  > *"Namaste [Name]! Hope your wedding planning is progressing smoothly. When selecting a bridal red, lighting makes all the difference: in warm incandescent mandap lighting, our Ruby Red takes on a deep crimson royal tone, while in natural outdoor daylight, the gold Kadwa zari vines shimmer vibrantly. Please take all the time you need with your mother. Whenever you are ready, I can share a short side-by-side video under both lighting conditions! 🙏"*

### Lead-002 (Hyderabad — Festive / Temple Pooja)
- **Status**: `CHECKOUT_STARTED` (SKU #3, ₹8,667)
- **Context**: Awaiting authentic bank payment UTR.
- **Protocol**: Zero repeated follow-ups. Full customer space respected.

### Lead-003 (Chennai — Executive Linen Drape Comfort)
- **Status**: `PRODUCT_SHARED` (SKU #6, ₹9,800)
- **Context**: Inquiring about daily office desk wear and whether organic linen creases uncomfortably.
- **Advisory Touchpoint Script**:
  > *"Namaste [Name]! Regarding your question about daily office wear: unlike stiff heavily starched fabrics, our handspun Bhagalpur organic linen in Natural Ivory is naturally soft and breathable. When sitting at a desk, it forms relaxed ripples rather than sharp creases, keeping you comfortable throughout the day. The 0.8m running blouse piece can be tailored or paired with a contrasting blouse for an effortless executive look!"*

### Lead-004 (Delhi — Corporate Gifting Committee)
- **Status**: `QUALIFIED` (SKU #8, ₹13,200)
- **Context**: Awaiting committee budget sign-off for corporate gifting to Pune destination.
- **Advisory Touchpoint Script**:
  > *"Namaste [Name]! Just touching base regarding your committee's corporate gifting selection. Our Gadwal Cotton-Silk in Mustard Gold (SKU #8) remains one of our most distinguished gifts because it offers universal 6.3m sizing, official GST-compliant tax invoicing, and a lightweight cotton body joined with an authentic silk Kuttu zari border. Let me know if your finance team needs a formal proforma invoice or specimen spec sheet! 🙏"*

### Lead-005 (Mumbai — Hand-Embroidered Organza Drape Comparison)
- **Status**: `FOLLOW_UP_SENT` (SKU #5, ₹14,500)
- **Context**: Evaluating full 6.3m pure silk cut vs boutique 5.5m alternatives.
- **Advisory Touchpoint Script**:
  > *"Namaste [Name]! Hope you are having a lovely weekend. When comparing organza drapes, boutique pieces often measure only 5.2m to 5.5m, making pleats flare stiffly. Our hand-embroidered Dusty Rose organza is cut to a generous 6.3m with tailored blouse fabric, allowing the translucent folds to fall in graceful, soft tiers. Take all the time you need to review!"*

### Lead-006 (Pune — Milestone 25th Anniversary)
- **Status**: `FOLLOW_UP_SCHEDULED` (SKU #4, ₹24,111)
- **Context**: Agreed Saturday morning check-in post-travel. (Touchpoint pre-drafted in Section 4 above).

### Lead-008 (Bangalore — Festive Drapes: Cotton vs Tissue)
- **Status**: `PRODUCT_SHARED` (SKU #3 vs SKU #11, ₹8,667 vs ₹11,800)
- **Context**: Choosing between lightweight breathable cotton and celebratory tissue shimmer.
- **Advisory Touchpoint Script**:
  > *"Namaste [Name]! To help with your choice between the Venkatagiri Cotton (SKU #3) and the Chanderi Tissue (SKU #11): If your family function is during warm morning hours where you will be moving around a lot, the lightweight Venkatagiri fine cotton gives you all-day breathable comfort. If it is an evening reception under chandeliers, the Champagne Silver Chanderi tissue catches artificial lighting with stunning luminescence. Which time of day is the main function?"*

---

## 7. Summary of System Safeguards

- **Zero Engineering Churn**: Local and remote git branches remain locked on `main`.
- **Zero Paid Spend**: All operations rely on organic social posting, WhatsApp, and manual relationship management.
- **Strict Factual Accuracy**: Every specification (6.3m length, fine cotton, pure mulberry silk, unstitched blouse piece) matches verified catalog truth.

*(Document active. Standing by for registrar DNS update or customer payment receipt.)*
