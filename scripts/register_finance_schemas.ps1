#!/usr/bin/env pwsh
# Register finance schemas in Schema Registry
param(
    [string]$RegistryUrl = "http://localhost:18081",
    [string]$BasicAuth = ""
)

$ErrorActionPreference = "Stop"

$ROOT_DIR = Split-Path -Parent $PSScriptRoot
$SCHEMA_DIR = Join-Path $ROOT_DIR "docs\schemas\finance"

Write-Host "Schema Registry URL: $RegistryUrl" -ForegroundColor Cyan
Write-Host "Schema Directory: $SCHEMA_DIR" -ForegroundColor Cyan
Write-Host ""

function Register-Schema {
    param(
        [string]$Subject,
        [string]$FilePath,
        [string]$SchemaType = "JSON"
    )
    
    Write-Host "Registering ${Subject}..." -ForegroundColor Yellow
    
    # Read and escape the schema
    $rawSchema = Get-Content $FilePath -Raw | ConvertFrom-Json | ConvertTo-Json -Depth 100 -Compress
    
    # Create the payload
    $payload = @{
        schemaType = $SchemaType
        schema = $rawSchema
    } | ConvertTo-Json -Depth 100
    
    # Prepare headers
    $headers = @{
        "Content-Type" = "application/vnd.schemaregistry.v1+json"
    }
    
    if ($BasicAuth) {
        $headers["Authorization"] = "Basic $BasicAuth"
    }
    
    try {
        $response = Invoke-RestMethod -Uri "$RegistryUrl/subjects/$Subject/versions" `
            -Method Post `
            -Headers $headers `
            -Body $payload
        
        Write-Host "✓ Registered successfully" -ForegroundColor Green
        Write-Host "  Schema ID: $($response.id)" -ForegroundColor White
        Write-Host ""
        
        return $response
    }
    catch {
        Write-Host "✗ Registration failed: $($_.Exception.Message)" -ForegroundColor Red
        Write-Host "  Response: $($_.ErrorDetails.Message)" -ForegroundColor Red
        Write-Host ""
        throw
    }
}

# Register all finance schemas
$schemas = @{
    "finance.journal.events.v1-value" = "finance.journal.events.v1.json"
    "finance.period.events.v1-value" = "finance.period.events.v1.json"
    "finance.reconciliation.events.v1-value" = "finance.reconciliation.events.v1.json"
}

$results = @{}

foreach ($entry in $schemas.GetEnumerator()) {
    $subject = $entry.Key
    $filename = $entry.Value
    $filepath = Join-Path $SCHEMA_DIR $filename
    
    if (-not (Test-Path $filepath)) {
        Write-Host "✗ Schema file not found: $filepath" -ForegroundColor Red
        continue
    }
    
    try {
        $result = Register-Schema -Subject $subject -FilePath $filepath
        $results[$subject] = $result.id
    }
    catch {
        Write-Host "Failed to register $subject" -ForegroundColor Red
    }
}

Write-Host "===========================================" -ForegroundColor Cyan
Write-Host "Registration Summary" -ForegroundColor Cyan
Write-Host "===========================================" -ForegroundColor Cyan

foreach ($entry in $results.GetEnumerator()) {
    Write-Host "$($entry.Key): Schema ID $($entry.Value)" -ForegroundColor White
}

Write-Host ""
Write-Host "Done. Registered $($results.Count) schemas." -ForegroundColor Green
