@echo off
REM Forge MTG Simulator - Build Script (Windows)

echo === Forge Build ===
echo.

set TARGET=compile
if not "%~1"=="" set TARGET=%~1

if "%TARGET%"=="clean" goto :do_clean
if "%TARGET%"=="compile" goto :do_compile
if "%TARGET%"=="package" goto :do_package
if "%TARGET%"=="test" goto :do_test
if "%TARGET%"=="install" goto :do_install
if "%TARGET%"=="run" goto :do_run

echo Usage: build.bat {clean|compile|package|test|install|run}
exit /b 1

:do_clean
echo [clean] Removing build artifacts...
mvn -f pom.xml clean
goto :eof

:do_compile
echo [compile] Building all modules...
mvn -f pom.xml compile -q
echo.
echo Build successful.
goto :eof

:do_package
echo [package] Packaging all modules (skipping tests)...
mvn -f pom.xml package -DskipTests -q
echo.
echo Package complete.
goto :eof

:do_test
echo [test] Running unit tests...
mvn -f pom.xml test -q
echo.
echo Tests passed.
goto :eof

:do_install
echo [install] Installing all modules to local repo...
mvn -f pom.xml install -DskipTests -q
echo.
echo Install complete.
goto :eof

:do_run
echo [run] Compiling and launching Forge desktop...
mvn -f pom.xml compile -q
call run-forge.bat
goto :eof
