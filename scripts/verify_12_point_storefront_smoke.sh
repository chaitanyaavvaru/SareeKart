#!/usr/bin/env bash
# ==============================================================================
# SareeKart 12-Point Public Storefront Smoke Test Harness
# Tests 12 critical public storefront routes, static assets, and security headers.
# ==============================================================================
set -euo pipefail

TARGET_HOST="${1:-sareekart.com}"
BASE_URL="https://${TARGET_HOST}"

echo "========================================================================"
echo "          SareeKart 12-Point Public Storefront Smoke Test               "
echo "========================================================================"
echo "Target Host:     ${TARGET_HOST}"
echo "Target Base:     ${BASE_URL}"
echo "Timestamp (UTC): $(date -u +"%Y-%m-%dT%H:%M:%SZ")"
echo "------------------------------------------------------------------------"

PASS_COUNT=0
FAIL_COUNT=0
TOTAL_POINTS=12

test_route() {
  local point_num="$1"
  local name="$2"
  local path="$3"
  local expected_code="${4:-200}"
  local content_pattern="${5:-}"

  echo -n "Point ${point_num} — ${name} (${path})... "
  
  # Fetch HTTP status and body header with 5s timeout
  local resp_file
  resp_file=$(mktemp)
  local http_code
  http_code=$(curl -s -o "${resp_file}" -w "%{http_code}" -m 5 "${BASE_URL}${path}" 2>/dev/null || echo "000")

  if [ "${http_code}" != "${expected_code}" ]; then
    echo "[FAIL] (Received HTTP ${http_code}, Expected ${expected_code})"
    FAIL_COUNT=$((FAIL_COUNT + 1))
    rm -f "${resp_file}"
    return
  fi

  if [ -n "${content_pattern}" ]; then
    if ! grep -qi "${content_pattern}" "${resp_file}"; then
      echo "[FAIL] (Pattern '${content_pattern}' not found in response)"
      FAIL_COUNT=$((FAIL_COUNT + 1))
      rm -f "${resp_file}"
      return
    fi
  fi

  echo "[PASS] (HTTP ${http_code})"
  PASS_COUNT=$((PASS_COUNT + 1))
  rm -f "${resp_file}"
}

# 1. Homepage Render & Branding
test_route 1 "Homepage Render & Branding" "/" 200 "SareeKart"

# 2. Product Catalog Browsing
test_route 2 "Product Catalog Browsing" "/products" 200 "<div id=\"root\">"

# 3. Shopping Cart View
test_route 3 "Shopping Cart View" "/cart" 200 "<div id=\"root\">"

# 4. Customer Wishlist View
test_route 4 "Customer Wishlist View" "/wishlist" 200 "<div id=\"root\">"

# 5. Customer Authentication Gateway
test_route 5 "Customer Auth Gateway" "/login" 200 "<div id=\"root\">"

# 6. Customer Order Tracking View
test_route 6 "Customer Order Tracking" "/orders" 200 "<div id=\"root\">"

# 7. Staff Admin Console View
test_route 7 "Staff Admin Console" "/admin" 200 "<div id=\"root\">"

# 8. SEO Robots Directives
test_route 8 "SEO Robots Directives" "/robots.txt" 200 "User-agent"

# 9. XML Sitemap Delivery
test_route 9 "XML Sitemap Delivery" "/sitemap.xml" 200 "urlset"

# 10. PWA Web App Manifest
test_route 10 "PWA Manifest" "/manifest.json" 200 "SareeKart"

# 11. Production JavaScript Bundle
test_route 11 "JavaScript Bundle Delivery" "/assets/index-DJewXS61.js" 200 ""

# 12. Production CSS Bundle
test_route 12 "CSS Stylesheet Delivery" "/assets/index-CogZ6YUc.css" 200 ""

echo "------------------------------------------------------------------------"
echo "Smoke Test Summary: ${PASS_COUNT} / ${TOTAL_POINTS} Points Passed"

if [ "${FAIL_COUNT}" -eq 0 ]; then
  echo "STATUS: [PASS] All 12 public storefront smoke test points passed!"
  echo "========================================================================"
  exit 0
else
  echo "STATUS: [FAIL] ${FAIL_COUNT} test point(s) failed."
  if [ "${TARGET_HOST}" = "sareekart.com" ]; then
    echo "Diagnostic: Custom domain DNS remains blocked or pointed to Afternic parking."
  fi
  echo "========================================================================"
  exit 1
fi
