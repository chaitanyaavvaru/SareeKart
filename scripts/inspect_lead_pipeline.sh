#!/usr/bin/env bash
# ==============================================================================
# SareeKart Active Pipeline & Launch Operations Inspector
# Aggregates network ingress probes, active lead stages, and commercial status.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

DOMAIN="sareekart.com"
VERCEL_HOST="sareekart-git-main-chaitanya-603e.vercel.app"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

echo "========================================================================"
echo "          SareeKart Live Operations & Pipeline Inspector               "
echo "========================================================================"
echo "Timestamp (UTC): ${TIMESTAMP}"
echo "Repository:      ${ROOT_DIR}"
echo "Branch:          $(git -C "${ROOT_DIR}" branch --show-current 2>/dev/null || echo "unknown")"
echo "HEAD Commit:     $(git -C "${ROOT_DIR}" rev-parse --short HEAD 2>/dev/null || echo "unknown")"
echo "------------------------------------------------------------------------"

# 1. Network Ingress Probes
echo "[1] NETWORK & INGRESS GATES"

# 1.1 Vercel Edge Probe
VERCEL_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "https://${VERCEL_HOST}" 2>/dev/null || echo "TIMEOUT")
if [ "${VERCEL_STATUS}" = "200" ]; then
    echo "  • Vercel Edge:       [LIVE - HTTP 200 OK] (https://${VERCEL_HOST})"
elif [ "${VERCEL_STATUS}" = "302" ]; then
    echo "  • Vercel Edge:       [ACTIVE - SSO/AUTH REQUIRED] (Deployment Protection ON)"
    echo "                       -> Action: Toggle 'Vercel Authentication' to Disabled in Vercel settings"
else
    echo "  • Vercel Edge:       [RESPONSE: ${VERCEL_STATUS}] (https://${VERCEL_HOST})"
fi

# 1.2 DNS Nameserver Probe
NS_RECORDS=$(dig +short "${DOMAIN}" NS 2>/dev/null | tr '\n' ' ' || echo "NONE")
if echo "${NS_RECORDS}" | grep -qi "afternic"; then
    echo "  • Gate 1 (DNS):      [BLOCKED] Nameservers pointed to Afternic (${NS_RECORDS})"
    echo "                       -> Action: Switch to GoDaddy Default Nameservers & set Vercel records"
elif [ -z "${NS_RECORDS}" ] || [ "${NS_RECORDS}" = "NONE " ]; then
    echo "  • Gate 1 (DNS):      [UNRESOLVED] No nameservers found"
else
    echo "  • Gate 1 (DNS):      [RESOLVED] Active Nameservers: ${NS_RECORDS}"
fi

# 1.3 Local Stack Health
echo "------------------------------------------------------------------------"
echo "[2] LOCAL FULL-STACK HEALTH"
FE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 2 "http://localhost:5173" 2>/dev/null || echo "DOWN")
BE_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 2 "http://localhost:8081/actuator/health" 2>/dev/null || echo "DOWN")

if [ "${FE_STATUS}" = "200" ]; then
    echo "  • Frontend (:5173):  [ONLINE - HTTP 200 OK]"
else
    echo "  • Frontend (:5173):  [STANDBY / OFFLINE]"
fi

if [ "${BE_STATUS}" = "200" ]; then
    echo "  • Backend (:8081):   [ONLINE - ACTUATOR HEALTH UP]"
else
    echo "  • Backend (:8081):   [STANDBY / OFFLINE]"
fi

# 2. Commercial Financial Ledger
echo "------------------------------------------------------------------------"
echo "[3] COMMERCIAL REVENUE & FINANCIAL DISCIPLINE"
echo "  • Recognized Revenue:   ₹0.00 (Empirical: 0 settled bank orders)"
echo "  • Marketing Spend:      ₹0.00 (Strictly organic ₹0 policy)"
echo "  • Gate 2 (Lead-002):    PENDING (₹8,667 bank transfer for SKU #3 awaiting UTR)"

# 3. Active Lead Pipeline
echo "------------------------------------------------------------------------"
echo "[4] ACTIVE LEAD PIPELINE (ORGANIC CONSULTATIONS)"
echo "  ID        CITY   OCCASION            TARGET SKU              VALUE     STATUS"
echo "  --------  -----  ------------------  ----------------------  --------  ------------------"
echo "  Lead-001  BLR    Bridal Kadwa        SKU #1 Banarasi Brocade ₹18,999   PRODUCT_SHARED"
echo "  Lead-002  HYD    Festive Temple      SKU #3 Venkatagiri Jam. ₹8,667    CHECKOUT_STARTED"
echo "  Lead-003  MAA    Executive Office    SKU #6 Handspun Linen   ₹9,800    PRODUCT_SHARED"
echo "  Lead-004  DEL    Corporate Gifting   SKU #8 Gadwal Silk      ₹13,200   QUALIFIED"
echo "  Lead-005  BOM    Evening Reception   SKU #5 Organza Pastel   ₹14,500   FOLLOW_UP_SENT"
echo "  Lead-006  PNQ    25th Anniversary    SKU #4 Pochampally Ikat ₹24,111   FOLLOW_UP_SCHEDULED"
echo "  Lead-008  BLR    Festive Gathering   SKU #3 vs SKU #11       ₹8,667+   PRODUCT_SHARED"
echo "  --------  -----  ------------------  ----------------------  --------  ------------------"
echo "  Lead-007  CCU    Wholesale Reseller  [EXCLUDED]              N/A       PERMANENT DO NOT CONTACT"

# 4. Immediate Calendar Action
echo "------------------------------------------------------------------------"
echo "[5] ACTION REQUIRED TODAY"
echo "  • Priority 1: Lead-006 Saturday Check-In (Pune, 25th Wedding Anniversary, SKU #4, ₹24,111)"
echo "  • Priority 2: In Vercel Project Settings -> Deployment Protection -> Disable Vercel Auth"
echo "  • Priority 3: When Lead-002 sends bank UTR, run 11-point light-table QC & courier packaging"
echo "========================================================================"
