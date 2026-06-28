$ErrorActionPreference = 'Stop'

$root = $PSScriptRoot
$backendDir = Join-Path $root 'backend'
$frontendDir = Join-Path $root 'space'
$backendScript = Join-Path $backendDir 'run-backend.ps1'
$frontendScript = Join-Path $frontendDir 'run-space.ps1'
$logDir = Join-Path $root 'logs'
$mailLocalScript = Join-Path $root 'mail.local.ps1'

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

if (Test-Path -LiteralPath $mailLocalScript) {
    . $mailLocalScript
}

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
    -Port 3000 `
    -ScriptPath $backendScript `
    -WorkingDirectory $backendDir `
    -OutLog (Join-Path $logDir 'backend.out.log') `
    -ErrLog (Join-Path $logDir 'backend.err.log') `
    -ShowConsole

Start-ServiceScript `
    -Name 'Frontend' `
    -Port 8081 `
    -ScriptPath $frontendScript `
    -WorkingDirectory $frontendDir `
    -OutLog (Join-Path $logDir 'frontend.out.log') `
    -ErrLog (Join-Path $logDir 'frontend.err.log')

$deadline = (Get-Date).AddSeconds(60)
do {
    Start-Sleep -Seconds 2
    $backendReady = @(Get-ListeningPids -Port 3000).Count -gt 0
    $frontendReady = @(Get-ListeningPids -Port 8081).Count -gt 0
} until (($backendReady -and $frontendReady) -or (Get-Date) -ge $deadline)

Write-Host ''
if ($backendReady) {
    Write-Host 'Backend:  http://localhost:3000'
} else {
    Write-Host 'Backend did not start within 60 seconds. Check logs/backend.err.log'
}

if ($frontendReady) {
    Write-Host 'Frontend: http://localhost:8081'
    Write-Host 'Admin:    http://localhost:8081/#/admin'
} else {
    Write-Host 'Frontend did not start within 60 seconds. Check logs/frontend.err.log'
}
