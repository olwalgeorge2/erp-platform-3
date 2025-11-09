#!/usr/bin/env pwsh
<#
.SYNOPSIS
    ERP Platform development task automation (alternative to justfile)
.DESCRIPTION
    Provides common development tasks for systems without 'just' command runner
.EXAMPLE
    .\dev.ps1 install-hooks
    .\dev.ps1 verify
    .\dev.ps1 format
#>

param(
    [Parameter(Position = 0)]
    [string]$Task = "help",
    
    [Parameter(Position = 1, ValueFromRemainingArguments = $true)]
    [string[]]$Args
)

$ErrorActionPreference = 'Stop'

function Show-Help {
    Write-Host @"
ERP Platform Development Tasks
==============================

Usage: .\dev.ps1 <task> [args]

Hook Management:
  install-hooks     Install git hooks for pre-commit and pre-push validation
  verify-hooks      Verify git hooks are properly installed
  uninstall-hooks   Remove git hooks

Code Quality:
  lint              Run ktlint style check only
  format            Auto-format code with ktlint
  verify            Run full local verification (lint + tests)
  preflight         Format + verify combo

Testing:
  arch              Run architecture tests only
  identity-tests    Run identity infrastructure tests
  smoke             Run smoke tests for identity service

Build:
  build             Build all modules
  clean             Clean build artifacts

Git Helpers:
  pre-commit        Run pre-commit checks manually
  pre-push          Run pre-push checks manually
  push              Git push with pre-push hook enforcement

Examples:
  .\dev.ps1 install-hooks
  .\dev.ps1 format
  .\dev.ps1 verify
  .\dev.ps1 push

Note: Consider installing 'just' for shorter commands:
  choco install just
  scoop install just
"@ -ForegroundColor Cyan
}

function Invoke-InstallHooks {
    Write-Host "Installing git hooks..." -ForegroundColor Cyan
    if (-not (Test-Path '.git/hooks')) {
        Write-Error "Not a git repository"
        exit 1
    }
    # Use bash scripts for Git compatibility on Windows
    Copy-Item 'scripts/hooks/pre-commit' '.git/hooks/pre-commit' -Force
    Copy-Item 'scripts/hooks/pre-push' '.git/hooks/pre-push' -Force
    Write-Host "✅ Git hooks installed successfully (bash versions for Git compatibility)" -ForegroundColor Green
    Write-Host "Run: .\dev.ps1 verify-hooks to test" -ForegroundColor Cyan
}

function Invoke-VerifyHooks {
    & pwsh -NoProfile -File scripts/verify-hooks.ps1
}

function Invoke-UninstallHooks {
    Write-Host "Removing git hooks..." -ForegroundColor Yellow
    Remove-Item '.git/hooks/pre-commit' -ErrorAction SilentlyContinue
    Remove-Item '.git/hooks/pre-push' -ErrorAction SilentlyContinue
    Write-Host "Git hooks removed" -ForegroundColor Yellow
}

function Invoke-Lint {
    & .\gradlew ktlintCheck
}

function Invoke-Format {
    & .\gradlew ktlintFormat
}

function Invoke-Verify {
    & .\gradlew verifyLocal
}

function Invoke-Preflight {
    Write-Host "Running preflight: format + verify" -ForegroundColor Cyan
    Invoke-Format
    Invoke-Verify
}

function Invoke-Arch {
    & .\gradlew :tests:arch:test
}

function Invoke-IdentityTests {
    & .\gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test
}

function Invoke-Smoke {
    & .\gradlew smokeIdentity
}

function Invoke-Build {
    & .\gradlew build
}

function Invoke-Clean {
    & .\gradlew clean
}

function Invoke-PreCommit {
    & pwsh -NoProfile -File scripts/hooks/pre-commit.ps1
}

function Invoke-PrePush {
    & pwsh -NoProfile -File scripts/hooks/pre-push.ps1
}

function Invoke-Push {
    Write-Host "Running pre-push checks..." -ForegroundColor Cyan
    Invoke-PrePush
    if ($Args) {
        git push @Args
    } else {
        git push
    }
}

# Execute requested task
switch ($Task.ToLower()) {
    "help" { Show-Help }
    "install-hooks" { Invoke-InstallHooks }
    "verify-hooks" { Invoke-VerifyHooks }
    "uninstall-hooks" { Invoke-UninstallHooks }
    "lint" { Invoke-Lint }
    "format" { Invoke-Format }
    "verify" { Invoke-Verify }
    "preflight" { Invoke-Preflight }
    "arch" { Invoke-Arch }
    "identity-tests" { Invoke-IdentityTests }
    "smoke" { Invoke-Smoke }
    "build" { Invoke-Build }
    "clean" { Invoke-Clean }
    "pre-commit" { Invoke-PreCommit }
    "pre-push" { Invoke-PrePush }
    "push" { Invoke-Push }
    default {
        Write-Host "Unknown task: $Task" -ForegroundColor Red
        Write-Host ""
        Show-Help
        exit 1
    }
}
