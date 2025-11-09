#!/usr/bin/env pwsh
<#
.SYNOPSIS
    Verify that git hooks are properly installed and functional
.DESCRIPTION
    Checks that pre-commit and pre-push hooks exist and are executable
#>

$ErrorActionPreference = 'Stop'

Write-Host "`n🔍 Verifying Git Hooks Installation" -ForegroundColor Cyan
Write-Host "=" * 60

$checks = @()

# Check 1: Git repository exists
if (-not (Test-Path ".git")) {
    Write-Host "❌ Not a git repository" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Git repository found" -ForegroundColor Green

# Check 2: Hook scripts exist in source
$hookSource = "scripts/hooks"
if (-not (Test-Path $hookSource)) {
    Write-Host "❌ Hook source directory not found: $hookSource" -ForegroundColor Red
    exit 1
}
Write-Host "✅ Hook source directory exists: $hookSource" -ForegroundColor Green

# Check 3: Pre-commit hook installed
$preCommitHook = ".git/hooks/pre-commit"
if (Test-Path $preCommitHook) {
    Write-Host "✅ Pre-commit hook installed" -ForegroundColor Green
    $checks += $true
} else {
    Write-Host "⚠️  Pre-commit hook NOT installed" -ForegroundColor Yellow
    Write-Host "   Run: just install-hooks" -ForegroundColor Yellow
    $checks += $false
}

# Check 4: Pre-push hook installed
$prePushHook = ".git/hooks/pre-push"
if (Test-Path $prePushHook) {
    Write-Host "✅ Pre-push hook installed" -ForegroundColor Green
    $checks += $true
} else {
    Write-Host "⚠️  Pre-push hook NOT installed" -ForegroundColor Yellow
    Write-Host "   Run: just install-hooks" -ForegroundColor Yellow
    $checks += $false
}

# Check 5: Verify pre-commit content
if (Test-Path $preCommitHook) {
    $content = Get-Content $preCommitHook -Raw
    if ($content -match "ktlint") {
        Write-Host "✅ Pre-commit hook includes ktlint checks" -ForegroundColor Green
        $checks += $true
    } else {
        Write-Host "⚠️  Pre-commit hook missing ktlint checks" -ForegroundColor Yellow
        $checks += $false
    }
    
    if ($content -match "architecture") {
        Write-Host "✅ Pre-commit hook includes architecture tests" -ForegroundColor Green
        $checks += $true
    } else {
        Write-Host "⚠️  Pre-commit hook missing architecture tests" -ForegroundColor Yellow
        $checks += $false
    }
}

# Check 6: Verify pre-push content
if (Test-Path $prePushHook) {
    $content = Get-Content $prePushHook -Raw
    if ($content -match "ktlintCheck") {
        Write-Host "✅ Pre-push hook includes ktlint checks" -ForegroundColor Green
        $checks += $true
    } else {
        Write-Host "⚠️  Pre-push hook missing ktlint checks" -ForegroundColor Yellow
        $checks += $false
    }
}

# Check 7: Gradlew exists
if (Test-Path "./gradlew") {
    Write-Host "✅ Gradle wrapper found" -ForegroundColor Green
    $checks += $true
} elseif (Test-Path "./gradlew.bat") {
    Write-Host "✅ Gradle wrapper (Windows) found" -ForegroundColor Green
    $checks += $true
} else {
    Write-Host "❌ Gradle wrapper not found" -ForegroundColor Red
    $checks += $false
}

# Check 8: Just command available (optional)
$justInstalled = Get-Command just -ErrorAction SilentlyContinue
if ($justInstalled) {
    Write-Host "✅ 'just' command available" -ForegroundColor Green
    $checks += $true
} else {
    Write-Host "ℹ️  'just' command not found (optional)" -ForegroundColor Yellow
    Write-Host "   Install: choco install just OR scoop install just" -ForegroundColor Gray
}

# Summary
Write-Host "`n" + "=" * 60
$passedChecks = ($checks | Where-Object { $_ -eq $true }).Count
$totalChecks = $checks.Count

if ($passedChecks -eq $totalChecks) {
    Write-Host "✅ All checks passed ($passedChecks/$totalChecks)" -ForegroundColor Green
    Write-Host "`n📚 Next steps:" -ForegroundColor Cyan
    Write-Host "   1. Make a change to a Kotlin file" -ForegroundColor Gray
    Write-Host "   2. Run: git commit -m 'test'" -ForegroundColor Gray
    Write-Host "   3. Verify you see '[pre-commit]' output with ktlint check" -ForegroundColor Gray
    exit 0
} elseif ($passedChecks -ge ($totalChecks * 0.7)) {
    Write-Host "⚠️  Most checks passed ($passedChecks/$totalChecks)" -ForegroundColor Yellow
    Write-Host "`n🔧 To fix:" -ForegroundColor Cyan
    Write-Host "   just install-hooks" -ForegroundColor White
    exit 1
} else {
    Write-Host "❌ Several checks failed ($passedChecks/$totalChecks)" -ForegroundColor Red
    Write-Host "`n🔧 To fix:" -ForegroundColor Cyan
    Write-Host "   1. Run: just install-hooks" -ForegroundColor White
    Write-Host "   2. Run: pwsh scripts/verify-hooks.ps1" -ForegroundColor White
    exit 1
}
