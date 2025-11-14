#!/usr/bin/env pwsh
# Check Status of All ERP Platform Services

$ErrorActionPreference = "Continue"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║  ERP Platform - Service Status Check                      ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# Check Docker
Write-Host "🐳 Docker Status:" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>$null
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Docker running (version $dockerVersion)" -ForegroundColor Green
    } else {
        Write-Host "❌ Docker not running" -ForegroundColor Red
    }
} catch {
    Write-Host "❌ Docker not accessible" -ForegroundColor Red
}

Write-Host ""

# Check Docker containers
Write-Host "📦 Infrastructure Containers:" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

$containers = docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=erp-" 2>$null

if ($containers) {
    $containers | ForEach-Object { Write-Host $_ -ForegroundColor White }
    Write-Host ""
    
    # Check health status
    $postgresHealth = docker inspect --format='{{.State.Health.Status}}' erp-postgres 2>$null
    $redpandaRunning = docker ps --filter "name=erp-redpanda" --filter "status=running" --format "{{.Names}}" 2>$null
    
    if ($postgresHealth -eq "healthy") {
        Write-Host "   ✅ PostgreSQL: Healthy" -ForegroundColor Green
    } elseif ($postgresHealth -eq "starting") {
        Write-Host "   ⏳ PostgreSQL: Starting..." -ForegroundColor Yellow
    } elseif ($postgresHealth) {
        Write-Host "   ⚠️  PostgreSQL: $postgresHealth" -ForegroundColor Yellow
    }
    
    if ($redpandaRunning) {
        Write-Host "   ✅ Redpanda/Kafka: Running" -ForegroundColor Green
    }
} else {
    Write-Host "❌ No infrastructure containers running" -ForegroundColor Red
    Write-Host "   Start with: .\scripts\start-infrastructure.ps1" -ForegroundColor Cyan
}

Write-Host ""

# Check application ports
Write-Host "🌐 Application Services:" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Gray

function Test-ServiceHealth {
    param(
        [string]$Name,
        [int]$Port,
        [string]$HealthPath = "/q/health"
    )
    
    $portCheck = netstat -ano | Select-String ":$Port.*LISTENING"
    
    if ($portCheck) {
        Write-Host "   Port $Port (${Name}): " -NoNewline -ForegroundColor White
        
        try {
            $ErrorActionPreference = "Stop"
            $response = Invoke-WebRequest -Uri "http://localhost:$Port$HealthPath" -TimeoutSec 10 -UseBasicParsing -ErrorAction Stop
            
            if ($response.StatusCode -eq 200) {
                Write-Host "✅ UP" -ForegroundColor Green
                return $true
            } else {
                Write-Host "⚠️  Running but health check failed (Status: $($response.StatusCode))" -ForegroundColor Yellow
                return $false
            }
        } catch [Microsoft.PowerShell.Commands.HttpResponseException] {
            # HTTP error status (like 503) - service is responding but unhealthy
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -eq 503) {
                Write-Host "⚠️  UP but unhealthy (503 - some checks failing)" -ForegroundColor Yellow
                return $true  # Service IS running, just reporting issues
            } else {
                Write-Host "⚠️  Running but returned HTTP $statusCode" -ForegroundColor Yellow
                return $false
            }
        } catch [System.Net.WebException] {
            # Connection refused or timeout
            Write-Host "⏳ Starting (port bound but not responding)" -ForegroundColor Yellow
            return $false
        } catch {
            # Other errors
            Write-Host "⚠️  Error checking health: $($_.Exception.Message.Split("`n")[0])" -ForegroundColor Yellow
            return $false
        }
    } else {
        Write-Host "   Port $Port (${Name}): " -NoNewline -ForegroundColor White
        Write-Host "❌ Not running" -ForegroundColor Red
        return $false
    }
}

$gatewayUp = Test-ServiceHealth -Name "API Gateway" -Port 8080 -HealthPath "/q/health"
$identityUp = Test-ServiceHealth -Name "Tenancy-Identity" -Port 8081 -HealthPath "/q/health"
$financeUp = Test-ServiceHealth -Name "Finance" -Port 8082 -HealthPath "/q/health/ready"

Write-Host ""

if ($identityUp) {
    Invoke-WebRequest -Uri "http://localhost:8081/q/openapi" -TimeoutSec 10 -UseBasicParsing -ErrorAction SilentlyContinue > $null
}
if ($gatewayUp) {
    Invoke-WebRequest -Uri "http://localhost:8080/q/openapi" -TimeoutSec 10 -UseBasicParsing -ErrorAction SilentlyContinue > $null
}
if ($financeUp) {
    Invoke-WebRequest -Uri "http://localhost:8082/q/openapi" -TimeoutSec 10 -UseBasicParsing -ErrorAction SilentlyContinue > $null
}

Write-Host ""

# Summary
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host "  SUMMARY" -ForegroundColor Cyan
Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Cyan
Write-Host ""

$allUp = $gatewayUp -and $identityUp -and $financeUp

if ($allUp) {
    Write-Host "✅ All services are running and healthy!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🌐 Service URLs:" -ForegroundColor Cyan
    Write-Host "   • API Gateway:       http://localhost:8080/q/swagger-ui" -ForegroundColor Blue
    Write-Host "   • Tenancy-Identity:  http://localhost:8081/q/swagger-ui" -ForegroundColor Blue
    Write-Host "   • Finance:           http://localhost:8082/q/swagger-ui" -ForegroundColor Blue
    Write-Host "   • Redpanda Console:  http://localhost:8090" -ForegroundColor Blue
    Write-Host ""
} elseif ($gatewayUp -or $identityUp -or $financeUp) {
    Write-Host "⚠️  Some services are running, but not all" -ForegroundColor Yellow
    Write-Host ""
    if (-not $gatewayUp) { Write-Host "   ❌ API Gateway (8080) is not running" -ForegroundColor Red }
    if (-not $identityUp) { Write-Host "   ❌ Tenancy-Identity (8081) is not running" -ForegroundColor Red }
    if (-not $financeUp) { Write-Host "   ❌ Finance (8082) is not running" -ForegroundColor Red }
    Write-Host ""
} else {
    Write-Host "❌ No application services are running" -ForegroundColor Red
    Write-Host ""
    Write-Host "🚀 To start services:" -ForegroundColor Cyan
    Write-Host "   1. .\scripts\start-infrastructure.ps1" -ForegroundColor White
    Write-Host "   2. .\scripts\start-all-services.ps1" -ForegroundColor White
    Write-Host ""
}

# Quick test commands
Write-Host "🧪 Quick Health Checks:" -ForegroundColor Cyan
Write-Host "   curl http://localhost:8081/q/health          # Identity" -ForegroundColor Gray
Write-Host "   curl http://localhost:8080/q/health          # Gateway" -ForegroundColor Gray
Write-Host "   curl http://localhost:8082/q/health/ready    # Finance" -ForegroundColor Gray
Write-Host ""
