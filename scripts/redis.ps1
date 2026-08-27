[CmdletBinding()]
param(
    [ValidateSet('setup', 'start', 'status', 'stop', 'check', 'test')]
    [string]$Action = 'start'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$envText = if (Test-Path -LiteralPath $envFile) { [IO.File]::ReadAllText($envFile) } else { '' }
$values = @{}
foreach ($line in ($envText -split '\r?\n')) {
    if ($line -match '^\s*(REDIS_[A-Z_]+|AUTH_HMAC_SECRET)\s*=(.*)$') {
        $values[$matches[1]] = $matches[2].Trim()
    }
}
function Get-Setting([string]$Name, [string]$Default) {
    $process = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($process)) { return $process }
    if ($values.ContainsKey($Name)) { return $values[$Name] }
    return $Default
}
function New-Secret {
    $bytes = New-Object byte[] 32
    $random = [Security.Cryptography.RandomNumberGenerator]::Create()
    try { $random.GetBytes($bytes) } finally { $random.Dispose() }
    return [Convert]::ToBase64String($bytes)
}

$settings = @{
    REDIS_HOST = Get-Setting 'REDIS_HOST' '127.0.0.1'
    REDIS_PORT = Get-Setting 'REDIS_PORT' '6379'
    REDIS_USERNAME = Get-Setting 'REDIS_USERNAME' 'default'
    REDIS_PASSWORD = Get-Setting 'REDIS_PASSWORD' ''
    AUTH_HMAC_SECRET = Get-Setting 'AUTH_HMAC_SECRET' ''
}
$changed = $false
foreach ($name in @('REDIS_PASSWORD', 'AUTH_HMAC_SECRET')) {
    if ([string]::IsNullOrWhiteSpace($settings[$name])) {
        if ($Action -notin @('setup', 'start')) { throw 'Run redis.ps1 setup to configure missing secrets first.' }
        $settings[$name] = New-Secret
        if ($envText -match "(?m)^[ \t]*$name[ \t]*=") {
            $envText = [regex]::Replace($envText, "(?m)^[ \t]*$name[ \t]*=[^\r\n]*", "$name=$($settings[$name])")
        } else {
            $envText += "`r`n$name=$($settings[$name])`r`n"
        }
        $changed = $true
    }
}
if ($settings.REDIS_PASSWORD -notmatch '^[A-Za-z0-9+/=_-]{32,128}$') {
    throw 'Local Compose expects REDIS_PASSWORD to be 32-128 Base64/URL-safe characters.'
}
try { $secretBytes = [Convert]::FromBase64String($settings.AUTH_HMAC_SECRET) }
catch { throw 'AUTH_HMAC_SECRET must be valid Base64.' }
if ($secretBytes.Length -lt 32) { throw 'AUTH_HMAC_SECRET must encode at least 32 random bytes.' }
if ($settings.REDIS_PORT -notmatch '^\d{1,5}$' -or [int]$settings.REDIS_PORT -lt 1 -or [int]$settings.REDIS_PORT -gt 65535) {
    throw 'REDIS_PORT must be a valid TCP port.'
}
if ($Action -in @('start', 'check', 'test') -and ($settings.REDIS_HOST -notin @('127.0.0.1', 'localhost') -or $settings.REDIS_USERNAME -ne 'default')) {
    throw 'This helper operates on the local Compose Redis only; configure remote Redis separately.'
}
if ($changed) {
    [IO.File]::WriteAllText($envFile, $envText, [Text.UTF8Encoding]::new($false))
    Write-Host 'Generated missing Redis/HMAC secrets in ignored .env. Existing credentials were preserved; no secrets were printed.'
}
if ($Action -eq 'setup') { Write-Host 'Redis settings are ready.'; return }

$previous = @{}
Push-Location $projectRoot
try {
    foreach ($name in $settings.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }
    switch ($Action) {
        'start' { & docker compose up -d --wait redis }
        'status' { & docker compose ps redis }
        'stop' { & docker compose stop redis }
        { $_ -in @('check', 'test') } {
            $maven = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $projectRoot 'mvnw.cmd' }
            # Tests use an isolated random prefix in DB 15; they never FLUSHDB/FLUSHALL.
            $previous['REDIS_IT_PASSWORD'] = [Environment]::GetEnvironmentVariable('REDIS_IT_PASSWORD', 'Process')
            [Environment]::SetEnvironmentVariable('REDIS_IT_PASSWORD', $settings.REDIS_PASSWORD, 'Process')
            $testClass = if ($Action -eq 'check') { 'RedisConnectivityIT' } else { 'SecurityRedisIT' }
            $mavenArguments = @('-B', "-Dtest=$testClass", "-Dredis.it.port=$($settings.REDIS_PORT)")
            & $maven @mavenArguments test
        }
    }
    if ($LASTEXITCODE -ne 0) { throw 'Redis command failed. No PostgreSQL containers or persistent volumes were changed.' }
} finally {
    foreach ($name in $previous.Keys) { [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process') }
    Pop-Location
}
