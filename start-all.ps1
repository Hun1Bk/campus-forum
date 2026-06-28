$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'space'
$backendScript = Join-Path $backendDir 'run-backend.ps1'
$frontendScript = Join-Path $frontendDir 'run-space.ps1'
$logDir = Join-Path $root 'logs'
$envConfigScript = Join-Path $root '.env'
$legacyLocalConfigScript = Join-Path $root 'local.config.ps1'
$legacyMailConfigScript = Join-Path $root 'mail.local.ps1'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Import-DotEnv {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
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

    return $true
}

function Import-ForumConfig {
    if (Import-DotEnv -Path $envConfigScript) {
        Write-Host "Loaded config: $envConfigScript"
        return
    }

    if (Test-Path -LiteralPath $legacyLocalConfigScript) {
        . $legacyLocalConfigScript
        Write-Host "Loaded legacy config: $legacyLocalConfigScript"
        return
    }

    if (Test-Path -LiteralPath $legacyMailConfigScript) {
        . $legacyMailConfigScript
        Write-Host "Loaded legacy config: $legacyMailConfigScript"
    }
}

function Get-ConfiguredPort {
    param(
        [string]$Name,
        [int]$Default
    )

    $raw = [Environment]::GetEnvironmentVariable($Name, 'Process')
    $port = 0
    if ([int]::TryParse($raw, [ref]$port) -and $port -ge 1 -and $port -le 65535) {
        return $port
    }
    return $Default
}

Import-ForumConfig
$backendPort = Get-ConfiguredPort -Name 'BACKEND_PORT' -Default 3000
$frontendPort = Get-ConfiguredPort -Name 'FRONTEND_PORT' -Default 8081

function Get-ListeningPids {
    param([int]$Port)

    $pattern = "^\s*TCP\s+\S+:$Port\s+\S+\s+LISTENING\s+(\d+)\s*$"
    netstat -ano |
        ForEach-Object {
            if ($_ -match $pattern) {
                [int]$Matches[1]
            }
        } |
        Select-Object -Unique
}

function Start-ServiceScript {
    param(
        [string]$Name,
        [int]$Port,
        [string]$ScriptPath,
        [string]$WorkingDirectory,
        [string]$OutLog,
        [string]$ErrLog,
        [switch]$ShowConsole
    )

    $pids = @(Get-ListeningPids -Port $Port)
    if ($pids.Count -gt 0) {
        Write-Host "$Name is already running on port $Port. PID: $($pids -join ', ')"
        return
    }

    if (-not (Test-Path -LiteralPath $ScriptPath)) {
        throw "$Name start script not found: $ScriptPath"
    }

    if ($ShowConsole) {
        Start-Process `
            -FilePath 'powershell.exe' `
            -ArgumentList @('-NoExit', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $ScriptPath) `
            -WorkingDirectory $WorkingDirectory
    } else {
        Start-Process `
            -FilePath 'powershell.exe' `
            -ArgumentList @('-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $ScriptPath) `
            -WorkingDirectory $WorkingDirectory `
            -WindowStyle Hidden `
            -RedirectStandardOutput $OutLog `
            -RedirectStandardError $ErrLog
    }

    Write-Host "Starting $Name on port $Port..."
}

Start-ServiceScript `
    -Name 'Backend' `
    -Port $backendPort `
    -ScriptPath $backendScript `
    -WorkingDirectory $backendDir `
    -OutLog (Join-Path $logDir 'backend.out.log') `
    -ErrLog (Join-Path $logDir 'backend.err.log') `
    -ShowConsole

Start-ServiceScript `
    -Name 'Frontend' `
    -Port $frontendPort `
    -ScriptPath $frontendScript `
    -WorkingDirectory $frontendDir `
    -OutLog (Join-Path $logDir 'frontend.out.log') `
    -ErrLog (Join-Path $logDir 'frontend.err.log')

$deadline = (Get-Date).AddSeconds(60)
do {
    Start-Sleep -Seconds 2
    $backendReady = @(Get-ListeningPids -Port $backendPort).Count -gt 0
    $frontendReady = @(Get-ListeningPids -Port $frontendPort).Count -gt 0
} until (($backendReady -and $frontendReady) -or (Get-Date) -ge $deadline)

Write-Host ''
if ($backendReady) {
    Write-Host "Backend:  http://localhost:$backendPort"
} else {
    Write-Host "Backend did not start within 60 seconds. Check logs/backend.err.log"
}

if ($frontendReady) {
    Write-Host "Frontend: http://localhost:$frontendPort"
    Write-Host "Admin:    http://localhost:$frontendPort/#/admin"
} else {
    Write-Host "Frontend did not start within 60 seconds. Check logs/frontend.err.log"
}
