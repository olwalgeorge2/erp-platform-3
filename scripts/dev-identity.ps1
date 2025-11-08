param(
  [int] $PreferredPort = 8181
)

function Test-PortFree([int] $port) {
  try {
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, $port)
    $listener.Start(); $listener.Stop(); return $true
  } catch { return $false }
}

$port = $PreferredPort
for ($i = 0; $i -lt 20; $i++) {
  if (Test-PortFree $port) { break } else { $port++ }
}

$env:QUARKUS_HTTP_PORT = $port
$env:QUARKUS_ANALYTICS_DISABLED = 'true'

Write-Host "Launching identity-infrastructure on port $port (analytics disabled)" -ForegroundColor Green
& ./gradlew :bounded-contexts:tenancy-identity:identity-infrastructure:quarkusDev
