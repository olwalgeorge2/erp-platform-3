#!/usr/bin/env pwsh
# Start Infrastructure Services (PostgreSQL, Kafka/Redpanda, Redis)
# Run this FIRST before starting application services

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Magenta
Write-Host "║  ERP Platform - Infrastructure Services Startup           ║" -ForegroundColor Magenta
Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Magenta
Write-Host ""

# Check if Docker is running
Write-Host "🔍 Checking Docker status..." -ForegroundColor Yellow
try {
    $dockerVersion = docker version --format '{{.Server.Version}}' 2>$null
    if ($LASTEXITCODE -ne 0) {
        throw "Docker is not running"
    }
    Write-Host "✅ Docker is running (version $dockerVersion)" -ForegroundColor Green
} catch {
    Write-Host "❌ Docker is not running. Please start Docker Desktop first." -ForegroundColor Red
    Write-Host "   Open Docker Desktop and wait for it to fully start." -ForegroundColor Yellow
    exit 1
}

Write-Host ""
Write-Host "🚀 Starting infrastructure services..." -ForegroundColor Cyan
Write-Host ""

# Start PostgreSQL, Redis, and Redpanda (Kafka + Console)
try {
    docker compose -f docker-compose-kafka.yml up -d postgres redis redpanda redpanda-console
    
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to start infrastructure services"
    }
    
    Write-Host ""
    Write-Host "✅ Infrastructure services started successfully!" -ForegroundColor Green
    Write-Host ""
    
    # Show running containers
    Write-Host "📦 Running containers:" -ForegroundColor Cyan
    docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}" --filter "name=erp-"
    
    Write-Host ""
    Write-Host "⏳ Waiting for PostgreSQL to be healthy (this may take 10-15 seconds)..." -ForegroundColor Yellow
    
    $maxAttempts = 30
    $attempt = 0
    $healthy = $false
    
    while ($attempt -lt $maxAttempts -and -not $healthy) {
        $attempt++
        $status = docker inspect --format='{{.State.Health.Status}}' erp-postgres 2>$null
        
        if ($status -eq "healthy") {
            $healthy = $true
            Write-Host "✅ PostgreSQL is healthy and ready!" -ForegroundColor Green
        } else {
            Write-Host "   Attempt $attempt/$maxAttempts - Status: $status" -ForegroundColor Gray
            Start-Sleep -Seconds 2
        }
    }
    
    if (-not $healthy) {
        Write-Host "⚠️  PostgreSQL did not become healthy in time, but may still be starting..." -ForegroundColor Yellow
        Write-Host "   Check with: docker ps" -ForegroundColor Gray
    }
    
    Write-Host ""
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host "  INFRASTRUCTURE SERVICES READY" -ForegroundColor Magenta
    Write-Host "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" -ForegroundColor Magenta
    Write-Host ""
    Write-Host "📊 Service Endpoints:" -ForegroundColor Cyan
    Write-Host "   • PostgreSQL:        localhost:5432" -ForegroundColor White
    Write-Host "   • Redis:             localhost:6379" -ForegroundColor White
    Write-Host "   • Redpanda (Kafka):  localhost:19092" -ForegroundColor White
    Write-Host "   • Redpanda Console:  http://localhost:8090" -ForegroundColor White
    Write-Host "   • Schema Registry:   http://localhost:18081" -ForegroundColor White
    Write-Host ""
    Write-Host "🔐 Database Credentials:" -ForegroundColor Cyan
    Write-Host "   • Database: erp_identity" -ForegroundColor White
    Write-Host "   • Username: erp_user" -ForegroundColor White
    Write-Host "   • Password: erp_pass" -ForegroundColor White
    Write-Host ""
    Write-Host "✅ You can now start application services with:" -ForegroundColor Green
    Write-Host "   .\scripts\start-all-services.ps1" -ForegroundColor Cyan
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "❌ Failed to start infrastructure services: $_" -ForegroundColor Red
    Write-Host ""
    Write-Host "💡 Troubleshooting:" -ForegroundColor Yellow
    Write-Host "   1. Ensure Docker Desktop is running" -ForegroundColor Gray
    Write-Host "   2. Check if port 5432 is available (stop other PostgreSQL instances)" -ForegroundColor Gray
    Write-Host "   3. Try: docker compose -f docker-compose-kafka.yml down" -ForegroundColor Gray
    Write-Host "   4. Then run this script again" -ForegroundColor Gray
    Write-Host ""
    exit 1
}
