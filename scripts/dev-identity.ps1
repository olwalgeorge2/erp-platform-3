param(
  [int] $PreferredPort = 8181,
  [switch] $SkipPostgresCheck = $false
)

function Test-PortFree([int] $port) {
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
    $listener.Start(); $listener.Stop(); return $true
  } catch { return $false }
}

function Test-HostPostgreSQL {
  # Check for Windows PostgreSQL services
  $pgServices = Get-Service -Name "postgresql*" -ErrorAction SilentlyContinue | Where-Object { $_.Status -eq 'Running' }
  
  if ($pgServices) {
    Write-Host ""
    Write-Host "⚠️  WARNING: Host PostgreSQL service detected running!" -ForegroundColor Yellow
    Write-Host "   Service(s): $($pgServices.Name -join ', ')" -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   This will prevent connections to Docker PostgreSQL on localhost:5432" -ForegroundColor Yellow
    Write-Host "   causing 'password authentication failed' errors." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "   See docs/TROUBLESHOOTING_DATABASE.md for details" -ForegroundColor Cyan
    Write-Host ""
    
    $response = Read-Host "   Stop the host PostgreSQL service now? (y/N)"
    if ($response -eq 'y' -or $response -eq 'Y') {
      foreach ($service in $pgServices) {
        try {
          Write-Host "   Stopping $($service.Name)..." -ForegroundColor Yellow
          Stop-Service -Name $service.Name -Force -ErrorAction Stop
          Write-Host "   ✓ Stopped $($service.Name)" -ForegroundColor Green
        } catch {
          Write-Host "   ✗ Failed to stop $($service.Name): $($_.Exception.Message)" -ForegroundColor Red
          Write-Host "   Try running as Administrator" -ForegroundColor Yellow
        }
      }
      Write-Host ""
    } else {
      Write-Host "   ⚠️  Continuing anyway - expect database connection failures!" -ForegroundColor Yellow
      Write-Host ""
    }
  }
  
  # Check for PostgreSQL processes (even if service is not registered)
  $pgProcesses = Get-Process -Name "postgres" -ErrorAction SilentlyContinue
  if ($pgProcesses -and -not $pgServices) {
    Write-Host ""
    Write-Host "⚠️  WARNING: PostgreSQL process(es) detected but not as a service!" -ForegroundColor Yellow
    Write-Host "   Process count: $($pgProcesses.Count)" -ForegroundColor Yellow
    Write-Host "   This may conflict with Docker PostgreSQL on port 5432" -ForegroundColor Yellow
    Write-Host "   See docs/TROUBLESHOOTING_DATABASE.md for manual resolution" -ForegroundColor Cyan
    Write-Host ""
  }
}

function Test-DockerPostgreSQL {
  try {
    $container = docker ps --filter "name=erp-postgres" --filter "status=running" --format "{{.Names}}" 2>$null
    if (-not $container) {
      Write-Host ""
      Write-Host "⚠️  WARNING: Docker PostgreSQL container 'erp-postgres' is not running!" -ForegroundColor Yellow
      Write-Host "   Start it with: docker compose -f docker-compose-kafka.yml up -d postgres" -ForegroundColor Cyan
      Write-Host ""
      
      $response = Read-Host "   Start PostgreSQL container now? (y/N)"
      if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Host "   Starting PostgreSQL container..." -ForegroundColor Yellow
        docker compose -f docker-compose-kafka.yml up -d postgres
        Start-Sleep -Seconds 5
        Write-Host "   ✓ PostgreSQL container started" -ForegroundColor Green
        Write-Host ""
      } else {
        Write-Host "   ⚠️  Continuing anyway - expect database connection failures!" -ForegroundColor Yellow
        Write-Host ""
      }
    }
  } catch {
    Write-Host "⚠️  Could not check Docker status (Docker may not be running)" -ForegroundColor Yellow
  }
}

# Pre-flight checks
if (-not $SkipPostgresCheck) {
  Write-Host "=== Pre-flight Checks ===" -ForegroundColor Cyan
  Test-HostPostgreSQL
  Test-DockerPostgreSQL
  Write-Host "=========================" -ForegroundColor Cyan
  Write-Host ""
}

# Find available port
$port = $PreferredPort
for ($i = 0; $i -lt 20; $i++) {
  if (Test-PortFree $port) { break } else { $port++ }
}

$env:QUARKUS_HTTP_PORT = $port
$env:QUARKUS_ANALYTICS_DISABLED = 'true'

Write-Host "Launching identity-infrastructure on port $port (analytics disabled)" -ForegroundColor Green
& ./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev
