#!/usr/bin/env pwsh
# Local verification script - Run before pushing to catch issues early
# Usage: .\verify-local.ps1

$ErrorActionPreference = "Stop"

Write-Host "🔍 Running local verification checks..." -ForegroundColor Cyan
Write-Host ""

# Step 1: Ktlint
Write-Host "📝 Step 1/3: Running ktlint..." -ForegroundColor Yellow
.\gradlew.bat ktlintCheck --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Ktlint failed. Run '.\gradlew.bat ktlintFormat' to fix." -ForegroundColor Red
    exit 1
}
Write-Host "✅ Ktlint passed" -ForegroundColor Green
Write-Host ""

# Step 2: Architecture tests
Write-Host "🏗️  Step 2/3: Running architecture tests..." -ForegroundColor Yellow
.\gradlew.bat :tests:arch:test --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Architecture tests failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Architecture tests passed" -ForegroundColor Green
Write-Host ""

# Step 3: Unit tests (without containers)
Write-Host "🧪 Step 3/3: Running unit tests..." -ForegroundColor Yellow
.\gradlew.bat test -PwithContainers=false --no-daemon
if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Tests failed" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Tests passed" -ForegroundColor Green
Write-Host ""

Write-Host "🎉 All local verification checks passed!" -ForegroundColor Green
Write-Host "You can now commit and push with confidence." -ForegroundColor Cyan
