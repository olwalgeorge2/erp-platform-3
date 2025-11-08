Param()

Write-Host "[pre-commit] Running architecture governance suites (ADR-006, Layering, Hexagonal)" -ForegroundColor Cyan

if (-not (Test-Path "./gradlew.bat") -and -not (Test-Path "./gradlew")) {
  Write-Error "[pre-commit] gradlew/gradlew.bat not found. Aborting."
  exit 1
}

# Allow bypass via environment variable
if ($env:SKIP_ARCH_HOOK -eq '1') {
  Write-Host "[pre-commit] SKIP_ARCH_HOOK=1 set. Skipping architecture tests." -ForegroundColor Yellow
  exit 0
}

# Skip when only docs/markdown files are staged
$changed = git diff --cached --name-only --diff-filter=ACMRTUXB
if (-not $changed) {
  Write-Host "[pre-commit] No staged changes. Skipping." -ForegroundColor Yellow
  exit 0
}
$nonDocs = $changed | Where-Object { $_ -notmatch '^(docs/|README\.md|CONTRIBUTING\.md|.*\.md$)' }
# Further narrow to relevant code files/paths that can affect ArchUnit
$codeChanges = $nonDocs | Where-Object { $_ -match '^(bounded-contexts/|platform-shared/|platform-infrastructure/|api-gateway/|tests/arch/|buildSrc/|build\.gradle\.kts$|settings\.gradle\.kts$)|\.(kt|kts|java|gradle(\.kts)?)$' }
if (-not $codeChanges) {
  Write-Host "[pre-commit] No relevant code changes for architecture tests. Skipping." -ForegroundColor Yellow
  exit 0
}

$gradleCmd = if (Test-Path "./gradlew.bat") { "cmd /c gradlew.bat" } else { "./gradlew" }
$cmd = "$gradleCmd :tests:arch:test --no-daemon --stacktrace"
Write-Host "[pre-commit] Executing: $cmd"

Invoke-Expression $cmd

if ($LASTEXITCODE -ne 0) {
  Write-Error "[pre-commit] Architecture tests failed. Commit aborted."
  exit $LASTEXITCODE
}

Write-Host "[pre-commit] Architecture tests passed" -ForegroundColor Green
