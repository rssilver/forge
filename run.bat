@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM Forge MTG - Windows Launcher Script
REM Usage: run.bat [rebuild]
REM   rebuild  - Rebuild the project before launching (default is to skip and use the existing JAR)
REM ============================================================

cd /d "%~dp0"

echo.
echo ========================================
echo    Forge MTG Launcher
echo ========================================
echo.

REM Only rebuild when 'rebuild' is passed; otherwise launch the existing JAR as-is.
if "%~1"=="rebuild" (
    echo [INFO] Building project...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
    set "JAVACMD_ARGS="
) else (
    echo [INFO] Skipping build. Using existing JAR...
    set "JAVACMD_ARGS=%*"
)

REM Step 2: Sync resources to project root.
REM Always re-sync so a stale/partial root res/ (from an earlier run or a half-finished
REM copy) can't shadow the real assets in forge-gui\res and break skin loading.
echo [INFO] Syncing resource files...
xcopy /E /I /Y /Q forge-gui\res res > nul

REM Step 3: Find the JAR
for %%J in (forge-gui-desktop\target\forge-gui-desktop-*-jar-with-dependencies.jar) do (
    set "JAR=%%~fJ"
)

if not defined JAR (
    echo.
    echo [ERROR] Could not find Forge JAR!
    echo Build the project first; run run.bat rebuild.
    pause
    exit /b 1
)

REM Step 4: Launch
echo [INFO] Starting Forge...
echo.
java -Xmx4g -jar "%JAR%" %JAVACMD_ARGS%

pause
