Param()

Write-Host "[pre-commit] Running code style and architecture governance checks" -ForegroundColor Cyan

if (-not (Test-Path "./gradlew.bat") -and -not (Test-Path "./gradlew")) {
  Write-Error "[pre-commit] gradlew/gradlew.bat not found. Aborting."
  exit 1
}

# Allow bypass via environment variable
if ($env:SKIP_ARCH_HOOK -eq '1') {
  Write-Host "[pre-commit] SKIP_ARCH_HOOK=1 set. Skipping all pre-commit checks." -ForegroundColor Yellow
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

# Check for Kotlin files specifically for ktlint
$kotlinFiles = $changed | Where-Object { $_ -match '\.(kt|kts)$' }

$gradleCmd = if (Test-Path "./gradlew.bat") { "cmd /c gradlew.bat" } else { "./gradlew" }

# Run ktlint if Kotlin files changed
if ($kotlinFiles) {
  Write-Host "[pre-commit] Step 1/2: Running ktlint on Kotlin files..." -ForegroundColor Cyan
  $lintCmd = "$gradleCmd ktlintCheck --no-daemon --stacktrace"
  Invoke-Expression $lintCmd
  
  if ($LASTEXITCODE -ne 0) {
    Write-Error "[pre-commit] ktlint check failed. Run './gradlew ktlintFormat' to auto-fix, then commit again."
    exit $LASTEXITCODE
  }
  Write-Host "[pre-commit] ✓ ktlint passed" -ForegroundColor Green
} else {
  Write-Host "[pre-commit] No Kotlin files changed, skipping ktlint" -ForegroundColor Yellow
}

# Run architecture tests if relevant code changed
if (-not $codeChanges) {
  Write-Host "[pre-commit] No relevant code changes for architecture tests. Skipping." -ForegroundColor Yellow
  exit 0
}

Write-Host "[pre-commit] Step 2/2: Running architecture tests..." -ForegroundColor Cyan
$archCmd = "$gradleCmd :tests:arch:test --no-daemon --stacktrace"
Invoke-Expression $archCmd

if ($LASTEXITCODE -ne 0) {
  Write-Error "[pre-commit] Architecture tests failed. Commit aborted."
  exit $LASTEXITCODE
}

Write-Host "[pre-commit] ✓ Architecture tests passed" -ForegroundColor Green
Write-Host "[pre-commit] All checks passed! ✓" -ForegroundColor Green
