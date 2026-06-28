$ErrorActionPreference = 'Continue'

$ports = @(3000, 8081)

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
