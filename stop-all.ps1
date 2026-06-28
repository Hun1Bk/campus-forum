$ErrorActionPreference = 'Continue'

$root = $PSScriptRoot
$localConfigScript = Join-Path $root 'local.config.ps1'
$legacyConfigScript = Join-Path $root 'mail.local.ps1'

function Import-ForumConfig {
    if (Test-Path -LiteralPath $localConfigScript) {
        . $localConfigScript
        return
    }

    if (Test-Path -LiteralPath $legacyConfigScript) {
        . $legacyConfigScript
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
$ports = @(
    (Get-ConfiguredPort -Name 'BACKEND_PORT' -Default 3000),
    (Get-ConfiguredPort -Name 'FRONTEND_PORT' -Default 8081)
) | Select-Object -Unique

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

$allPids = @()
foreach ($port in $ports) {
    $pids = @(Get-ListeningPids -Port $port)
    if ($pids.Count -eq 0) {
        Write-Host "No process is listening on port $port."
        continue
    }

    foreach ($pidValue in $pids) {
        $allPids += $pidValue
        Write-Host "Found PID $pidValue on port $port."
    }
}

$allPids = @($allPids | Select-Object -Unique)
foreach ($pidValue in $allPids) {
    try {
        Stop-Process -Id $pidValue -Force -ErrorAction Stop
        Write-Host "Stopped PID $pidValue."
    } catch {
        Write-Host "Failed to stop PID $pidValue. Try running this script as administrator."
        Write-Host $_.Exception.Message
    }
}

if ($allPids.Count -eq 0) {
    Write-Host 'No Campus Forum services were running.'
} else {
    Write-Host 'Campus Forum services stopped.'
}
