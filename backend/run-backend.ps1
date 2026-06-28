$ErrorActionPreference = 'Stop'

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
