# ============================================================
# Forge MTG - Windows PowerShell Launcher Script
# Usage: .\run.ps1 [-Rebuild]
#   -Rebuild  Rebuild the project before launching (default is to skip and use the existing JAR)
# ============================================================

param(
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

Write-Output ""
Write-Output "========================================"
Write-Output "   Forge MTG Launcher"
Write-Output "========================================"
Write-Output ""

# Only rebuild when -Rebuild is passed; otherwise launch the existing JAR as-is.
if ($Rebuild) {
    Write-Output "[INFO] Building project..."
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Output ""
        Write-Output "[ERROR] Build failed!"
        exit 1
    }
} else {
    Write-Output "[INFO] Skipping build. Using existing JAR..."
}

# Sync resources to project root.
# Always re-sync so a stale/partial root res/ (from an earlier run or a half-finished
# copy) can't shadow the real assets in forge-gui\res and break skin loading.
Write-Output "[INFO] Syncing resource files..."
$resDest = Join-Path $PSScriptRoot "res"
if (Test-Path $resDest) {
    Remove-Item -Recurse -Force $resDest
}
Copy-Item -Recurse -Force (Join-Path $PSScriptRoot "forge-gui\res") $resDest

# Find the JAR
$jarPattern = "forge-gui-desktop\target\forge-gui-desktop-*-jar-with-dependencies.jar"
$jars = Get-ChildItem -Path (Join-Path $PSScriptRoot $jarPattern) -ErrorAction SilentlyContinue
if (-not $jars) {
    Write-Output ""
    Write-Output "[ERROR] Could not find Forge JAR!"
    Write-Output "Build the project first (run run.ps1 -Rebuild)."
    exit 1
}
$jar = $jars[-1].FullName

Write-Output "[INFO] Starting Forge..."
Write-Output ""

java -Xmx4g -jar $jar @args
