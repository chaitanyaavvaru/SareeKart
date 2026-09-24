#!/usr/bin/env bash
# ==============================================================================
# SareeKart Production Backup Pruning & Storage Hygiene Utility (Phase 14 · Stage 12)
# Enforces rolling retention window to maintain >= 30% storage headroom on host.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
BACKUP_DIR="${REPO_ROOT}/backups"
MAX_BACKUPS="${1:-7}"

echo "========================================================================"
echo "          SareeKart Production Backup Pruning Utility                   "
echo "========================================================================"
echo "Backup Directory:  ${BACKUP_DIR}"
echo "Retention Policy:  Keep latest ${MAX_BACKUPS} backups"
echo "------------------------------------------------------------------------"

if [ ! -d "${BACKUP_DIR}" ]; then
    echo "Notice: Backup directory ${BACKUP_DIR} does not exist. Nothing to prune."
    exit 0
fi

# Count timestamped backup files (excluding latest symlinks/pointers)
BACKUP_FILES=$(ls -1t "${BACKUP_DIR}"/sareekart_db_[0-9]*.sql 2>/dev/null || true)

if [ -z "${BACKUP_FILES}" ]; then
    echo "Notice: No timestamped backup archives found in ${BACKUP_DIR}."
    exit 0
fi

TOTAL_BACKUPS=$(echo "${BACKUP_FILES}" | wc -l | tr -d ' ')
echo "Current timestamped archives found: ${TOTAL_BACKUPS}"

if [ "${TOTAL_BACKUPS}" -le "${MAX_BACKUPS}" ]; then
    echo "Retention Status: [OK] Backup count (${TOTAL_BACKUPS}) is within limit (${MAX_BACKUPS}). No pruning needed."
    exit 0
fi

PRUNE_COUNT=$((TOTAL_BACKUPS - MAX_BACKUPS))
echo "Pruning ${PRUNE_COUNT} archive(s) older than the latest ${MAX_BACKUPS}..."

echo "${BACKUP_FILES}" | tail -n +"$((MAX_BACKUPS + 1))" | while read -r old_file; do
    if [ -f "${old_file}" ]; then
        file_size=$(wc -c < "${old_file}" | tr -d ' ')
        rm -f "${old_file}"
        echo "  [PRUNED] ${old_file} (${file_size} bytes)"
        if [ -f "${old_file}.sha256" ]; then
            rm -f "${old_file}.sha256"
            echo "  [PRUNED] ${old_file}.sha256"
        fi
    fi
done

REMAINING_COUNT=$(ls -1t "${BACKUP_DIR}"/sareekart_db_[0-9]*.sql 2>/dev/null | wc -l | tr -d ' ')
echo "------------------------------------------------------------------------"
echo "Pruning Status: [SUCCESS] Remaining active backups: ${REMAINING_COUNT}."
echo "========================================================================"
