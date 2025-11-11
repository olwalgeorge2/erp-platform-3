param(
  [int] $PreferredPort = 8080,
  [switch] $SkipRedisCheck = $false
)

function Test-PortFree([int] $port) {
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
    $listener.Start(); $listener.Stop(); return $true
  } catch { return $false }
}

function Test-RedisRunning {
  # Check if Redis container is running
  try {
    $redisContainer = docker ps --filter "name=erp-redis" --format "{{.Names}}" 2>$null
    if (-not $redisContainer) {
      Write-Host ""
      Write-Host "⚠️  WARNING: Redis container not running!" -ForegroundColor Yellow
      Write-Host "   The API Gateway health checks require Redis" -ForegroundColor Yellow
      Write-Host ""
      
      $response = Read-Host "   Start Redis container now? (y/N)"
      if ($response -eq 'y' -or $response -eq 'Y') {
        Write-Host "   Starting Redis..." -ForegroundColor Yellow
        docker compose -f docker-compose-kafka.yml up -d redis
        Start-Sleep -Seconds 3
        Write-Host "   ✓ Redis started" -ForegroundColor Green
        Write-Host ""
      } else {
        Write-Host "   ⚠️  Continuing anyway - readiness checks will fail!" -ForegroundColor Yellow
        Write-Host ""
      }
    }
  } catch {
    Write-Host "   Could not check Redis status: $($_.Exception.Message)" -ForegroundColor Yellow
  }
}

function Test-IdentityService {
  # Check if Identity service is running on port 8081
  try {
    $connection = Test-NetConnection -ComputerName localhost -Port 8081 -InformationLevel Quiet -WarningAction SilentlyContinue
    if (-not $connection) {
      Write-Host ""
      Write-Host "⚠️  WARNING: Identity service not running on port 8081!" -ForegroundColor Yellow
      Write-Host "   The API Gateway backend health check requires the identity service" -ForegroundColor Yellow
      Write-Host ""
      Write-Host "   Start it with: .\scripts\dev-identity.ps1 -PreferredPort 8081" -ForegroundColor Cyan
      Write-Host ""
      
      $response = Read-Host "   Continue without identity service? (y/N)"
      if ($response -ne 'y' -and $response -ne 'Y') {
        Write-Host "   Exiting. Start identity service first." -ForegroundColor Yellow
        exit 1
      }
      Write-Host ""
    }
  } catch {
    Write-Host "   Could not check identity service status" -ForegroundColor Yellow
  }
}

# Pre-flight checks
if (-not $SkipRedisCheck) {
  Test-RedisRunning
  Test-IdentityService
}

# Find available port
$port = $PreferredPort
while (-not (Test-PortFree $port)) {
  Write-Host "Port $port is in use, trying $(($port + 1))..." -ForegroundColor Yellow
  $port++
}

if ($port -ne $PreferredPort) {
  Write-Host "Using port $port instead of $PreferredPort" -ForegroundColor Cyan
}

Write-Host "Launching API Gateway on port $port" -ForegroundColor Green

# Set environment variables for the gateway
$env:QUARKUS_HTTP_PORT = $port
$env:QUARKUS_ANALYTICS_DISABLED = 'true'
$env:REDIS_URL = "redis://localhost:6379"
$env:IDENTITY_SERVICE_URL = "http://localhost:8081"

# Navigate to api-gateway and run
Set-Location api-gateway
..\gradlew.bat quarkusDev
