#!/usr/bin/env pwsh
# Stop All ERP Platform Services (Infrastructure + Applications)

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Red
Write-Host "║  ERP Platform - Shutdown All Services                     ║" -ForegroundColor Red
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Red
Write-Host ""

Write-Host "🛑 Stopping all services..." -ForegroundColor Yellow
Write-Host ""

# Stop application services (Java/Gradle processes)
Write-Host "1️⃣  Stopping application services (Quarkus/Gradle)..." -ForegroundColor Cyan

$javaProcesses = Get-Process -Name java -ErrorAction SilentlyContinue | Where-Object {
    $_.CommandLine -like "*gradle*" -or 
    $_.CommandLine -like "*quarkus*" -or
    $_.MainWindowTitle -like "*8080*" -or
    $_.MainWindowTitle -like "*8081*" -or
    $_.MainWindowTitle -like "*8082*"
}

if ($javaProcesses) {
    Write-Host "   Found $($javaProcesses.Count) Java/Gradle process(es)" -ForegroundColor Yellow
    $javaProcesses | ForEach-Object {
        Write-Host "   • Stopping PID $($_.Id): $($_.ProcessName)" -ForegroundColor Gray
        Stop-Process -Id $_.Id -Force -ErrorAction SilentlyContinue
    }
    Write-Host "   ✅ Application services stopped" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  No application services running" -ForegroundColor Gray
}

Write-Host ""

# Stop Docker infrastructure services
Write-Host "2️⃣  Stopping infrastructure services (Docker)..." -ForegroundColor Cyan

try {
    $dockerRunning = docker ps -q 2>$null
    
    if ($LASTEXITCODE -eq 0 -and $dockerRunning) {
        docker compose -f docker-compose-kafka.yml down
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host "   ✅ Infrastructure services stopped" -ForegroundColor Green
        } else {
            Write-Host "   ⚠️  Some issues stopping infrastructure" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ℹ️  No Docker infrastructure running" -ForegroundColor Gray
    }
} catch {
    Write-Host "   ⚠️  Docker not running or not accessible" -ForegroundColor Yellow
}

Write-Host ""

# Clean up Testcontainers (if any orphaned ones)
Write-Host "3️⃣  Cleaning up Testcontainers..." -ForegroundColor Cyan

$testcontainers = docker ps -a --filter "label=org.testcontainers=true" --format "{{.ID}}" 2>$null

if ($testcontainers) {
    Write-Host "   Found Testcontainer(s), removing..." -ForegroundColor Yellow
    docker rm -f $testcontainers 2>$null
    Write-Host "   ✅ Testcontainers cleaned up" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  No Testcontainers to clean up" -ForegroundColor Gray
}

Write-Host ""

# Show current port usage
Write-Host "4️⃣  Checking port status..." -ForegroundColor Cyan

$port8080 = netstat -ano | Select-String ":8080.*LISTENING"
$port8081 = netstat -ano | Select-String ":8081.*LISTENING"
$port8082 = netstat -ano | Select-String ":8082.*LISTENING"
$port5432 = netstat -ano | Select-String ":5432.*LISTENING"

if (-not $port8080 -and -not $port8081 -and -not $port8082) {
    Write-Host "   ✅ All application ports are free (8080, 8081, 8082)" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Some ports still in use:" -ForegroundColor Yellow
    if ($port8080) { Write-Host "      • Port 8080" -ForegroundColor Yellow }
    if ($port8081) { Write-Host "      • Port 8081" -ForegroundColor Yellow }
    if ($port8082) { Write-Host "      • Port 8082" -ForegroundColor Yellow }
    Write-Host "   💡 They may be released shortly or run: netstat -ano | findstr :80XX" -ForegroundColor Gray
}

if (-not $port5432) {
    Write-Host "   ✅ PostgreSQL port is free (5432)" -ForegroundColor Green
} else {
    Write-Host "   ℹ️  Port 5432 still in use (may be Docker cleanup in progress)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
Write-Host "  ALL SERVICES STOPPED" -ForegroundColor Red
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Red
Write-Host ""
Write-Host "✅ Shutdown complete!" -ForegroundColor Green
Write-Host ""
Write-Host "📌 Note: External PowerShell windows may still be open." -ForegroundColor Yellow
Write-Host "   You can close them manually or they will show the services have stopped." -ForegroundColor Gray
Write-Host ""
Write-Host "🚀 To start again:" -ForegroundColor Cyan
Write-Host "   .\scripts\start-infrastructure.ps1" -ForegroundColor White
Write-Host "   .\scripts\start-all-services.ps1" -ForegroundColor White
Write-Host ""
