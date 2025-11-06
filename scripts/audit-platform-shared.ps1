#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Audits platform-shared modules for governance compliance (ADR-006)

.DESCRIPTION
    Monitors platform-shared for signs of "distributed monolith" anti-pattern:
    - File count per module (warning at 25, critical at 50)
    - Forbidden patterns (business logic, domain models, utilities)
    - Module count (max 4)
    
    Run this script weekly or before major releases.

.EXAMPLE
    .\audit-platform-shared.ps1
    
.NOTES
    Related: ADR-006 Platform-Shared Governance Rules
#>

$ErrorActionPreference = "Stop"
$WarningCount = 0
$CriticalCount = 0

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Platform-Shared Governance Audit (ADR-006)" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

# Check module count
$PlatformSharedDir = "$PSScriptRoot\..\platform-shared"
$Modules = Get-ChildItem -Path $PlatformSharedDir -Directory | Where-Object { $_.Name -notlike ".*" }
$ModuleCount = $Modules.Count

Write-Host "Module Count: $ModuleCount / 4 (max)" -ForegroundColor $(if ($ModuleCount -gt 4) { "Red" } else { "Green" })
if ($ModuleCount -gt 4) {
    Write-Host "  ❌ CRITICAL: Exceeded maximum module count!" -ForegroundColor Red
    Write-Host "  Action: Remove module or get team consensus" -ForegroundColor Yellow
    $CriticalCount++
}
Write-Host ""

# Check each module
foreach ($Module in $Modules) {
    Write-Host "Module: $($Module.Name)" -ForegroundColor White
    Write-Host "  Location: platform-shared/$($Module.Name)" -ForegroundColor Gray
    
    # Count Kotlin files
    $SrcDir = Join-Path $Module.FullName "src\main\kotlin"
    if (Test-Path $SrcDir) {
        $KotlinFiles = Get-ChildItem -Path $SrcDir -Recurse -Filter "*.kt"
        $FileCount = $KotlinFiles.Count
        
        $Status = if ($FileCount -ge 50) {
            $CriticalCount++
            "❌ CRITICAL"
        } elseif ($FileCount -ge 25) {
            $WarningCount++
            "⚠️  WARNING"
        } else {
            "✅ OK"
        }
        
        $Color = if ($FileCount -ge 50) {
            "Red"
        } elseif ($FileCount -ge 25) {
            "Yellow"
        } else {
            "Green"
        }
        
        Write-Host "  Files: $FileCount" -ForegroundColor $Color -NoNewline
        Write-Host " $Status" -ForegroundColor $Color
        
        if ($FileCount -ge 25) {
            Write-Host "  Action: Review and refactor module" -ForegroundColor Yellow
        }
        
        # Check for forbidden patterns
        $ForbiddenPatterns = @(
            @{Pattern = "*Service.kt"; Description = "Business services" },
            @{Pattern = "*Repository.kt"; Description = "Repositories" },
            @{Pattern = "*Impl.kt"; Description = "Concrete implementations" },
            @{Pattern = "*Adapter.kt"; Description = "Infrastructure adapters" },
            @{Pattern = "*Utils.kt"; Description = "Utility classes" },
            @{Pattern = "*Helper.kt"; Description = "Helper classes" }
        )
        
        foreach ($Forbidden in $ForbiddenPatterns) {
            $Matches = $KotlinFiles | Where-Object { $_.Name -like $Forbidden.Pattern }
            if ($Matches) {
                $WarningCount++
                Write-Host "  ⚠️  Found $($Matches.Count) $($Forbidden.Description):" -ForegroundColor Yellow
                foreach ($Match in $Matches) {
                    Write-Host "     - $($Match.Name)" -ForegroundColor Yellow
                }
            }
        }
        
        # Check for business domain terms
        $BusinessTerms = @(
            "Customer", "Order", "Invoice", "Product", "Payment",
            "Shipment", "Inventory", "Purchase", "Sale", "Account"
        )
        
        foreach ($File in $KotlinFiles) {
            $Content = Get-Content $File.FullName -Raw
            foreach ($Term in $BusinessTerms) {
                if ($Content -match "class $Term\b|data class $Term\b|interface $Term\b") {
                    $WarningCount++
                    Write-Host "  ⚠️  Found business domain term '$Term' in:" -ForegroundColor Yellow
                    Write-Host "     $($File.Name)" -ForegroundColor Yellow
                    Write-Host "     Action: Move to appropriate bounded context" -ForegroundColor Yellow
                }
            }
        }
    } else {
        Write-Host "  Files: 0 (no src/main/kotlin)" -ForegroundColor Gray
    }
    
    Write-Host ""
}

# Summary
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Audit Summary" -ForegroundColor Cyan
Write-Host "==================================================" -ForegroundColor Cyan
Write-Host ""

if ($CriticalCount -eq 0 -and $WarningCount -eq 0) {
    Write-Host "✅ PASS: No issues found" -ForegroundColor Green
    Write-Host "Platform-shared is compliant with ADR-006" -ForegroundColor Green
    exit 0
} else {
    Write-Host "Issues Found:" -ForegroundColor Yellow
    if ($CriticalCount -gt 0) {
        Write-Host "  ❌ Critical: $CriticalCount" -ForegroundColor Red
    }
    if ($WarningCount -gt 0) {
        Write-Host "  ⚠️  Warnings: $WarningCount" -ForegroundColor Yellow
    }
    Write-Host ""
    Write-Host "Action Required:" -ForegroundColor Yellow
    Write-Host "  1. Review flagged files" -ForegroundColor White
    Write-Host "  2. Move business logic to bounded contexts" -ForegroundColor White
    Write-Host "  3. Refactor large modules" -ForegroundColor White
    Write-Host "  4. See: docs/adr/ADR-006-platform-shared-governance.md" -ForegroundColor White
    
    if ($CriticalCount -gt 0) {
        exit 1
    } else {
        exit 0
    }
}
