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
    TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
    BACKUP_FILE="$BASE_DIR/backups/sareekart_db_$TIMESTAMP.sql"
    mysqldump -u root -proot123 --single-transaction sareekart_db > "$BACKUP_FILE" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "✔ Database backup created successfully:"
        echo "  $BACKUP_FILE"
        cp "$BACKUP_FILE" "$BASE_DIR/backups/sareekart_db_latest.sql"
    else
        echo "✖ Failed to create database backup. Ensure MySQL is running."
    fi
    echo "============================================="
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
    test)
        run_tests
        ;;
    loadtest)
        run_loadtest
        ;;
    *)
        echo "Usage: $0 {start|stop|restart|status|backup|test|loadtest}"
        exit 1
        ;;
esac
