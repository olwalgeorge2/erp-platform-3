# ERP Platform Development Tasks
# https://github.com/casey/just

# Set PowerShell as the shell on Windows
set windows-shell := ["pwsh.exe", "-NoLogo", "-Command"]

# Default recipe to display help
default:
    @just --list

# Install git hooks for pre-commit and pre-push validation
install-hooks:
    @echo "Installing git hooks..."
    @pwsh -NoProfile -Command "if (Test-Path '.git/hooks') { Copy-Item 'scripts/hooks/pre-commit' '.git/hooks/pre-commit' -Force; Copy-Item 'scripts/hooks/pre-push' '.git/hooks/pre-push' -Force; Write-Host 'Git hooks installed successfully (bash versions for Git compatibility)' -ForegroundColor Green; Write-Host 'Run: just verify-hooks to test' -ForegroundColor Cyan } else { Write-Error 'Not a git repository' }"

# Verify git hooks are properly installed and functional
verify-hooks:
    @pwsh -NoProfile -File scripts/verify-hooks.ps1

# Uninstall git hooks
uninstall-hooks:
    @echo "Removing git hooks..."
    @pwsh -NoProfile -Command "Remove-Item '.git/hooks/pre-commit' -ErrorAction SilentlyContinue; Remove-Item '.git/hooks/pre-push' -ErrorAction SilentlyContinue; Write-Host 'Git hooks removed' -ForegroundColor Yellow"

# Run ktlint style check only
lint:
    ./gradlew ktlintCheck

# Auto-format code with ktlint
format:
    ./gradlew ktlintFormat

# Run full local verification (lint + arch tests + identity tests)
verify:
    ./gradlew verifyLocal

# Run architecture tests only
arch:
    ./gradlew :tests:arch:test

# Run identity infrastructure tests
identity-tests:
    ./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:test

# Run smoke tests for identity service
smoke:
    ./gradlew smokeIdentity

# Build all modules
build:
    ./gradlew build

# Clean build artifacts
clean:
    ./gradlew clean

# Run pre-commit checks manually
pre-commit:
    @pwsh -NoProfile -File scripts/hooks/pre-commit.ps1

# Run pre-push checks manually
pre-push:
    @pwsh -NoProfile -File scripts/hooks/pre-push.ps1

# Quick preflight before committing (format + verify)
preflight: format verify

# Git push with pre-push hook enforcement (explicit)
push:
    @echo "Running pre-push checks..."
    @just pre-push
    @git push

# Git push with arguments
push-args *args:
    @echo "Running pre-push checks..."
    @just pre-push
    @git push {{args}}
