<#!
.SYNOPSIS
  Start/stop Prometheus + Grafana stack for local monitoring.

.PARAMETER Action
  up (default) | down | restart | logs
#>

param(
  [ValidateSet('up','down','restart','logs')]
  [string]$Action = 'up'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Ensure-Command($name) {
  if (-not (Get-Command $name -ErrorAction SilentlyContinue)) {
    throw "Required command '$name' not found in PATH. Please install Docker Desktop."
  }
}

Ensure-Command docker

$root = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
$compose = Join-Path $root 'monitoring/docker-compose.yml'
if (-not (Test-Path $compose)) { throw "Compose file not found: $compose" }

switch ($Action) {
  'up' { docker compose -f $compose up -d; Write-Host "Monitoring stack started (Prometheus:9090, Grafana:3000)" -ForegroundColor Green }
  'down' { docker compose -f $compose down; Write-Host "Monitoring stack stopped" -ForegroundColor Yellow }
  'restart' { docker compose -f $compose down; docker compose -f $compose up -d; Write-Host "Monitoring stack restarted" -ForegroundColor Green }
  'logs' { docker compose -f $compose logs -f }
}

