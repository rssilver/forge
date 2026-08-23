@echo off
setlocal enabledelayedexpansion

REM ============================================================
REM Forge MTG - Windows Launcher Script
REM Usage: run-forge.bat [build]
REM   build  - Rebuilds the project before launching (optional)
REM ============================================================

cd /d "%~dp0"

echo.
echo ========================================
echo    Forge MTG Launcher
echo ========================================
echo.

REM Step 1: Build if requested or if JAR doesn't exist
if not exist "forge-gui-desktop\target\forge-gui-desktop-*-jar-with-dependencies.jar" (
    echo [INFO] JAR not found. Building project...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
)

if "%~1"=="build" (
    echo [INFO] Rebuilding project...
    call mvn clean package -DskipTests
    if errorlevel 1 (
        echo.
        echo [ERROR] Build failed!
        pause
        exit /b 1
    )
)

REM Step 2: Copy resources to project root if needed
if not exist "res\skins" (
    echo [INFO] Setting up resource files...
    xcopy /E /I /Y /Q forge-gui\res res > nul
)

REM Step 3: Find the JAR
for %%J in (forge-gui-desktop\target\forge-gui-desktop-*-jar-with-dependencies.jar) do (
    set "JAR=%%~fJ"
)

if not defined JAR (
    echo.
    echo [ERROR] Could not find Forge JAR!
    echo Run this script with 'build' argument to compile first.
    pause
    exit /b 1
)

REM Step 4: Launch
echo [INFO] Starting Forge...
echo.
java -Xmx4g -jar "%JAR%" %*

pause
