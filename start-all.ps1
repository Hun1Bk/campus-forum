param(
    [ValidateSet("start", "stop", "restart", "config", "docker-start", "docker-stop", "docker-restart")]
    [string]$Action = "start",
    [switch]$Detached,
    [switch]$Overwrite
)

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot
$backendDir = Join-Path $root "backend"
$frontendDir = Join-Path $root "space"
$logDir = Join-Path $root "logs"
$envFile = Join-Path $root ".env"
$schemaFile = Join-Path $root "backend\sql\forum_schema.sql"

New-Item -ItemType Directory -Force -Path $logDir | Out-Null

function Load-EnvFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) {
        return $false
    }
    foreach ($rawLine in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith("#")) {
            continue
        }
        $index = $line.IndexOf("=")
        if ($index -le 0) {
            continue
        }
        $name = $line.Substring(0, $index).Trim()
        $value = $line.Substring($index + 1).Trim()
        if ($value.StartsWith('"') -and $value.EndsWith('"')) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
    return $true
}

function Get-PortValue {
    param(
        [string]$Name,
        [int]$Default
    )
    $raw = [Environment]::GetEnvironmentVariable($Name, "Process")
    if ($raw -match '^\d+$') {
        $port = [int]$raw
        if ($port -ge 1 -and $port -le 65535) {
            return $port
        }
    }
    return $Default
}

function Read-Text {
    param(
        [string]$Prompt,
        [string]$Default = ""
    )
    if ($Default) {
        $value = Read-Host "$Prompt [$Default]"
        if ([string]::IsNullOrWhiteSpace($value)) {
            return $Default
        }
        return $value.Trim()
    }
    return (Read-Host $Prompt).Trim()
}

function Read-SecretText {
    param(
        [string]$Prompt,
        [string]$Default = ""
    )
    $message = "$Prompt [可直接回车留空]"
    if ($Default) {
        $message = "$Prompt [直接回车使用默认值]"
    }
    $secure = Read-Host $message -AsSecureString
    if ($secure.Length -eq 0) {
        return $Default
    }
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Read-YesNo {
    param(
        [string]$Prompt,
        [bool]$DefaultYes = $false
    )
    $suffix = "y/N"
    if ($DefaultYes) {
        $suffix = "Y/n"
    }
    $answer = Read-Host "$Prompt ($suffix)"
    if ([string]::IsNullOrWhiteSpace($answer)) {
        return $DefaultYes
    }
    return $answer.Trim().ToLowerInvariant().StartsWith("y")
}

function Read-Port {
    param(
        [string]$Prompt,
        [int]$Default
    )
    while ($true) {
        $defaultText = [string]$Default
        $raw = Read-Text -Prompt $Prompt -Default $defaultText
        if ($raw -match '^\d+$') {
            $port = [int]$raw
            if ($port -ge 1 -and $port -le 65535) {
                return $port
            }
        }
        Write-Host "端口必须是 1 到 65535 之间的数字。"
    }
}

function New-Secret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

function Clean-EnvValue {
    param([string]$Value)
    if ($null -eq $Value) {
        return ""
    }
    return $Value.Replace("`r", "").Replace("`n", "")
}

function Run-ConfigWizard {
    Write-Host "校园论坛首次运行配置向导"
    Write-Host "这个向导会生成 .env 本地配置，不会把密码、授权码写入 Git。"
    Write-Host ""

    foreach ($cmd in @("java", "node", "npm.cmd", "mysql")) {
        $found = Get-Command $cmd -ErrorAction SilentlyContinue
        if ($found) {
            Write-Host "[OK] ${cmd}: $($found.Source)"
        } else {
            Write-Host "[缺少] $cmd：请安装或手动完成对应步骤。"
        }
    }

    if ((Test-Path -LiteralPath $envFile) -and (-not $Overwrite)) {
        Write-Host "已存在统一配置：$envFile"
        if (-not (Read-YesNo "是否重新生成 .env")) {
            return
        }
    }

    $backendPort = Read-Port -Prompt "后端端口" -Default 3000
    while ($true) {
        $frontendPort = Read-Port -Prompt "前端端口" -Default 8081
        if ($frontendPort -ne $backendPort) {
            break
        }
        Write-Host "前端端口不能和后端端口相同。"
    }

    $dbUser = Read-Text -Prompt "数据库用户名" -Default "root"
    $dbPassword = Read-SecretText -Prompt "数据库密码"
    $dbHost = Read-Text -Prompt "数据库主机" -Default "localhost"
    $dbPort = Read-Port -Prompt "数据库端口" -Default 3306
    $dbName = Read-Text -Prompt "数据库名称" -Default "kob"
    $dbUrl = Read-Text -Prompt "数据库连接地址（高级，可直接回车留空）"
    $adminPassword = Read-SecretText -Prompt "默认站长 admin 的初始密码" -Default "admin123456"
    $jwtSecret = Read-Text -Prompt "JWT 密钥" -Default (New-Secret)

    $mailHost = ""
    $mailPort = ""
    $mailUsername = ""
    $mailPassword = ""
    if (Read-YesNo "是否现在配置注册邮箱验证码 SMTP") {
        $mailHost = Read-Text -Prompt "SMTP Host" -Default "smtp.qq.com"
        $mailPort = Read-Text -Prompt "SMTP Port" -Default "465"
        $mailUsername = Read-Text -Prompt "发件邮箱账号"
        $mailPassword = Read-SecretText -Prompt "SMTP 授权码"
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add("BACKEND_PORT=" + (Clean-EnvValue ([string]$backendPort))) | Out-Null
    $lines.Add("FRONTEND_PORT=" + (Clean-EnvValue ([string]$frontendPort))) | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("DB_USERNAME=" + (Clean-EnvValue $dbUser)) | Out-Null
    $lines.Add("DB_PASSWORD=" + (Clean-EnvValue $dbPassword)) | Out-Null
    $lines.Add("DB_HOST=" + (Clean-EnvValue $dbHost)) | Out-Null
    $lines.Add("DB_PORT=" + (Clean-EnvValue ([string]$dbPort))) | Out-Null
    $lines.Add("DB_NAME=" + (Clean-EnvValue $dbName)) | Out-Null
    if ($dbUrl) {
        $lines.Add("DB_URL=" + (Clean-EnvValue $dbUrl)) | Out-Null
    } else {
        $lines.Add("# DB_URL=jdbc:mysql://localhost:3306/kob?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8&allowPublicKeyRetrieval=true&useSSL=false") | Out-Null
    }
    $lines.Add("ADMIN_PASSWORD=" + (Clean-EnvValue $adminPassword)) | Out-Null
    $lines.Add("JWT_SECRET=" + (Clean-EnvValue $jwtSecret)) | Out-Null
    $lines.Add("") | Out-Null
    $lines.Add("MAIL_HOST=" + (Clean-EnvValue $mailHost)) | Out-Null
    $lines.Add("MAIL_PORT=" + (Clean-EnvValue $mailPort)) | Out-Null
    $lines.Add("MAIL_USERNAME=" + (Clean-EnvValue $mailUsername)) | Out-Null
    $lines.Add("MAIL_PASSWORD=" + (Clean-EnvValue $mailPassword)) | Out-Null

    $text = [string]::Join([Environment]::NewLine, $lines.ToArray()) + [Environment]::NewLine
    $utf8Bom = New-Object System.Text.UTF8Encoding -ArgumentList $true
    [System.IO.File]::WriteAllText($envFile, $text, $utf8Bom)

    Write-Host "已生成统一配置：$envFile"
    Write-Host "请确认 MySQL 已启动，并导入初始化 SQL："
    Write-Host ('mysql -u你的用户名 -p < "' + $schemaFile + '"')
    Write-Host "启动项目：.\start-all.ps1"
    Write-Host "关闭项目：.\start-all.ps1 stop"
    Write-Host "重新配置：.\start-all.ps1 config"

    $nodeModules = Join-Path $frontendDir "node_modules"
    if (-not (Test-Path -LiteralPath $nodeModules)) {
        Write-Host "首次运行前需要安装前端依赖：cd space; npm install --legacy-peer-deps"
        if ((Get-Command npm.cmd -ErrorAction SilentlyContinue) -and (Read-YesNo "是否现在执行 npm install --legacy-peer-deps")) {
            Push-Location $frontendDir
            try {
                npm.cmd install --legacy-peer-deps
            } finally {
                Pop-Location
            }
        }
    }
}

function Ensure-Config {
    if (-not (Test-Path -LiteralPath $envFile)) {
        Run-ConfigWizard
    }
    if (-not (Load-EnvFile -Path $envFile)) {
        throw "未找到 .env 配置文件，请先执行：.\start-all.ps1 config"
    }
}

function Get-ListeningPids {
    param([int]$Port)
    try {
        return @(Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique)
    } catch {
        return @()
    }
}

function Stop-Local {
    param([int[]]$Ports)
    $allPids = @()
    foreach ($port in $Ports) {
        $allPids += @(Get-ListeningPids -Port $port)
    }
    $allPids = @($allPids | Select-Object -Unique)
    if ($allPids.Count -eq 0) {
        Write-Host "No Campus Forum services were running."
        return
    }
    foreach ($pidValue in $allPids) {
        try {
            Stop-Process -Id $pidValue -Force -ErrorAction Stop
            Write-Host "Stopped PID $pidValue."
        } catch {
            Write-Host "Failed to stop PID $pidValue."
            Write-Host $_.Exception.Message
        }
    }
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
        return [pscustomobject]@{ Id = $pids[0] }
    }
    $args = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-File", $ScriptPath)
    if ($ShowConsole) {
        $process = Start-Process -FilePath "powershell.exe" -ArgumentList $args -WorkingDirectory $WorkingDirectory -PassThru
    } else {
        $process = Start-Process -FilePath "powershell.exe" -ArgumentList $args -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -RedirectStandardOutput $OutLog -RedirectStandardError $ErrLog -PassThru
    }
    Write-Host "Starting $Name on port $Port. PID: $($process.Id)"
    return $process
}

function Start-Monitor {
    param(
        [int]$BackendPid,
        [int]$FrontendPid
    )
    $command = 'try { Wait-Process -Id ' + $BackendPid + ' -ErrorAction SilentlyContinue } catch { }; Start-Sleep -Seconds 1; try { Stop-Process -Id ' + $FrontendPid + ' -Force -ErrorAction SilentlyContinue } catch { }'
    $encoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($command))
    Start-Process -FilePath "powershell.exe" -ArgumentList @("-NoProfile", "-ExecutionPolicy", "Bypass", "-EncodedCommand", $encoded) -WindowStyle Hidden | Out-Null
}

function Start-Local {
    Ensure-Config
    $backendPort = Get-PortValue -Name "BACKEND_PORT" -Default 3000
    $frontendPort = Get-PortValue -Name "FRONTEND_PORT" -Default 8081
    if ($Action -eq "restart") {
        Stop-Local -Ports @($backendPort, $frontendPort)
    }

    $backendScript = Join-Path $backendDir "run-backend.ps1"
    $frontendScript = Join-Path $frontendDir "run-space.ps1"
    $backend = Start-ServiceScript -Name "Backend" -Port $backendPort -ScriptPath $backendScript -WorkingDirectory $backendDir -OutLog (Join-Path $logDir "backend.out.log") -ErrLog (Join-Path $logDir "backend.err.log") -ShowConsole
    $frontend = Start-ServiceScript -Name "Frontend" -Port $frontendPort -ScriptPath $frontendScript -WorkingDirectory $frontendDir -OutLog (Join-Path $logDir "frontend.out.log") -ErrLog (Join-Path $logDir "frontend.err.log")

    if ($backend -and $frontend) {
        Start-Monitor -BackendPid $backend.Id -FrontendPid $frontend.Id
        Write-Host "Backend window monitor started. Closing the backend window will stop frontend too."
    }

    Write-Host ""
    Write-Host "Backend:  http://localhost:$backendPort"
    Write-Host "Frontend: http://localhost:$frontendPort"
    Write-Host "Admin:    http://localhost:$frontendPort/#/admin"
}

function Invoke-Docker {
    param([string]$DockerAction)
    if ($DockerAction -ne "docker-stop") {
        Ensure-Config
    } elseif (Test-Path -LiteralPath $envFile) {
        Load-EnvFile -Path $envFile | Out-Null
    }
    Push-Location $root
    try {
        if ($DockerAction -eq "docker-stop") {
            docker compose down
            return
        }
        if ($DockerAction -eq "docker-restart") {
            docker compose down
        }
        if ($Detached) {
            docker compose up -d --build
        } else {
            try {
                docker compose up --build
            } finally {
                docker compose down
            }
        }
    } finally {
        Pop-Location
    }
}

if ($Action -eq "config") {
    Run-ConfigWizard
    return
}

if ($Action -like "docker-*") {
    Invoke-Docker -DockerAction $Action
    return
}

if (Test-Path -LiteralPath $envFile) {
    Load-EnvFile -Path $envFile | Out-Null
}
$backendPortForStop = Get-PortValue -Name "BACKEND_PORT" -Default 3000
$frontendPortForStop = Get-PortValue -Name "FRONTEND_PORT" -Default 8081

if ($Action -eq "stop") {
    Stop-Local -Ports @($backendPortForStop, $frontendPortForStop)
    return
}

Start-Local

