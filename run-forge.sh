#!/usr/bin/env bash

# ============================================================
# Forge MTG - Unix/Mac Launcher Script
# Usage: ./run-forge.sh [build]
#   build  - Rebuilds the project before launching (optional)
# ============================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo ""
echo "========================================"
echo "   Forge MTG Launcher"
echo "========================================"
echo ""

# Step 1: Build if requested or if JAR doesn't exist
JAR=$(ls forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar 2>/dev/null | head -1)

if [ -z "$JAR" ]; then
    echo "[INFO] JAR not found. Building project..."
    mvn clean package -DskipTests || { echo ""; echo "[ERROR] Build failed!"; exit 1; }
    JAR=$(ls forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar | head -1)
fi

if [ "$1" = "build" ]; then
    echo "[INFO] Rebuilding project..."
    mvn clean package -DskipTests || { echo ""; echo "[ERROR] Build failed!"; exit 1; }
    JAR=$(ls forge-gui-desktop/target/forge-gui-desktop-*-jar-with-dependencies.jar | head -1)
fi

# Step 2: Copy resources to project root if needed
if [ ! -d "res/skins" ]; then
    echo "[INFO] Setting up resource files..."
    cp -r forge-gui/res res
fi

# Step 3: Launch
echo "[INFO] Starting Forge..."
echo ""
java -Xmx4g -jar "$JAR" "$@"
