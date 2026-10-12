# ============================================================
# Forge MTG - Windows PowerShell Launcher Script
# Usage: .\run.ps1 [build]
#   build  - Rebuilds the project before launching (optional)
# ============================================================

$ErrorActionPreference = 'Stop'

Set-Location $PSScriptRoot

Write-Output ""
Write-Output "========================================"
Write-Output "   Forge MTG Launcher"
Write-Output "========================================"
Write-Output ""

$jarPattern = "forge-gui-desktop\target\forge-gui-desktop-*-jar-with-dependencies.jar"

function Get-Jar {
    $matches = Get-ChildItem -Path (Join-Path $PSScriptRoot $jarPattern) -ErrorAction SilentlyContinue
    if ($matches) {
        return $matches[-1].FullName
    }
    return $null
}

$jar = Get-Jar

if (-not $jar) {
    Write-Output "[INFO] JAR not found. Building project..."
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Output ""
        Write-Output "[ERROR] Build failed!"
        exit 1
    }
    $jar = Get-Jar
}

if ($args.Count -gt 0 -and $args[0] -eq "build") {
    Write-Output "[INFO] Rebuilding project..."
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Output ""
        Write-Output "[ERROR] Build failed!"
        exit 1
    }
    $jar = Get-Jar
}

if (-not $jar) {
    Write-Output ""
    Write-Output "[ERROR] Could not find Forge JAR!"
    Write-Output "Run this script with 'build' argument to compile first."
    exit 1
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

Write-Output "[INFO] Starting Forge..."
Write-Output ""

java -Xmx4g -jar $jar @args
