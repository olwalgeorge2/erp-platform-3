#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Restart all ERP platform services to apply configuration changes
.DESCRIPTION
    Gracefully stops all running services and restarts them with updated configuration.
    Useful after making changes to application.yml, OpenAPI schemas, or code.
#>

$ErrorActionPreference = "Stop"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         ERP Platform - Restart Services                   ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

Write-Host "🔄 Restarting all services to apply changes...`n" -ForegroundColor Yellow

# Stop services
Write-Host "1️⃣  Stopping services..." -ForegroundColor Cyan
& "$PSScriptRoot\stop-all-services.ps1"

Start-Sleep -Seconds 2

# Start services
Write-Host "`n2️⃣  Starting services..." -ForegroundColor Cyan
& "$PSScriptRoot\start-all-services.ps1"

Write-Host "`n✅ Restart complete!" -ForegroundColor Green
Write-Host "`n💡 Verify Swagger UI to check OpenAPI schemas:" -ForegroundColor Yellow
Write-Host "   Gateway:  http://localhost:8080/q/swagger-ui/" -ForegroundColor Gray
Write-Host "   Identity: http://localhost:8081/q/swagger-ui/" -ForegroundColor Gray
Write-Host "   Finance:  http://localhost:8082/q/swagger-ui/" -ForegroundColor Gray
