#!/usr/bin/env pwsh
# Start All ERP Platform Application Services
# Starts Tenancy-Identity, API Gateway, and Finance services in separate windows

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║  ERP Platform - Application Services Startup              ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta
Write-Host ""

# Check if infrastructure is running
Write-Host "🔍 Checking infrastructure services..." -ForegroundColor Yellow

$postgresRunning = docker ps --filter "name=erp-postgres" --filter "status=running" --format "{{.Names}}" 2>$null
$redisRunning = docker ps --filter "name=erp-redis" --filter "status=running" --format "{{.Names}}" 2>$null
$redpandaRunning = docker ps --filter "name=erp-redpanda" --filter "status=running" --format "{{.Names}}" 2>$null

if (-not $postgresRunning -or -not $redisRunning -or -not $redpandaRunning) {
    Write-Host "❌ Infrastructure services are not running!" -ForegroundColor Red
    Write-Host ""
    Write-Host "   Missing services:" -ForegroundColor Yellow
    if (-not $postgresRunning) { Write-Host "   • PostgreSQL (erp-postgres)" -ForegroundColor Red }
    if (-not $redisRunning) { Write-Host "   • Redis (erp-redis)" -ForegroundColor Red }
    if (-not $redpandaRunning) { Write-Host "   • Redpanda/Kafka (erp-redpanda)" -ForegroundColor Red }
    Write-Host ""
    Write-Host "   Please run: .\scripts\start-infrastructure.ps1" -ForegroundColor Cyan
    Write-Host ""
    exit 1
}

Write-Host "✅ Infrastructure services are running" -ForegroundColor Green
Write-Host ""

# Check if services are already running and stop them
Write-Host "🔍 Checking for existing application services..." -ForegroundColor Yellow

$port8081Used = netstat -ano | Select-String ":8081.*LISTENING"
$port8080Used = netstat -ano | Select-String ":8080.*LISTENING"
$port8082Used = netstat -ano | Select-String ":8082.*LISTENING"

if ($port8081Used -or $port8080Used -or $port8082Used) {
    Write-Host "⚠️  Detected running services on ports:" -ForegroundColor Yellow
    if ($port8081Used) { Write-Host "   • Port 8081 (Tenancy-Identity)" -ForegroundColor Yellow }
    if ($port8080Used) { Write-Host "   • Port 8080 (API Gateway)" -ForegroundColor Yellow }
    if ($port8082Used) { Write-Host "   • Port 8082 (Finance)" -ForegroundColor Yellow }
    Write-Host ""
    Write-Host "   Stopping previous services..." -ForegroundColor Cyan
    
    # Stop Java/Gradle processes
    $javaProcesses = Get-Process -Name "java","gradle*" -ErrorAction SilentlyContinue | 
        Where-Object { $_.Path -like "*gradle*" -or $_.Path -like "*java*" }
    
    if ($javaProcesses) {
        foreach ($proc in $javaProcesses) {
            try {
                Write-Host "   • Stopping PID $($proc.Id): $($proc.ProcessName)" -ForegroundColor Gray
                Stop-Process -Id $proc.Id -Force -ErrorAction SilentlyContinue
            } catch {
                Write-Host "   ⚠️  Could not stop PID $($proc.Id)" -ForegroundColor Yellow
            }
        }
        Start-Sleep -Seconds 2
        Write-Host "   ✅ Previous services stopped" -ForegroundColor Green
    }
    Write-Host ""
}

Write-Host "🚀 Starting application services in external windows..." -ForegroundColor Cyan
Write-Host ""

# Start Tenancy-Identity Service (Port 8081)
Write-Host "1️⃣  Starting Tenancy-Identity Service (Port 8081)..." -ForegroundColor Cyan
Start-Process pwsh -ArgumentList "-NoExit", "-Command", @"
`$host.UI.RawUI.WindowTitle='TENANCY-IDENTITY (8081)';
`$env:QUARKUS_HTTP_PORT='8081';
`$env:QUARKUS_ANALYTICS_DISABLED='true';
cd '$PWD';
Write-Host '╔════════════════════════════════════════╗' -ForegroundColor Cyan;
Write-Host '║  TENANCY-IDENTITY SERVICE (Port 8081) ║' -ForegroundColor Cyan;
Write-Host '╚════════════════════════════════════════╝' -ForegroundColor Cyan;
Write-Host '';
Write-Host 'Starting service...' -ForegroundColor Cyan;
Write-Host 'Database: erp-postgres (erp_user/erp_pass on localhost:5432)' -ForegroundColor Gray;
Write-Host '';
./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev
"@

Start-Sleep -Seconds 2

# Start API Gateway (Port 8080)
Write-Host "2️⃣  Starting API Gateway (Port 8080)..." -ForegroundColor Green
Start-Process pwsh -ArgumentList "-NoExit", "-Command", @"
`$host.UI.RawUI.WindowTitle='API-GATEWAY (8080)';
`$env:QUARKUS_HTTP_PORT='8080';
`$env:QUARKUS_ANALYTICS_DISABLED='true';
`$env:FINANCE_SERVICE_URL='http://localhost:8082';
cd '$PWD';
Write-Host '╔════════════════════════════════════╗' -ForegroundColor Green;
Write-Host '║  API GATEWAY SERVICE (Port 8080)  ║' -ForegroundColor Green;
Write-Host '╚════════════════════════════════════╝' -ForegroundColor Green;
Write-Host '';
Write-Host 'Starting service...' -ForegroundColor Cyan;
Write-Host 'Redis: localhost:6379, Kafka: localhost:19092' -ForegroundColor Gray;
Write-Host '';
./gradlew :api-gateway:quarkusDev
"@

Start-Sleep -Seconds 2

# Start Finance Service (Port 8082)
Write-Host "3️⃣  Starting Finance Service (Port 8082)..." -ForegroundColor Yellow
Start-Process pwsh -ArgumentList "-NoExit", "-Command", @"
`$host.UI.RawUI.WindowTitle='FINANCE (8082)';
`$env:FINANCE_HTTP_PORT='8082';
`$env:QUARKUS_ANALYTICS_DISABLED='true';
cd '$PWD';
Write-Host '╔═══════════════════════════════════════════════════╗' -ForegroundColor Yellow;
Write-Host '║  FINANCIAL ACCOUNTING SERVICE (Port 8082)        ║' -ForegroundColor Yellow;
Write-Host '║  Phase 5A: Multi-Currency + Revaluation         ║' -ForegroundColor Yellow;
Write-Host '╚═══════════════════════════════════════════════════╝' -ForegroundColor Yellow;
Write-Host '';
Write-Host 'Starting service with Testcontainers...' -ForegroundColor Cyan;
Write-Host 'This will start ephemeral PostgreSQL and Kafka containers.' -ForegroundColor Gray;
Write-Host 'Database: postgres/postgres (auto-configured by DevServices)' -ForegroundColor Gray;
Write-Host '';
./gradlew :bounded-contexts:financial-management:financial-accounting:accounting-infrastructure:quarkusDev
"@

Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
Write-Host "  APPLICATION SERVICES STARTING" -ForegroundColor Magenta
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
Write-Host ""
Write-Host "✅ Three external PowerShell windows have been launched:" -ForegroundColor Green
Write-Host ""
Write-Host "   1. 🔐 TENANCY-IDENTITY (8081) - Cyan window" -ForegroundColor Cyan
Write-Host "   2. 🌐 API-GATEWAY (8080) - Green window" -ForegroundColor Green
Write-Host "   3. 💰 FINANCE (8082) - Yellow window" -ForegroundColor Yellow
Write-Host ""
Write-Host "⏱️  Expected startup times:" -ForegroundColor Cyan
Write-Host "   • Tenancy-Identity:  30-60 seconds" -ForegroundColor White
Write-Host "   • API Gateway:       30-60 seconds" -ForegroundColor White
Write-Host "   • Finance:           60-90 seconds (Testcontainers startup)" -ForegroundColor White
Write-Host ""
Write-Host "🔍 Look for 'Listening on: http://0.0.0.0:XXXX' in each window" -ForegroundColor Yellow
Write-Host ""
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
Write-Host ""
Write-Host "📊 Once all services are ready, verify with:" -ForegroundColor Cyan
Write-Host ""
Write-Host "   curl http://localhost:8081/q/health          # Identity" -ForegroundColor Gray
Write-Host "   curl http://localhost:8080/q/health          # Gateway" -ForegroundColor Gray
Write-Host "   curl http://localhost:8082/q/health/ready    # Finance" -ForegroundColor Gray
Write-Host ""
Write-Host "🌐 Open Swagger UIs:" -ForegroundColor Cyan
Write-Host "   http://localhost:8081/q/swagger-ui" -ForegroundColor Blue
Write-Host "   http://localhost:8080/q/swagger-ui" -ForegroundColor Blue
Write-Host "   http://localhost:8082/q/swagger-ui" -ForegroundColor Blue
Write-Host ""
Write-Host "📚 Documentation:" -ForegroundColor Cyan
Write-Host "   • docs/SERVICE_STARTUP_GUIDE.md" -ForegroundColor Gray
Write-Host "   • docs/FINANCE_LIVE_TEST_GUIDE.md" -ForegroundColor Gray
Write-Host "   • docs/rest/finance-accounting.rest" -ForegroundColor Gray
Write-Host ""
Write-Host "🛑 To stop all services:" -ForegroundColor Red
Write-Host "   .\scripts\stop-all-services.ps1" -ForegroundColor Cyan
Write-Host ""
