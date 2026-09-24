#!/usr/bin/env bash
# ==============================================================================
# SareeKart Non-Destructive Backup Integrity Verification Utility (Phase 14 · Stage 8)
# Validates checksums and schema integrity without touching live production DB.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${REPO_ROOT}/backups"

echo "========================================================================"
echo "      SareeKart Production Backup Integrity Verification Suite         "
echo "========================================================================"
echo "Backups Directory: ${BACKUP_DIR}"
echo "------------------------------------------------------------------------"

if [ ! -d "${BACKUP_DIR}" ]; then
    echo "Error: Backup directory ${BACKUP_DIR} does not exist." >&2
    exit 1
fi

TOTAL_CHECKED=0
PASSED_CHECKS=0

for sha_file in "${BACKUP_DIR}"/*.sha256; do
    if [ ! -f "${sha_file}" ]; then
        continue
    fi
    TOTAL_CHECKED=$((TOTAL_CHECKED + 1))
    sql_file="${sha_file%.sha256}"
    base_name="$(basename "${sql_file}")"

    if [ ! -f "${sql_file}" ]; then
        echo " [FAIL] Missing corresponding SQL file for: ${sha_file}"
        continue
    fi

    # Read expected checksum
    expected_hash=$(awk '{print $1}' "${sha_file}")

    # Compute actual checksum
    if command -v shasum >/dev/null 2>&1; then
        actual_hash=$(shasum -a 256 "${sql_file}" | awk '{print $1}')
    else
        actual_hash=$(sha256sum "${sql_file}" | awk '{print $1}')
    fi

    if [ "${expected_hash}" != "${actual_hash}" ]; then
        echo " [FAIL] Hash mismatch for ${base_name}:"
        echo "        Expected: ${expected_hash}"
        echo "        Actual:   ${actual_hash}"
        continue
    fi

    # Check that SQL dump is non-empty and contains valid DDL/DML statements
    if ! grep -q -E "(CREATE TABLE|INSERT INTO)" "${sql_file}"; then
        echo " [FAIL] ${base_name} is missing valid MySQL table or insert statements"
        continue
    fi

    file_size=$(wc -c < "${sql_file}" | tr -d ' ')
    echo " [PASS] ${base_name} (${file_size} bytes) — SHA-256 integrity verified."
    PASSED_CHECKS=$((PASSED_CHECKS + 1))
done

echo "------------------------------------------------------------------------"
echo "Summary: ${PASSED_CHECKS}/${TOTAL_CHECKED} backup checkpoints intact."

if [ "${PASSED_CHECKS}" -eq "${TOTAL_CHECKED}" ] && [ "${TOTAL_CHECKED}" -gt 0 ]; then
    echo "Status:  [PASS] All existing database backups verified valid."
    exit 0
else
    echo "Status:  [FAIL] Integrity verification failed or no backups found."
    exit 1
fi
