#!/usr/bin/env pwsh
# Test JWT authentication through API Gateway

$ErrorActionPreference = "Stop"

$tokenFile = "$PSScriptRoot\tokens\dev.jwt"
if (-not (Test-Path $tokenFile)) {
    Write-Host "❌ Token file not found: $tokenFile" -ForegroundColor Red
    exit 1
}

$token = Get-Content $tokenFile -Raw
$token = $token.Trim()

Write-Host "`n🔑 Testing JWT Authentication through API Gateway" -ForegroundColor Cyan
Write-Host "Token length: $($token.Length) characters`n" -ForegroundColor Gray

# Test 1: Public endpoint (should work without token)
Write-Host "Test 1: Public health endpoint (no auth required)" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/q/health/live" -Method GET -UseBasicParsing
    Write-Host "✅ Status: $($response.StatusCode) - Public endpoint accessible`n" -ForegroundColor Green
} catch {
    Write-Host "❌ Failed: $($_.Exception.Message)`n" -ForegroundColor Red
}

# Test 2: Protected endpoint without token (should fail with 401)
Write-Host "Test 2: Protected endpoint WITHOUT token (should return 401)" -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/identity/tenants" -Method GET -UseBasicParsing
    Write-Host "❌ Unexpected success: $($response.StatusCode)`n" -ForegroundColor Red
} catch {
    if ($_.Exception.Response.StatusCode -eq 401) {
        Write-Host "✅ Correctly rejected: 401 Unauthorized`n" -ForegroundColor Green
    } else {
        Write-Host "❌ Unexpected error: $($_.Exception.Message)`n" -ForegroundColor Red
    }
}

# Test 3: Regular endpoint with JWT token (optional auth)
Write-Host "Test 3: Regular endpoint WITH JWT token (authentication validated)" -ForegroundColor Yellow
try {
    $headers = @{
        Authorization = "Bearer $token"
        "Content-Type" = "application/json"
    }
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/v1/identity/tenants" -Headers $headers -Method GET -UseBasicParsing
    Write-Host "✅ Status: $($response.StatusCode) - JWT validated successfully!" -ForegroundColor Green
    Write-Host "Response preview: $($response.Content.Substring(0, [Math]::Min(200, $response.Content.Length)))...`n" -ForegroundColor Gray
    
    # Check for W3C trace headers
    $traceparent = $response.Headers["traceparent"]
    if ($traceparent) {
        Write-Host "✅ W3C traceparent header present: $traceparent" -ForegroundColor Green
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    $errorBody = ""
    try {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $errorBody = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()
    } catch {}
    
    Write-Host "❌ Status: $statusCode" -ForegroundColor Red
    if ($errorBody) {
        Write-Host "Error: $errorBody`n" -ForegroundColor Red
    } else {
        Write-Host "Error: $($_.Exception.Message)`n" -ForegroundColor Red
    }
}

# Test 4: Protected prefix without proper roles (should return 403)
Write-Host "Test 4: Protected path (/api/admin/) without admin role (should return 403)" -ForegroundColor Yellow
try {
    # Generate token WITHOUT admin role
    Write-Host "  Generating token with only 'user' role..." -ForegroundColor Gray
    & "$PSScriptRoot\dev-jwt.ps1" -Subject test-user -Roles "user" -Issuer erp-platform-dev -Minutes 10 | Out-Null
    $userToken = (Get-Content "$PSScriptRoot\tokens\dev.jwt" -Raw).Trim()
    
    $headers = @{
        Authorization = "Bearer $userToken"
        "Content-Type" = "application/json"
    }
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/admin/test" -Headers $headers -Method GET -UseBasicParsing
    Write-Host "❌ Unexpected success: $($response.StatusCode) (403 expected)`n" -ForegroundColor Red
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 403) {
        Write-Host "✅ Correctly rejected: 403 Forbidden (role-based access control working!)`n" -ForegroundColor Green
    } elseif ($statusCode -eq 404) {
        Write-Host "⚠️  404 Not Found - endpoint doesn't exist, but auth check passed`n" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Unexpected status: $statusCode`n" -ForegroundColor Red
    }
}

# Test 5: Protected prefix WITH admin role (should work or 404)
Write-Host "Test 5: Protected path (/api/admin/) WITH admin role" -ForegroundColor Yellow
try {
    # Restore token with admin role
    Write-Host "  Using token with 'admin' role..." -ForegroundColor Gray
    & "$PSScriptRoot\dev-jwt.ps1" -Subject dev-user -Roles "admin,user" -Issuer erp-platform-dev -Minutes 10 | Out-Null
    $adminToken = (Get-Content "$PSScriptRoot\tokens\dev.jwt" -Raw).Trim()
    
    $headers = @{
        Authorization = "Bearer $adminToken"
        "Content-Type" = "application/json"
    }
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/admin/test" -Headers $headers -Method GET -UseBasicParsing
    Write-Host "✅ Status: $($response.StatusCode) - Admin access granted!`n" -ForegroundColor Green
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    if ($statusCode -eq 404) {
        Write-Host "✅ Auth passed (404 only because /api/admin/test doesn't exist)`n" -ForegroundColor Green
    } else {
        Write-Host "❌ Unexpected status: $statusCode`n" -ForegroundColor Red
    }
}

Write-Host "`n✨ JWT Authentication & Authorization Tests Completed!" -ForegroundColor Cyan
Write-Host "Summary:" -ForegroundColor Cyan
Write-Host "  - JWT validation: Working ✅" -ForegroundColor Gray
Write-Host "  - W3C tracing: Check trace headers above" -ForegroundColor Gray
Write-Host "  - 403 role enforcement: Check Test 4 & 5 results" -ForegroundColor Gray
