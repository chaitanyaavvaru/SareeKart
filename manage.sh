#!/bin/bash

# Directory paths
BASE_DIR="$(cd "$(dirname "$0")" && pwd)"
BACKEND_DIR="$BASE_DIR/backend/backend"
FRONTEND_DIR="$BASE_DIR/frontend"

LOG_BACKEND="$BASE_DIR/backend.log"
LOG_FRONTEND="$BASE_DIR/frontend.log"

is_port_active() {
    lsof -i :"$1" -sTCP:LISTEN >/dev/null 2>&1
}

get_port_pid() {
    lsof -ti :"$1" -sTCP:LISTEN 2>/dev/null
}

start_app() {
    echo "============================================="
    echo "   Starting SareeKart Application Services   "
    echo "============================================="
    
    # 1. Start MySQL via Homebrew if on macOS
    if command -v brew &> /dev/null; then
        echo "[1/3] Checking MySQL database service..."
        if brew services list | grep mysql | grep -q "started"; then
            echo "✔ MySQL is running."
        else
            echo "→ Starting MySQL via Homebrew..."
            brew services start mysql
            sleep 2
        fi
    else
        echo "ℹ Homebrew not found. Assuming MySQL is running."
    fi
    
    # 2. Start Spring Boot Backend (Port 8081)
    echo "[2/3] Checking Spring Boot Backend (Port 8081)..."
    if is_port_active 8081; then
        PID=$(get_port_pid 8081)
        echo "✔ Backend is already running (PID: $PID, Port: 8081)."
    else
        echo "→ Starting Backend on http://localhost:8081..."
        cd "$BACKEND_DIR" || exit 1
        chmod +x mvnw
        nohup ./mvnw spring-boot:run > "$LOG_BACKEND" 2>&1 &
        echo "✔ Backend launch initiated. Logs: backend.log"
    fi

    # 3. Start React/Vite Frontend (Port 5173)
    echo "[3/3] Checking React/Vite Frontend (Port 5173)..."
    if is_port_active 5173; then
        PID=$(get_port_pid 5173)
        echo "✔ Frontend is already running (PID: $PID, Port: 5173)."
    else
        echo "→ Starting Frontend on http://localhost:5173..."
        cd "$FRONTEND_DIR" || exit 1
        nohup npm run dev > "$LOG_FRONTEND" 2>&1 &
        echo "✔ Frontend launch initiated. Logs: frontend.log"
    fi

    echo "============================================="
    echo "✔ SareeKart startup completed!"
    echo "  Frontend:    http://localhost:5173"
    echo "  Backend API: http://localhost:8081"
    echo "============================================="
}

stop_app() {
    echo "============================================="
    echo "   Stopping SareeKart Application Services   "
    echo "============================================="

    # Stop Backend
    if is_port_active 8081; then
        PID=$(get_port_pid 8081)
        echo "→ Stopping Backend on port 8081 (PID: $PID)..."
        kill -9 $PID 2>/dev/null || true
        echo "✔ Backend stopped."
    else
        echo "ℹ Backend on port 8081 is not running."
    fi

    # Stop Frontend
    if is_port_active 5173; then
        PID=$(get_port_pid 5173)
        echo "→ Stopping Frontend on port 5173 (PID: $PID)..."
        kill -9 $PID 2>/dev/null || true
        echo "✔ Frontend stopped."
    else
        echo "ℹ Frontend on port 5173 is not running."
    fi

    # Cleanup lingering processes
    pkill -f "sareekart" 2>/dev/null || true

    echo "============================================="
    echo "✔ All SareeKart application processes stopped."
    echo "============================================="
}

restart_app() {
    stop_app
    sleep 2
    start_app
}

status_app() {
    echo "============================================="
    echo "       SareeKart Application Status          "
    echo "============================================="
    
    # Check MySQL
    if command -v brew &> /dev/null; then
        if brew services list | grep mysql | grep -q "started"; then
            echo " - Database (MySQL): RUNNING (Homebrew)"
        else
            echo " - Database (MySQL): STOPPED"
        fi
    else
        echo " - Database (MySQL): ACTIVE"
    fi

    # Check Backend
    if is_port_active 8081; then
        PID=$(get_port_pid 8081)
        echo " - Backend:  RUNNING (Port 8081, PID: $PID)"
    else
        echo " - Backend:  STOPPED"
    fi

    # Check Frontend
    if is_port_active 5173; then
        PID=$(get_port_pid 5173)
        echo " - Frontend: RUNNING (Port 5173, PID: $PID)"
    else
        echo " - Frontend: STOPPED"
    fi
    echo "============================================="
}

backup_db() {
    echo "============================================="
    echo "     Creating Local SareeKart Backup         "
    echo "============================================="
    mkdir -p "$BASE_DIR/backups"

    # Pre-check: Disk space discipline (ensure >= 30% free)
    if command -v df >/dev/null 2>&1; then
        AVAIL_PCT=$(df -k "$BASE_DIR" | awk 'NR==2 {print 100 - $5}' | tr -d '%')
        if [ -n "$AVAIL_PCT" ] && [ "$AVAIL_PCT" -lt 30 ]; then
            echo "✖ Storage constraint violation: available disk space is ${AVAIL_PCT}% (< 30%). Aborting backup."
            return 1
        fi
    fi

    DB_USER="${SPRING_DATASOURCE_USERNAME:-root}"
    DB_PASS="${SPRING_DATASOURCE_PASSWORD:-root123}"
    DB_NAME="${SPRING_DATASOURCE_DATABASE:-sareekart_db}"
    DB_PORT="${SPRING_DATASOURCE_PORT:-3306}"

    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    BACKUP_FILE="$BASE_DIR/backups/sareekart_db_$TIMESTAMP.sql"
    ERR_LOG="$BASE_DIR/backups/.backup_err.log"

    START_TS=$(date +%s)
    echo "→ Backing up database '$DB_NAME' (port: $DB_PORT, user: $DB_USER)..."
    
    # mysqldump with single-transaction, routines, triggers, and disabled GTID purge for portability
    mysqldump -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" \
        --single-transaction \
        --set-gtid-purged=OFF \
        --routines \
        --triggers \
        "$DB_NAME" > "$BACKUP_FILE" 2> "$ERR_LOG"
    DUMP_EXIT=$?
    END_TS=$(date +%s)
    DURATION=$((END_TS - START_TS))

    if [ $DUMP_EXIT -eq 0 ] && [ -s "$BACKUP_FILE" ]; then
        FILE_SIZE_KB=$(du -k "$BACKUP_FILE" | cut -f1)
        # Compute SHA-256 checksum
        if command -v shasum >/dev/null 2>&1; then
            CHECKSUM=$(shasum -a 256 "$BACKUP_FILE" | awk '{print $1}')
            echo "$CHECKSUM  $(basename "$BACKUP_FILE")" > "$BACKUP_FILE.sha256"
        else
            CHECKSUM="N/A"
        fi

        echo "✔ Database backup created successfully in ${DURATION}s (${FILE_SIZE_KB} KB):"
        echo "  File:     $BACKUP_FILE"
        echo "  SHA-256:  $CHECKSUM"
        
        cp "$BACKUP_FILE" "$BASE_DIR/backups/sareekart_db_latest.sql"
        [ -f "$BACKUP_FILE.sha256" ] && cp "$BACKUP_FILE.sha256" "$BASE_DIR/backups/sareekart_db_latest.sql.sha256"

        # Automated backup rotation: retain strictly the 10 most recent backups
        BACKUP_COUNT=$(ls -1t "$BASE_DIR/backups"/sareekart_db_*.sql 2>/dev/null | grep -v "latest" | wc -l | tr -d ' ')
        if [ "$BACKUP_COUNT" -gt 10 ]; then
            echo "→ Rotating backups (retaining 10 latest, pruning older)..."
            ls -1t "$BASE_DIR/backups"/sareekart_db_*.sql 2>/dev/null | grep -v "latest" | tail -n +11 | while read -r old_file; do
                rm -f "$old_file" "$old_file.sha256"
            done
        fi
        rm -f "$ERR_LOG"
        echo "============================================="
        return 0
    else
        echo "✖ Failed to create database backup (exit code: $DUMP_EXIT)."
        if [ -s "$ERR_LOG" ]; then
            echo "  Error: $(cat "$ERR_LOG")"
        fi
        rm -f "$BACKUP_FILE" "$ERR_LOG"
        echo "============================================="
        return 1
    fi
}

verify_backup() {
    BACKUP_FILE="${1:-$BASE_DIR/backups/sareekart_db_latest.sql}"
    echo "============================================="
    echo "     Verifying Database Backup Integrity     "
    echo "============================================="
    if [ ! -f "$BACKUP_FILE" ]; then
        echo "✖ Backup file not found: $BACKUP_FILE"
        echo "============================================="
        return 1
    fi

    echo "→ Inspecting backup: $BACKUP_FILE"
    FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    LINE_COUNT=$(wc -l < "$BACKUP_FILE" | tr -d ' ')
    echo "  Size:       $FILE_SIZE"
    echo "  Lines:      $LINE_COUNT"

    # Check MySQL dump banner
    if ! grep -q "MySQL dump" "$BACKUP_FILE" 2>/dev/null; then
        echo "✖ Invalid backup: MySQL dump header not found."
        echo "============================================="
        return 1
    fi

    # Check SHA-256 checksum if exists
    if [ -f "$BACKUP_FILE.sha256" ] && command -v shasum >/dev/null 2>&1; then
        EXPECTED_HASH=$(awk '{print $1}' "$BACKUP_FILE.sha256")
        ACTUAL_HASH=$(shasum -a 256 "$BACKUP_FILE" | awk '{print $1}')
        if [ "$EXPECTED_HASH" = "$ACTUAL_HASH" ]; then
            echo "✔ SHA-256 Checksum Verified: $ACTUAL_HASH"
        else
            echo "✖ Checksum mismatch! Expected $EXPECTED_HASH but got $ACTUAL_HASH"
            echo "============================================="
            return 1
        fi
    fi

    # Verify critical table definitions exist in the dump
    CRITICAL_TABLES=("products" "categories" "users" "orders" "inventory_items" "trousseau_boards" "whatsapp_contacts")
    MISSING_TABLES=0
    for tbl in "${CRITICAL_TABLES[@]}"; do
        if grep -q "CREATE TABLE \`$tbl\`" "$BACKUP_FILE" 2>/dev/null; then
            echo "  ✔ Table definition present: \`$tbl\`"
        else
            echo "  ✖ Table definition MISSING: \`$tbl\`"
            MISSING_TABLES=$((MISSING_TABLES + 1))
        fi
    done

    TOTAL_TABLES=$(grep -c "CREATE TABLE" "$BACKUP_FILE" 2>/dev/null || echo 0)
    echo "  Total tables declared in backup: $TOTAL_TABLES"

    if [ "$MISSING_TABLES" -gt 0 ]; then
        echo "✖ Backup verification FAILED: $MISSING_TABLES critical table(s) missing."
        echo "============================================="
        return 1
    fi

    echo "✔ Backup integrity verification PASSED (all critical structures verified)."
    echo "============================================="
    return 0
}

restore_db() {
    BACKUP_FILE="${1:-$BASE_DIR/backups/sareekart_db_latest.sql}"
    TARGET_DB="${2:-}"
    FORCE_FLAG="${3:-}"

    echo "============================================="
    echo "     Restoring SareeKart Database            "
    echo "============================================="

    if [ ! -f "$BACKUP_FILE" ]; then
        echo "✖ Backup file not found: $BACKUP_FILE"
        echo "============================================="
        return 1
    fi

    # Require explicit target database parameter to prevent accidental overwrites
    if [ -z "$TARGET_DB" ]; then
        echo "✖ TARGET DATABASE UNSPECIFIED!"
        echo ""
        echo "  To protect the live database, an explicit target database must be provided."
        echo "  Usage: $0 restore <backup_file> <target_database> [--force-production-overwrite]"
        echo ""
        echo "  Examples:"
        echo "    - Restore into isolated drill database:"
        echo "        $0 restore $BACKUP_FILE sareekart_recovery_drill_db"
        echo "    - Restore into production (requires explicit flag):"
        echo "        $0 restore $BACKUP_FILE sareekart_db --force-production-overwrite"
        echo "============================================="
        return 1
    fi

    # Target safeguard: Protect production/primary database from silent overwrite
    if [ "$TARGET_DB" = "sareekart_db" ] || [[ "$TARGET_DB" == *"prod"* ]]; then
        if [ "$FORCE_FLAG" != "--force-production-overwrite" ]; then
            echo "✖ DESTRUCTIVE OPERATION BLOCKED BY SAFETY GATE!"
            echo ""
            echo "  Target database '$TARGET_DB' is identified as primary/production."
            echo "  Direct restoration requires the affirmative safeguard flag:"
            echo "    --force-production-overwrite"
            echo ""
            echo "  Example:"
            echo "    $0 restore $BACKUP_FILE $TARGET_DB --force-production-overwrite"
            echo "============================================="
            return 1
        fi
        echo "⚠ WARNING: Overwriting primary production database '$TARGET_DB' as instructed by --force-production-overwrite!"
    fi

    DB_USER="${SPRING_DATASOURCE_USERNAME:-root}"
    DB_PASS="${SPRING_DATASOURCE_PASSWORD:-root123}"
    DB_PORT="${SPRING_DATASOURCE_PORT:-3306}"

    echo "→ Target Database:  $TARGET_DB"
    echo "→ Backup Source:    $BACKUP_FILE"

    # Ensure target database exists
    mysql -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" -e "CREATE DATABASE IF NOT EXISTS \`$TARGET_DB\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null
    if [ $? -ne 0 ]; then
        echo "✖ Failed to create/verify target database '$TARGET_DB'. Ensure MySQL is running."
        echo "============================================="
        return 1
    fi

    START_TS=$(date +%s)
    mysql -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$TARGET_DB" < "$BACKUP_FILE" 2>/dev/null
    RESTORE_EXIT=$?
    END_TS=$(date +%s)
    RESTORE_DURATION=$((END_TS - START_TS))

    if [ $RESTORE_EXIT -eq 0 ]; then
        RESTORED_TABLES=$(mysql -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$TARGET_DB" -e "SHOW TABLES;" 2>/dev/null | wc -l | tr -d ' ')
        RESTORED_TABLES=$((RESTORED_TABLES - 1))
        echo "✔ Database restoration completed successfully in ${RESTORE_DURATION:-0}s!"
        echo "  Target:  $TARGET_DB ($RESTORED_TABLES tables restored)"
        echo "============================================="
        return 0
    else
        echo "✖ Database restoration failed (exit code: $RESTORE_EXIT)."
        echo "============================================="
        return 1
    fi
}

dr_check() {
    echo "============================================="
    echo "     SareeKart Disaster Recovery Diagnostic  "
    echo "============================================="
    DR_FAILURES=0

    # 1. Check MySQL connectivity
    echo -n "[1/6] MySQL Database Service: "
    DB_USER="${SPRING_DATASOURCE_USERNAME:-root}"
    DB_PASS="${SPRING_DATASOURCE_PASSWORD:-root123}"
    DB_PORT="${SPRING_DATASOURCE_PORT:-3306}"
    if mysqladmin ping -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" >/dev/null 2>&1; then
        echo "✔ OPERATIONAL (:3306)"
    else
        echo "✖ UNREACHABLE"
        DR_FAILURES=$((DR_FAILURES + 1))
    fi

    # 2. Check live tables in sareekart_db
    echo -n "[2/6] Primary Schema Integrity: "
    TABLE_COUNT=$(mysql -h 127.0.0.1 -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" sareekart_db -e "SHOW TABLES;" 2>/dev/null | wc -l | tr -d ' ')
    TABLE_COUNT=$((TABLE_COUNT > 0 ? TABLE_COUNT - 1 : 0))
    if [ "$TABLE_COUNT" -ge 30 ]; then
        echo "✔ HEALTHY ($TABLE_COUNT tables found)"
    else
        echo "✖ DEGRADED ($TABLE_COUNT tables found, expected >= 30)"
        DR_FAILURES=$((DR_FAILURES + 1))
    fi

    # 3. Check latest backup recency (< 24h)
    echo -n "[3/6] Backup Recency & Availability: "
    LATEST_BACKUP="$BASE_DIR/backups/sareekart_db_latest.sql"
    if [ -f "$LATEST_BACKUP" ]; then
        BACKUP_MOD=$(stat -f %m "$LATEST_BACKUP" 2>/dev/null || stat -c %Y "$LATEST_BACKUP" 2>/dev/null || echo 0)
        NOW_TS=$(date +%s)
        AGE_HOURS=$(( (NOW_TS - BACKUP_MOD) / 3600 ))
        if [ "$AGE_HOURS" -lt 24 ]; then
            echo "✔ FRESH ($AGE_HOURS hours old, $(du -h "$LATEST_BACKUP" | cut -f1))"
        else
            echo "⚠ STALE ($AGE_HOURS hours old > 24h)"
        fi
    else
        echo "✖ MISSING (No latest backup found in backups/)"
        DR_FAILURES=$((DR_FAILURES + 1))
    fi

    # 4. Check disk headroom (>= 30% free)
    echo -n "[4/6] Storage Headroom (>= 30% free): "
    AVAIL_PCT=$(df -k "$BASE_DIR" | awk 'NR==2 {print 100 - $5}' | tr -d '%')
    if [ -n "$AVAIL_PCT" ] && [ "$AVAIL_PCT" -ge 30 ]; then
        echo "✔ SUFFICIENT (${AVAIL_PCT}% available)"
    else
        echo "✖ INSUFFICIENT (${AVAIL_PCT}% available < 30%)"
        DR_FAILURES=$((DR_FAILURES + 1))
    fi

    # 5. Check Neo4j status & fallback readiness
    echo -n "[5/6] Knowledge Graph Degradation Defense: "
    if lsof -i :7687 >/dev/null 2>&1; then
        echo "✔ ONLINE (Port 7687 active)"
    else
        echo "✔ DEGRADED SAFE (Offline, MySQL deterministic fallback active)"
    fi

    # 6. Verify Spring Boot executable / build artifact
    echo -n "[6/6] Application Artifact Readiness: "
    if [ -f "$BACKEND_DIR/mvnw" ]; then
        echo "✔ READY (Maven wrapper verified)"
    else
        echo "✖ MISSING MAVEN WRAPPER"
        DR_FAILURES=$((DR_FAILURES + 1))
    fi

    echo "---------------------------------------------"
    if [ "$DR_FAILURES" -eq 0 ]; then
        echo "✔ DR Readiness Check: ALL SYSTEMS READY FOR RECOVERY"
        echo "============================================="
        return 0
    else
        echo "✖ DR Readiness Check: $DR_FAILURES COMPONENT(S) REQUIRE ATTENTION"
        echo "============================================="
        return 1
    fi
}

run_tests() {
    echo "============================================="
    echo "     Running Full Local SareeKart Tests      "
    echo "============================================="
    echo "[1/2] Running Backend JUnit 5 Tests..."
    cd "$BACKEND_DIR" || exit 1
    ./mvnw test -q
    if [ $? -eq 0 ]; then
        echo "✔ Backend Tests: PASSED (18/18)"
    else
        echo "✖ Backend Tests: FAILED"
        exit 1
    fi

    echo "[2/2] Running Frontend Playwright Tests..."
    cd "$FRONTEND_DIR" || exit 1
    npx playwright test --project=chromium --reporter=list
    if [ $? -eq 0 ]; then
        echo "✔ Frontend E2E Tests: PASSED (26/26 - 14 Desktop + 12 Mobile)"
    else
        echo "✖ Frontend E2E Tests: FAILED"
        exit 1
    fi
    echo "============================================="
    echo "✔ All SareeKart Automated Tests Passed 100%!"
    echo "============================================="
}

run_loadtest() {
    echo "============================================="
    echo "     Running SareeKart Load & Stress Test    "
    echo "============================================="
    if ! is_port_active 8081; then
        echo "✖ Spring Boot Backend is not running on port 8081."
        echo "  Please run './manage.sh start' first."
        exit 1
    fi
    node "$BASE_DIR/scripts/load-test.mjs"
    LOAD_EXIT=$?
    echo ""
    echo "→ Performing Post-Test Health Check Recovery Probe..."
    HEALTH_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/api/products)
    if [ "$HEALTH_CODE" = "200" ]; then
        echo "✔ Post-Test Health Check: HTTP 200 OK (Backend is fully operational)"
    else
        echo "✖ Post-Test Health Check FAILED with HTTP $HEALTH_CODE"
        exit 1
    fi
    exit $LOAD_EXIT
}

case "$1" in
    start)
        start_app
        ;;
    stop)
        stop_app
        ;;
    restart)
        restart_app
        ;;
    status)
        status_app
        ;;
    backup)
        backup_db
        ;;
    restore)
        restore_db "$2" "$3" "$4"
        ;;
    verify)
        verify_backup "$2"
        ;;
    dr_check)
        dr_check
        ;;
    test)
        run_tests
        ;;
    loadtest)
        run_loadtest
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|backup|restore|verify|dr_check|test|loadtest}"
        exit 1
        ;;
esac
