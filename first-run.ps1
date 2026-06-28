param(
    [switch]$Overwrite
)

$ErrorActionPreference = 'Stop'

try {
    [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding
    $OutputEncoding = New-Object System.Text.UTF8Encoding
} catch {
}

$root = $PSScriptRoot
$envConfig = Join-Path $root '.env'
$legacyLocalConfig = Join-Path $root 'local.config.ps1'
$legacyMailConfig = Join-Path $root 'mail.local.ps1'
$exampleConfig = Join-Path $root '.env.example'
$schemaFile = Join-Path $root 'backend\sql\forum_schema.sql'
$frontendDir = Join-Path $root 'space'
$startScript = Join-Path $root 'start-all.ps1'

function Write-Section {
    param([string]$Title)

    Write-Host ''
    Write-Host "== $Title =="
}

function Test-LocalCommand {
    param(
        [string]$Name,
        [string]$InstallHint
    )

    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if ($cmd) {
        Write-Host "[OK] ${Name}: $($cmd.Source)"
        return $true
    }

    Write-Host "[缺少] $Name：$InstallHint"
    return $false
}

function Read-WithDefault {
    param(
        [string]$Prompt,
        [string]$Default = ''
    )

    if ($Default) {
        $value = Read-Host "$Prompt [$Default]"
        if ([string]::IsNullOrWhiteSpace($value)) {
            return $Default
        }
        return $value.Trim()
    }

    $value = Read-Host $Prompt
    return $value.Trim()
}

function Read-PortWithDefault {
    param(
        [string]$Prompt,
        [int]$Default
    )

    while ($true) {
        $raw = Read-WithDefault $Prompt ([string]$Default)
        $port = 0
        if ([int]::TryParse($raw, [ref]$port) -and $port -ge 1 -and $port -le 65535) {
            return $port
        }
        Write-Host '端口必须是 1 到 65535 之间的数字。'
    }
}

function Read-SecretWithDefault {
    param(
        [string]$Prompt,
        [string]$Default = ''
    )

    if ($Default) {
        $secure = Read-Host "$Prompt [直接回车使用默认值]" -AsSecureString
    } else {
        $secure = Read-Host "$Prompt [可直接回车留空]" -AsSecureString
    }

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

    $suffix = 'y/N'
    if ($DefaultYes) {
        $suffix = 'Y/n'
    }
    $answer = Read-Host "$Prompt ($suffix)"

    if ([string]::IsNullOrWhiteSpace($answer)) {
        return $DefaultYes
    }

    return $answer.Trim().ToLowerInvariant().StartsWith('y')
}

function ConvertTo-DotEnvValue {
    param([string]$Value)

    if ($null -eq $Value) {
        return ''
    }
    return ($Value -replace "`r", '' -replace "`n", '')
}

function New-RandomSecret {
    $bytes = New-Object byte[] 48
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
    } finally {
        $rng.Dispose()
    }
    return [Convert]::ToBase64String($bytes)
}

Write-Host '校园论坛首次运行配置向导'
Write-Host '这个脚本会生成 .env 本地配置，不会把密码、授权码写入 Git。'

Write-Section '1. 环境检查'
$javaOk = Test-LocalCommand 'java' '请安装 JDK，并配置 JAVA_HOME 或 PATH。'
$nodeOk = Test-LocalCommand 'node' '请安装 Node.js。'
$npmOk = Test-LocalCommand 'npm.cmd' '请安装 Node.js 自带的 npm。'
$mysqlOk = Test-LocalCommand 'mysql' '请安装 MySQL，并把 mysql 命令加入 PATH；也可以用 MySQL Workbench 手动导入 SQL。'

Write-Section '2. 本地配置'
if ((Test-Path -LiteralPath $envConfig) -and (-not $Overwrite)) {
    Write-Host "已存在统一配置：$envConfig"
    Write-Host '如需重新生成，请执行：.\first-run.ps1 -Overwrite'
} else {
    if ((Test-Path -LiteralPath $envConfig) -and $Overwrite) {
        Write-Host '将重新生成 .env。'
    } elseif ((Test-Path -LiteralPath $legacyLocalConfig) -or (Test-Path -LiteralPath $legacyMailConfig)) {
        Write-Host '检测到旧 ps1 配置。启动脚本仍兼容读取它，但新配置会写入 .env。'
    } elseif (Test-Path -LiteralPath $exampleConfig) {
        Write-Host "将基于示例配置生成：$envConfig"
    }

    $backendPort = Read-PortWithDefault '后端端口' 3000
    do {
        $frontendPort = Read-PortWithDefault '前端端口' 8081
        if ($frontendPort -eq $backendPort) {
            Write-Host '前端端口不能和后端端口相同。'
        }
    } while ($frontendPort -eq $backendPort)

    $dbUser = Read-WithDefault '数据库用户名' 'root'
    $dbPassword = Read-SecretWithDefault '数据库密码'
    $dbUrl = Read-WithDefault '数据库连接地址' 'jdbc:mysql://localhost:3306/kob?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8'
    $adminPassword = Read-SecretWithDefault '默认站长 admin 的初始密码' 'admin123456'
    $jwtSecret = Read-WithDefault 'JWT 密钥' (New-RandomSecret)

    $mailHost = ''
    $mailPort = ''
    $mailUsername = ''
    $mailPassword = ''

    if (Read-YesNo '是否现在配置注册邮箱验证码 SMTP') {
        $mailHost = Read-WithDefault 'SMTP Host' 'smtp.qq.com'
        $mailPort = Read-WithDefault 'SMTP Port' '465'
        $mailUsername = Read-WithDefault '发件邮箱账号'
        $mailPassword = Read-SecretWithDefault 'SMTP 授权码'
    } else {
        Write-Host '已跳过 SMTP。未配置时，注册邮箱验证码接口会提示邮件服务未配置或发送失败。'
    }

    $lines = New-Object System.Collections.Generic.List[string]
    $lines.Add('BACKEND_PORT=' + (ConvertTo-DotEnvValue ([string]$backendPort))) | Out-Null
    $lines.Add('FRONTEND_PORT=' + (ConvertTo-DotEnvValue ([string]$frontendPort))) | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add('DB_USERNAME=' + (ConvertTo-DotEnvValue $dbUser)) | Out-Null
    $lines.Add('DB_PASSWORD=' + (ConvertTo-DotEnvValue $dbPassword)) | Out-Null
    $lines.Add('DB_URL=' + (ConvertTo-DotEnvValue $dbUrl)) | Out-Null
    $lines.Add('ADMIN_PASSWORD=' + (ConvertTo-DotEnvValue $adminPassword)) | Out-Null
    $lines.Add('JWT_SECRET=' + (ConvertTo-DotEnvValue $jwtSecret)) | Out-Null
    $lines.Add('') | Out-Null
    $lines.Add('MAIL_HOST=' + (ConvertTo-DotEnvValue $mailHost)) | Out-Null
    $lines.Add('MAIL_PORT=' + (ConvertTo-DotEnvValue $mailPort)) | Out-Null
    $lines.Add('MAIL_USERNAME=' + (ConvertTo-DotEnvValue $mailUsername)) | Out-Null
    $lines.Add('MAIL_PASSWORD=' + (ConvertTo-DotEnvValue $mailPassword)) | Out-Null

    $configText = [string]::Join([Environment]::NewLine, $lines.ToArray()) + [Environment]::NewLine
    $utf8Bom = New-Object System.Text.UTF8Encoding -ArgumentList $true
    [System.IO.File]::WriteAllText($envConfig, $configText, $utf8Bom)
    Write-Host "已生成统一配置：$envConfig"
    Write-Host '以后需要修改端口时，直接编辑 .env 中的 BACKEND_PORT 和 FRONTEND_PORT。'
}

Write-Section '3. 数据库准备'
Write-Host '请确认 MySQL 已启动，并导入初始化 SQL：'
Write-Host ('mysql -u你的用户名 -p < "' + $schemaFile + '"')
Write-Host 'SQL 会创建 kob 数据库和论坛所需表。后端启动时还会自动补充缺失字段。'
if (-not $mysqlOk) {
    Write-Host '当前未检测到 mysql 命令，可以用 MySQL Workbench、Navicat 等工具打开并执行该 SQL 文件。'
}

Write-Section '4. 前端依赖'
$nodeModulesDir = Join-Path $frontendDir 'node_modules'
if (Test-Path -LiteralPath $nodeModulesDir) {
    Write-Host '已检测到 space/node_modules，通常无需重复安装。'
} else {
    Write-Host '首次运行前需要安装前端依赖：'
    Write-Host 'cd space'
    Write-Host 'npm install --legacy-peer-deps'

    if ($nodeOk -and $npmOk -and (Read-YesNo '是否现在执行 npm install --legacy-peer-deps')) {
        Push-Location $frontendDir
        try {
            npm.cmd install --legacy-peer-deps
        } finally {
            Pop-Location
        }
    }
}

Write-Section '5. 启动项目'
Write-Host '配置完成后，在项目根目录执行：'
Write-Host '.\start-all.ps1'
Write-Host ''
Write-Host '访问地址默认如下；若修改 .env，请以脚本输出为准：'
Write-Host '前端：http://localhost:8081'
Write-Host '后端：http://localhost:3000'
Write-Host '后台：http://localhost:8081/#/admin'
Write-Host ''
Write-Host '关闭项目：'
Write-Host '.\stop-all.ps1'

$canStart = Test-Path -LiteralPath $startScript
if ($canStart -and (Read-YesNo '是否现在启动项目')) {
    & $startScript
}
