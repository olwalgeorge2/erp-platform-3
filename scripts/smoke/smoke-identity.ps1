Param(
  [string]$BaseUrl,
  [switch]$VerboseOutput
)

function Resolve-BaseUrl {
  Param([string]$Provided)
  if ($Provided) { return $Provided.TrimEnd('/') }
  if ($env:IDENTITY_BASE_URL) { return $env:IDENTITY_BASE_URL.TrimEnd('/') }
  $candidates = @('http://localhost:8181', 'http://localhost:8081')
  foreach ($c in $candidates) {
    try {
      $r = Invoke-WebRequest -Uri "$c/q/health" -UseBasicParsing -TimeoutSec 2 -Method GET -ErrorAction Stop
      if ($r.StatusCode -ge 200 -and $r.StatusCode -lt 500) { return $c }
    } catch { }
  }
  return 'http://localhost:8181'
}

function Test-Endpoint {
  Param([string]$Url, [int[]]$Expected=(200))
  try {
    $res = Invoke-WebRequest -Uri $Url -UseBasicParsing -TimeoutSec 5 -Method GET -ErrorAction Stop
    if ($VerboseOutput) { Write-Host ("[debug] {0} -> {1}" -f $Url, $res.StatusCode) -ForegroundColor DarkGray }
    return [pscustomobject]@{ Url=$Url; StatusCode=$res.StatusCode; Ok=($Expected -contains $res.StatusCode); Body=$res.Content }
  } catch {
    if ($VerboseOutput) { Write-Host ("[debug] {0} failed: {1}" -f $Url, $_.Exception.Message) -ForegroundColor DarkGray }
    return [pscustomobject]@{ Url=$Url; StatusCode=0; Ok=$false; Body='' }
  }
}

function Test-TcpPort {
  Param([string]$Host, [int]$Port)
  try {
    $r = Test-NetConnection -ComputerName $Host -Port $Port -WarningAction SilentlyContinue
    return [pscustomobject]@{ Host=$Host; Port=$Port; Reachable=$r.TcpTestSucceeded }
  } catch { return [pscustomobject]@{ Host=$Host; Port=$Port; Reachable=$false } }
}

Write-Host "[smoke] Tenancy-Identity + Kafka quick checks" -ForegroundColor Cyan
$base = Resolve-BaseUrl -Provided $BaseUrl
Write-Host "[smoke] Using base URL: $base" -ForegroundColor Cyan

# Identity service checks
$health = Test-Endpoint -Url "$base/q/health" -Expected @(200)
$templates = Test-Endpoint -Url "$base/api/roles/templates" -Expected @(200)

# Optional auth test
$tenant = $env:TENANT_ID
$user = $env:AUTH_USERNAME
$pass = $env:AUTH_PASSWORD

function Invoke-Login {
  Param([string]$Base, [string]$TenantId, [string]$Username, [string]$Password)
  $payload = @{ tenantId = $TenantId; usernameOrEmail = $Username; password = $Password } | ConvertTo-Json -Compress
  try {
    $res = Invoke-WebRequest -Uri "$Base/api/auth/login" -UseBasicParsing -TimeoutSec 5 -Method POST -ContentType 'application/json' -Body $payload -ErrorAction Stop
    return [pscustomobject]@{ StatusCode=$res.StatusCode; Ok=$true; Body=$res.Content }
  } catch {
    $code = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
    return [pscustomobject]@{ StatusCode=$code; Ok=$false; Body='' }
  }
}

if ($tenant -and $user -and $pass) {
  $login = Invoke-Login -Base $base -TenantId $tenant -Username $user -Password $pass
  Write-Host "[smoke] Login test (env creds): $($login.StatusCode)"
} else {
  # Negative login to validate endpoint wiring (expect 401)
  $login = Invoke-Login -Base $base -TenantId ([guid]::NewGuid().ToString()) -Username "nouser@example.com" -Password "wrongpass123!"
  Write-Host "[smoke] Login test (negative): $($login.StatusCode)"
}

# Kafka checks (network level + UI)
$kafkaPort = Test-TcpPort -Host 'localhost' -Port 9092
$kafkaUiPort = Test-TcpPort -Host 'localhost' -Port 8090
$kafkaUiHttp = Test-Endpoint -Url 'http://localhost:8090/' -Expected @(200,301,302)

# Summaries
Write-Host "[smoke] Identity health: $($health.StatusCode) (ok=$($health.Ok))"
Write-Host "[smoke] Role templates: $($templates.StatusCode) (ok=$($templates.Ok))"
Write-Host "[smoke] Kafka 9092 reachable: $($kafkaPort.Reachable)"
Write-Host "[smoke] Kafka UI 8090 reachable: $($kafkaUiPort.Reachable); HTTP=$($kafkaUiHttp.StatusCode)"

# Basic assertion logic
$fail = $false
if (-not $templates.Ok) { $fail = $true }
# If env creds were provided, require 200; otherwise accept 401/400 as valid negative
if ($tenant -and $user -and $pass) {
  if ($login.StatusCode -ne 200) { $fail = $true }
} else {
  if ($login.StatusCode -notin  @(400,401)) { $fail = $true }
}
# Health endpoint may be disabled; warn if not 200 but do not fail purely on that
if (-not $health.Ok) { Write-Host "[warn] /q/health not 200 (may be disabled)." -ForegroundColor Yellow }

if ($fail) {
  Write-Error "[smoke] One or more critical checks failed."
  exit 1
}

Write-Host "[smoke] All critical checks passed." -ForegroundColor Green
