Param(
  [switch]$Skip
)

Write-Host "[pre-push] Verifying style and key tests before push" -ForegroundColor Cyan

if ($Skip -or $env:SKIP_PREPUSH_CHECKS -eq '1') {
  Write-Host "[pre-push] Skipping checks (override)." -ForegroundColor Yellow
  exit 0
}

$gradle = if (Test-Path .\gradlew) { '.\gradlew' } elseif (Test-Path .\gradlew.bat) { '.\gradlew.bat' } else { $null }
if (-not $gradle) {
  Write-Error "[pre-push] gradlew/gradlew.bat not found. Aborting."
  exit 1
}

function Invoke-Step([string]$Name, [string]$Args) {
  Write-Host "[pre-push] $Name" -ForegroundColor Cyan
  & $gradle $Args
  if ($LASTEXITCODE -ne 0) { throw "Step failed: $Name" }
}

try {
  Invoke-Step -Name 'ktlintCheck' -Args 'ktlintCheck --no-daemon --stacktrace'
  Invoke-Step -Name 'Architecture tests' -Args ':tests:arch:test --no-daemon --stacktrace'
  Invoke-Step -Name 'Identity infra tests' -Args ':bounded-contexts:tenancy-identity:identity-infrastructure:test --no-daemon --stacktrace'
  Write-Host "[pre-push] All checks passed" -ForegroundColor Green
  exit 0
} catch {
  Write-Error "[pre-push] $_"; exit 1
}

