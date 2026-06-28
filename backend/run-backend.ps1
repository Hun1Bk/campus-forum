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

if ($env:JAVA_HOME) {
    $javaBin = Join-Path $env:JAVA_HOME 'bin'
    if (Test-Path -LiteralPath $javaBin) {
        $env:Path = "$javaBin;$env:Path"
    }
}

$backendPort = 3000
$parsedPort = 0
if ([int]::TryParse($env:BACKEND_PORT, [ref]$parsedPort) -and $parsedPort -ge 1 -and $parsedPort -le 65535) {
    $backendPort = $parsedPort
}

Write-Host '后端正在启动...'
Write-Host "当前端口：$backendPort"

$runArgs = @()
if ($env:DB_PASSWORD) {
    $runArgs += "--spring.datasource.password=$env:DB_PASSWORD"
}
if ($env:DB_USERNAME) {
    $runArgs += "--spring.datasource.username=$env:DB_USERNAME"
}
if ($env:DB_URL) {
    $runArgs += "--spring.datasource.url=$env:DB_URL"
}
$runArgs += "--server.port=$backendPort"

$jar = Join-Path $PSScriptRoot 'target\backend-0.0.1-SNAPSHOT.jar'
if (Test-Path -LiteralPath $jar) {
    java -jar $jar @runArgs
} else {
    $springArgs = $runArgs -join ' '
    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=$springArgs"
}
