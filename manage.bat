@echo off
SETLOCAL EnableDelayedExpansion

SET "BASE_DIR=%~dp0"
SET "BACKEND_DIR=%BASE_DIR%backend\backend"
SET "FRONTEND_DIR=%BASE_DIR%frontend"

IF "%~1"=="" GOTO usage
IF "%~1"=="start" GOTO start_app
IF "%~1"=="stop" GOTO stop_app
IF "%~1"=="restart" GOTO restart_app
IF "%~1"=="status" GOTO status_app

:usage
echo Usage: manage.bat {start^|stop^|restart^|status}
exit /b 1

:start_app
echo =============================================
echo    Starting SareeKart Application Services   
echo =============================================
echo Starting Backend in a new window...
start "SareeKart Backend" /D "%BACKEND_DIR%" cmd /c "mvnw spring-boot:run"

echo Starting Frontend in a new window...
start "SareeKart Frontend" /D "%FRONTEND_DIR%" cmd /c "npm run dev"

echo =============================================
echo ✔ SareeKart started successfully!
echo   Frontend: http://localhost:5173
echo   Backend API: http://localhost:8081
echo =============================================
exit /b 0

:stop_app
echo =============================================
echo    Stopping SareeKart Application Services   
echo =============================================
echo Killing Java / Spring Boot processes...
taskkill /FI "WINDOWTITLE eq SareeKart Backend*" /T /F >nul 2>&1
taskkill /FI "WINDOWTITLE eq SareeKart Frontend*" /T /F >nul 2>&1
taskkill /IM java.exe /F >nul 2>&1
taskkill /IM node.exe /F >nul 2>&1
echo ✔ All SareeKart application processes stopped.
echo =============================================
exit /b 0

:restart_app
call :stop_app
timeout /t 2 /nobreak >nul
call :start_app
exit /b 0

:status_app
echo =============================================
echo        SareeKart Application Status          
echo =============================================
tasklist /FI "IMAGENAME eq java.exe" | findstr java.exe >nul
if %ERRORLEVEL% equ 0 (
    echo  - Backend: RUNNING
) else (
    echo  - Backend: STOPPED
)
tasklist /FI "IMAGENAME eq node.exe" | findstr node.exe >nul
if %ERRORLEVEL% equ 0 (
    echo  - Frontend: RUNNING
) else (
    echo  - Frontend: STOPPED
)
echo =============================================
exit /b 0
