#!/usr/bin/env bash
# Forge MTG Simulator — Build Script (Unix/macOS)
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

echo "=== Forge Build ==="
echo ""

# Default target: compile everything
TARGET="${1:-compile}"

case "$TARGET" in
  clean)
    echo "[clean] Removing build artifacts..."
    mvn -f pom.xml clean
    ;;
  compile)
    echo "[compile] Building all modules..."
    mvn -f pom.xml compile -q
    echo ""
    echo "Build successful."
    ;;
  package)
    echo "[package] Packaging all modules (skipping tests)..."
    mvn -f pom.xml package -DskipTests -q
    echo ""
    echo "Package complete."
    ;;
  test)
    echo "[test] Running unit tests..."
    mvn -f pom.xml test -q
    echo ""
    echo "Tests passed."
    ;;
  install)
    echo "[install] Installing all modules to local repo..."
    mvn -f pom.xml install -DskipTests -q
    echo ""
    echo "Install complete."
    ;;
  run)
    echo "[run] Compiling and launching Forge desktop..."
    mvn -f pom.xml compile -q
    bash "$SCRIPT_DIR/run-forge.sh"
    ;;
  *)
    echo "Usage: $0 {clean|compile|package|test|install|run}"
    exit 1
    ;;
esac
