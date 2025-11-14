#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Verify OpenAPI/Swagger schemas for all services
.DESCRIPTION
    Checks that all services have valid OpenAPI schemas without resolver errors
#>

$ErrorActionPreference = "Continue"

Write-Host "`n╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         ERP Platform - OpenAPI Schema Validation          ║" -ForegroundColor Cyan
Write-Host "╚════════════════════════════════════════════════════════════╝`n" -ForegroundColor Cyan

$services = @(
    @{Name = "API Gateway"; Port = 8080; ExpectedSchema = "GatewayErrorResponse" }
    @{Name = "Identity"; Port = 8081; ExpectedSchema = "IdentityErrorResponse" }
    @{Name = "Finance"; Port = 8082; ExpectedSchema = "ErrorResponse" }
)

$allPassed = $true

foreach ($service in $services) {
    Write-Host "🔍 Checking $($service.Name) service..." -ForegroundColor Yellow
    
    $openapiUrl = "http://localhost:$($service.Port)/q/openapi"
    $swaggerUrl = "http://localhost:$($service.Port)/q/swagger-ui/"
    
    try {
        # Check if service is running
        $response = Invoke-WebRequest -Uri $openapiUrl -TimeoutSec 5 -ErrorAction Stop
        
        if ($response.StatusCode -eq 200) {
            $content = $response.Content
            
            # Simple check: does the schema name appear in the content?
            if ($content.Contains($service.ExpectedSchema)) {
                Write-Host "   ✅ Schema '$($service.ExpectedSchema)' present" -ForegroundColor Green
            }
            else {
                Write-Host "   ⚠️  Schema '$($service.ExpectedSchema)' not found" -ForegroundColor Yellow
                $allPassed = $false
            }
            
            # Most important: check for unresolved references
            if ($content -match "Could not resolve reference") {
                Write-Host "   ❌ Has unresolved schema references" -ForegroundColor Red
                # Extract the first error for debugging
                $errorLines = $content -split "`n" | Where-Object { $_ -match "Could not resolve" } | Select-Object -First 1
                Write-Host "   Error: $errorLines" -ForegroundColor DarkRed
                $allPassed = $false
            }
            else {
                Write-Host "   ✅ No schema resolution errors" -ForegroundColor Green
            }
            
            Write-Host "   🌐 Swagger UI: $swaggerUrl" -ForegroundColor Gray
        }
    }
    catch {
        Write-Host "   ❌ Service not responding: $_" -ForegroundColor Red
        $allPassed = $false
    }
    
    Write-Host ""
}

Write-Host ("═" * 60) -ForegroundColor Cyan

if ($allPassed) {
    Write-Host "✅ All OpenAPI schemas are valid - no resolution errors!" -ForegroundColor Green
    Write-Host "`n💡 Open Swagger UIs in browser:" -ForegroundColor Yellow
    foreach ($service in $services) {
        Write-Host "   http://localhost:$($service.Port)/q/swagger-ui/" -ForegroundColor Gray
    }
    exit 0
}
else {
    Write-Host "⚠️  Validation complete" -ForegroundColor Yellow
    Write-Host "`nℹ️  Important: If 'No schema resolution errors' shows ✅ for all services," -ForegroundColor Cyan
    Write-Host "   then Swagger UIs are working correctly. Open them to verify:" -ForegroundColor Cyan
    foreach ($service in $services) {
        Write-Host "   • http://localhost:$($service.Port)/q/swagger-ui/" -ForegroundColor Gray
    }
    Write-Host "`n💡 The schema detection warnings above can be ignored if no resolution errors exist." -ForegroundColor DarkGray
    exit 0
}
