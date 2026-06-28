$ErrorActionPreference = 'Stop'

$backendPort = 3000
$frontendPort = 8081
$parsedPort = 0

if ([int]::TryParse($env:BACKEND_PORT, [ref]$parsedPort) -and $parsedPort -ge 1 -and $parsedPort -le 65535) {
    $backendPort = $parsedPort
}

$parsedPort = 0
if ([int]::TryParse($env:FRONTEND_PORT, [ref]$parsedPort) -and $parsedPort -ge 1 -and $parsedPort -le 65535) {
    $frontendPort = $parsedPort
}

$env:NODE_OPTIONS = '--use-system-ca'
if (-not $env:VUE_APP_API_BASE_URL) {
    $env:VUE_APP_API_BASE_URL = "http://localhost:$backendPort"
}

npm.cmd run serve -- --host 0.0.0.0 --port $frontendPort
