$ErrorActionPreference = 'Stop'

$root = Split-Path -Parent $PSScriptRoot
$envConfig = Join-Path $root '.env'

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return
    }

    Get-Content -LiteralPath $Path -Encoding UTF8 | ForEach-Object {
        $line = $_.Trim()
        if (-not $line -or $line.StartsWith('#')) {
            return
        }

        $index = $line.IndexOf('=')
        if ($index -le 0) {
            return
        }

        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        if (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'"))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
    }
}

Import-DotEnv -Path $envConfig

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

Write-Host '前端正在启动...'
Write-Host "当前端口：$frontendPort"

npm.cmd run serve -- --host 0.0.0.0 --port $frontendPort
