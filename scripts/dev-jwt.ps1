<#!
.SYNOPSIS
  Generate a short-lived RS256 JWT for local dev.

.DESCRIPTION
  - Generates a 2048-bit RSA keypair on first run under scripts/keys/
  - Mints a JWT with roles (or groups) and writes it to scripts/tokens/dev.jwt
  - Prints the token to stdout

.PARAMETER Subject
  JWT subject (user id). Default: dev-user

.PARAMETER Roles
  Comma-separated roles (e.g. admin,user). Default: none

.PARAMETER Issuer
  JWT issuer. Default: erp-platform-dev

.PARAMETER Minutes
  Expiration in minutes from now. Default: 5
#>

param(
  [string]$Subject = "dev-user",
  [string]$Roles = "",
  [string]$Issuer = "erp-platform-dev",
  [int]$Minutes = 5
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

function Test-Command($name) { return [bool](Get-Command $name -ErrorAction SilentlyContinue) }

$useOpenssl = Test-Command openssl

$root = Split-Path -Parent $MyInvocation.MyCommand.Path | Split-Path -Parent
$keysDir = Join-Path $root 'scripts/keys'
$tokensDir = Join-Path $root 'scripts/tokens'
$privPath = Join-Path $keysDir 'dev-jwt-private.pem'
$pubPath  = Join-Path $keysDir 'dev-jwt-public.pem'
$outPath  = Join-Path $tokensDir 'dev.jwt'
$gatewayPubClasspath = Join-Path $root 'api-gateway\src\main\resources\keys\dev-jwt-public.pem'

New-Item -ItemType Directory -Force -Path $keysDir | Out-Null
New-Item -ItemType Directory -Force -Path $tokensDir | Out-Null

if (-not (Test-Path $privPath)) {
  Write-Host "[dev-jwt] Generating RSA keypair under $keysDir" -ForegroundColor Cyan
  if ($useOpenssl) {
    & openssl genrsa -out $privPath 2048 | Out-Null
    & openssl rsa -in $privPath -pubout -out $pubPath | Out-Null
  } else {
    # .NET RSA generation and PEM export
    $rsa = [System.Security.Cryptography.RSA]::Create(2048)
    $privDer = $rsa.ExportPkcs8PrivateKey()
    $pubDer  = $rsa.ExportSubjectPublicKeyInfo()
    function Write-Pem([string]$label,[byte[]]$bytes,[string]$path){
      $b64 = [System.Convert]::ToBase64String($bytes)
      $lines = ($b64 -split '(.{1,64})' | Where-Object { $_ -ne '' })
      $pem = @("-----BEGIN $label-----"); $pem += $lines; $pem += "-----END $label-----"
      Set-Content -Path $path -Value $pem -Encoding ascii
    }
    Write-Pem 'PRIVATE KEY' $privDer $privPath
    Write-Pem 'PUBLIC KEY'  $pubDer  $pubPath
  }
}

$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$exp = [DateTimeOffset]::UtcNow.AddMinutes($Minutes).ToUnixTimeSeconds()

$rolesArray = @()
if ($Roles -ne '') {
  $rolesArray = $Roles.Split(',') | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne '' }
}

$headerObj = [ordered]@{ alg = 'RS256'; typ = 'JWT' }
$payloadObj = [ordered]@{
  iss = $Issuer
  sub = $Subject
  iat = [int]$now
  exp = [int]$exp
}
if ($rolesArray.Count -gt 0) { $payloadObj.roles = $rolesArray }

function To-Base64Url([string]$text) {
  $bytes = [System.Text.Encoding]::UTF8.GetBytes($text)
  $b64 = [System.Convert]::ToBase64String($bytes)
  return ($b64 -replace '\+','-' -replace '/','_' -replace '=','')
}

$headerJson = ($headerObj | ConvertTo-Json -Compress)
$payloadJson = ($payloadObj | ConvertTo-Json -Compress)

$headerB64 = To-Base64Url $headerJson
$payloadB64 = To-Base64Url $payloadJson
$signingInput = "$headerB64.$payloadB64"

if ($useOpenssl) {
  $tmpFile = New-TemporaryFile
  Set-Content -Path $tmpFile -NoNewline -Value $signingInput -Encoding ascii
  $sigB64 = & openssl dgst -sha256 -sign $privPath -binary $tmpFile | openssl base64 -A
  $sigB64Url = ($sigB64 -replace '\+','-' -replace '/','_' -replace '=','')
} else {
  $rsa = [System.Security.Cryptography.RSA]::Create()
  $privPem = Get-Content -Raw -Path $privPath
  # Strip PEM header/footer
  $privB64 = ($privPem -replace '-----BEGIN [^-]+-----','' -replace '-----END [^-]+-----','' -replace '\s','')
  $privDer = [Convert]::FromBase64String($privB64)
  [void]$rsa.ImportPkcs8PrivateKey($privDer, [ref]0)
  $bytes = [System.Text.Encoding]::ASCII.GetBytes($signingInput)
  $sig = $rsa.SignData($bytes, [System.Security.Cryptography.HashAlgorithmName]::SHA256, [System.Security.Cryptography.RSASignaturePadding]::Pkcs1)
  $sigB64Url = ([Convert]::ToBase64String($sig) -replace '\+','-' -replace '/','_' -replace '=','')
}

$token = "$signingInput.$sigB64Url"

Set-Content -Path $outPath -NoNewline -Value $token
Write-Host "[dev-jwt] Token written to $outPath" -ForegroundColor Green
# Copy public key into gateway resources for classpath resolution
try {
  Copy-Item -Path $pubPath -Destination $gatewayPubClasspath -Force
  Write-Host "[dev-jwt] Public key copied to $gatewayPubClasspath" -ForegroundColor Green
} catch {
  Write-Host "[dev-jwt] Could not copy public key to gateway resources: $($_.Exception.Message)" -ForegroundColor Yellow
}
Write-Output $token
