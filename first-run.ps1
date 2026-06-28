$ErrorActionPreference = 'Stop'

try {
    [Console]::OutputEncoding = [System.Text.UTF8Encoding]::new()
    $OutputEncoding = [System.Text.UTF8Encoding]::new()
} catch {
}

$root = $PSScriptRoot
$localConfig = Join-Path $root 'mail.local.ps1'
$exampleConfig = Join-Path $root 'mail.local.example.ps1'
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

    $suffix = if ($DefaultYes) { 'Y/n' } else { 'y/N' }
    $answer = Read-Host "$Prompt ($suffix)"

    if ([string]::IsNullOrWhiteSpace($answer)) {
        return $DefaultYes
    }

    return $answer.Trim().ToLowerInvariant().StartsWith('y')
}

function ConvertTo-PowerShellSingleQuoted {
    param([string]$Value)

    return "'" + ($Value -replace "'", "''") + "'"
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
Write-Host '这个脚本只生成本机配置和给出启动提示，不会把密码、授权码写入 Git。'

Write-Section '1. 环境检查'
$javaOk = Test-LocalCommand 'java' '请安装 JDK，并配置 JAVA_HOME 或 PATH。'
$nodeOk = Test-LocalCommand 'node' '请安装 Node.js。'
$npmOk = Test-LocalCommand 'npm.cmd' '请安装 Node.js 自带的 npm。'
$mysqlOk = Test-LocalCommand 'mysql' '请安装 MySQL，并把 mysql 命令加入 PATH；也可以用 MySQL Workbench 手动导入 SQL。'

Write-Section '2. 本地配置'
if ((Test-Path -LiteralPath $localConfig) -and (-not $Overwrite)) {
    Write-Host "已存在本地配置：$localConfig"
    Write-Host '如需重新生成，请执行：.\first-run.ps1 -Overwrite'
} else {
    if ((Test-Path -LiteralPath $localConfig) -and $Overwrite) {
        Write-Host '将重新生成 mail.local.ps1。'
    } elseif (Test-Path -LiteralPath $exampleConfig) {
        Write-Host "将基于示例配置生成：$localConfig"
    }

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

    $lines = @(
        '$env:DB_USERNAME = ' + (ConvertTo-PowerShellSingleQuoted $dbUser),
        '$env:DB_PASSWORD = ' + (ConvertTo-PowerShellSingleQuoted $dbPassword),
        '$env:DB_URL = ' + (ConvertTo-PowerShellSingleQuoted $dbUrl),
        '$env:ADMIN_PASSWORD = ' + (ConvertTo-PowerShellSingleQuoted $adminPassword),
        '$env:JWT_SECRET = ' + (ConvertTo-PowerShellSingleQuoted $jwtSecret),
        '',
        '$env:MAIL_HOST = ' + (ConvertTo-PowerShellSingleQuoted $mailHost),
        '$env:MAIL_PORT = ' + (ConvertTo-PowerShellSingleQuoted $mailPort),
        '$env:MAIL_USERNAME = ' + (ConvertTo-PowerShellSingleQuoted $mailUsername),
        '$env:MAIL_PASSWORD = ' + (ConvertTo-PowerShellSingleQuoted $mailPassword)
    )

    Set-Content -LiteralPath $localConfig -Value $lines -Encoding UTF8
    Write-Host "已生成本地配置：$localConfig"
}

Write-Section '3. 数据库准备'
Write-Host '请确认 MySQL 已启动，并导入初始化 SQL：'
Write-Host "mysql -u你的用户名 -p < `"$schemaFile`""
Write-Host 'SQL 会创建 kob 数据库和论坛所需表。后端启动时还会自动补充缺失字段。'
if (-not $mysqlOk) {
    Write-Host '当前未检测到 mysql 命令，可以用 MySQL Workbench、Navicat 等工具打开并执行该 SQL 文件。'
}

Write-Section '4. 前端依赖'
if (Test-Path -LiteralPath (Join-Path $frontendDir 'node_modules')) {
    Write-Host '已检测到 space/node_modules，通常无需重复安装。'
} else {
    Write-Host '首次运行前需要安装前端依赖：'
    Write-Host 'cd space'
    Write-Host 'npm install'

    if ($nodeOk -and $npmOk -and (Read-YesNo '是否现在执行 npm install')) {
        Push-Location $frontendDir
        try {
            npm.cmd install
        } finally {
            Pop-Location
        }
    }
}

Write-Section '5. 启动项目'
Write-Host '配置完成后，在项目根目录执行：'
Write-Host '.\start-all.ps1'
Write-Host ''
Write-Host '访问地址：'
Write-Host '前端：http://localhost:8081'
Write-Host '后端：http://localhost:3000'
Write-Host '后台：http://localhost:8081/#/admin'
Write-Host ''
Write-Host '关闭项目：'
Write-Host '.\stop-all.ps1'

if ((Test-Path -LiteralPath $startScript) -and (Read-YesNo '是否现在启动项目')) {
    & $startScript
}
