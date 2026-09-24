#!/usr/bin/env bash
# ==============================================================================
# SareeKart Zero-Cost Operational Health & Readiness Probe Harness (Phase 14 · Stage 8)
# Probes critical production liveness, readiness, catalog, SEO, and commerce endpoints.
# ==============================================================================
set -euo pipefail

BACKEND_URL="${1:-http://localhost:8081}"
FRONTEND_URL="${2:-http://localhost:5173}"

echo "========================================================================"
echo "          SareeKart Production Operational Health Probe Suite           "
echo "========================================================================"
echo "Backend Target:  ${BACKEND_URL}"
echo "Frontend Target: ${FRONTEND_URL}"
echo "------------------------------------------------------------------------"

PASS_COUNT=0
TOTAL_COUNT=7

probe_endpoint() {
    local name="$1"
    local url="$2"
    local expected_code="$3"
    local pattern="$4"

    echo -n "Checking ${name} (${url})... "
    response=$(curl -s -w "\n%{http_code}" --max-time 15 "${url}" || echo -e "\n000")
    http_code=$(echo "${response}" | tail -n 1)
    body=$(echo "${response}" | sed '$d')

    if [ "${http_code}" = "${expected_code}" ]; then
        if [ -n "${pattern}" ]; then
            if echo "${body}" | grep -q "${pattern}"; then
                echo "[PASS] (HTTP ${http_code}, matched '${pattern}')"
                PASS_COUNT=$((PASS_COUNT + 1))
            else
                echo "[FAIL] (HTTP ${http_code}, missing expected pattern '${pattern}')"
            fi
        else
            echo "[PASS] (HTTP ${http_code})"
            PASS_COUNT=$((PASS_COUNT + 1))
        fi
    else
        echo "[FAIL] (Expected HTTP ${expected_code}, got ${http_code})"
    fi
}

echo "Executing non-blocking health matrix..."
# 1. Overall Actuator Health
probe_endpoint "Actuator Health" "${BACKEND_URL}/actuator/health" "200" "\"status\":\"UP\""

# 2. Liveness Probe
probe_endpoint "Liveness Probe" "${BACKEND_URL}/actuator/health/liveness" "200" "\"status\":\"UP\""

# 3. Readiness Probe
probe_endpoint "Readiness Probe" "${BACKEND_URL}/actuator/health/readiness" "200" "\"status\":\"UP\""

# 4. Search Engine Robots Directives
probe_endpoint "Robots.txt" "${BACKEND_URL}/robots.txt" "200" "Disallow: /admin"

# 5. Dynamic XML Sitemap
probe_endpoint "XML Sitemap" "${BACKEND_URL}/sitemap.xml" "200" "<urlset"

# 6. Meta / Instagram Commerce Catalog CSV
probe_endpoint "Meta Catalog Feed" "${BACKEND_URL}/api/meta/catalog.csv" "200" "google_product_category"

# 7. Public Handloom Products Catalog
probe_endpoint "Products Catalog API" "${BACKEND_URL}/api/products" "200" "success"

echo "------------------------------------------------------------------------"
echo "Results: ${PASS_COUNT}/${TOTAL_COUNT} endpoints healthy."

if [ "${PASS_COUNT}" -eq "${TOTAL_COUNT}" ]; then
    echo "Status: [PASS] All core observability and readiness endpoints are active."
    exit 0
else
    echo "Notice: Some endpoints were unavailable (ensure backend service is running)."
    exit 1
fi
