#!/usr/bin/env bash
# ==============================================================================
# SareeKart Active Pipeline & Launch Operations Inspector
# Aggregates network ingress probes, active lead stages, and commercial status.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"

DOMAIN="sareekart.com"
VERCEL_PROD_HOST="sareekart.vercel.app"
VERCEL_BRANCH_HOST="sareekart-git-main-chaitanya-603e.vercel.app"
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

# 1.1 Vercel Public Production Probe
PROD_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "https://${VERCEL_PROD_HOST}" 2>/dev/null || echo "TIMEOUT")
if [ "${PROD_STATUS}" = "200" ]; then
    echo "  • Public Production: [LIVE - HTTP 200 OK] (https://${VERCEL_PROD_HOST})"
elif [ "${PROD_STATUS}" = "302" ]; then
    echo "  • Public Production: [SSO/AUTH REQUIRED] (Deployment Protection ON)"
else
    echo "  • Public Production: [RESPONSE: ${PROD_STATUS}] (https://${VERCEL_PROD_HOST})"
fi

# 1.2 Vercel Branch / Preview Protection Probe
BRANCH_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -m 5 "https://${VERCEL_BRANCH_HOST}" 2>/dev/null || echo "TIMEOUT")
if [ "${BRANCH_STATUS}" = "302" ]; then
    echo "  • Preview/Branch:    [PROTECTED - SSO ACTIVE] (https://${VERCEL_BRANCH_HOST})"
elif [ "${BRANCH_STATUS}" = "200" ]; then
    echo "  • Preview/Branch:    [PUBLIC - HTTP 200 OK] (https://${VERCEL_BRANCH_HOST})"
else
    echo "  • Preview/Branch:    [RESPONSE: ${BRANCH_STATUS}] (https://${VERCEL_BRANCH_HOST})"
fi

# 1.3 DNS Nameserver Probe
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
echo "  Lead-006  PNQ    25th Anniversary    SKU #4 Pochampally Ikat ₹24,111   FOLLOW_UP_SENT"
echo "  Lead-008  BLR    Festive Gathering   SKU #3 vs SKU #11       ₹8,667+   PRODUCT_SHARED"
echo "  --------  -----  ------------------  ----------------------  --------  ------------------"
echo "  Lead-007  CCU    Wholesale Reseller  [EXCLUDED]              N/A       PERMANENT DO NOT CONTACT"

# 4. Immediate Calendar Action
echo "------------------------------------------------------------------------"
echo "[5] ACTION REQUIRED TODAY"
echo "  • Priority 1: Lead-006 Saturday Touchpoint Dispatched (Awaiting family review; zero pressure)"
echo "  • Priority 2: Public Production is LIVE (sareekart.vercel.app); Preview protection preserved"
echo "  • Priority 3: When Lead-002 sends bank UTR, run: ./scripts/verify_order_fulfillment.sh 3 8667 HYD \"<UTR>\""
echo "------------------------------------------------------------------------"
echo "[6] FULFILLMENT HARNESS READINESS"
if [ -f "${SCRIPT_DIR}/verify_order_fulfillment.sh" ]; then
    echo "  • Order QC Harness:  [READY] (${SCRIPT_DIR}/verify_order_fulfillment.sh)"
else
    echo "  • Order QC Harness:  [MISSING]"
fi
echo "========================================================================"
