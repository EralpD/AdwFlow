[CmdletBinding()]
param(
    [ValidateSet('start', 'migrate', 'info', 'test')]
    [string]$Action = 'start'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$envFile = Join-Path $projectRoot '.env'
$fileValues = @{}
$envText = ''
if (Test-Path -LiteralPath $envFile) {
    $envText = [System.IO.File]::ReadAllText($envFile)
    foreach ($line in ($envText -split '\r?\n')) {
        if ($line -match '^\s*(DB_NAME|DB_USERNAME|DB_PASSWORD|DB_PORT|DB_URL)\s*=(.*)$') {
            $fileValues[$matches[1]] = $matches[2].Trim()
        }
    }
}

function Get-DatabaseSetting([string]$Name, [string]$Default) {
    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if (-not [string]::IsNullOrWhiteSpace($value)) { return $value }
    if ($fileValues.ContainsKey($Name)) { return $fileValues[$Name] }
    return $Default
}

$databaseName = Get-DatabaseSetting 'DB_NAME' 'adwflow'
$databaseUser = Get-DatabaseSetting 'DB_USERNAME' 'adwflow'
$databasePort = Get-DatabaseSetting 'DB_PORT' '5432'
$databasePassword = Get-DatabaseSetting 'DB_PASSWORD' ''
if ($databaseName -notmatch '^[a-zA-Z_][a-zA-Z0-9_]*$' -or
    $databaseUser -notmatch '^[a-zA-Z_][a-zA-Z0-9_]*$' -or
    $databasePort -notmatch '^\d{1,5}$' -or [int]$databasePort -lt 1 -or [int]$databasePort -gt 65535) {
    throw 'Use simple database/user identifiers and a valid DB_PORT in the local Compose configuration.'
}
$localUrl = "jdbc:postgresql://127.0.0.1:${databasePort}/${databaseName}"
$databaseUrl = Get-DatabaseSetting 'DB_URL' $localUrl
if ($Action -eq 'start' -and $databaseUrl -ne $localUrl) {
    throw 'start only migrates its local Compose database. Remove DB_URL or use migrate explicitly for another database.'
}

if ([string]::IsNullOrWhiteSpace($databasePassword)) {
    if ($Action -ne 'start') { throw 'DB_PASSWORD is required. Run start once or configure your existing database credentials.' }
    $randomBytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    try { $rng.GetBytes($randomBytes) } finally { $rng.Dispose() }
    $databasePassword = [Convert]::ToBase64String($randomBytes)
    if ($envText -match '(?m)^\s*DB_PASSWORD\s*=') {
        $envText = [regex]::Replace($envText, '(?m)^[ \t]*DB_PASSWORD[ \t]*=[^\r\n]*', "DB_PASSWORD=$databasePassword")
    } else {
        $envText += "`r`nDB_PASSWORD=$databasePassword`r`n"
    }
    [System.IO.File]::WriteAllText($envFile, $envText, [System.Text.UTF8Encoding]::new($false))
    Write-Host 'Generated a local database password in the ignored .env file; existing keys were preserved.'
}

# Credentials go through the process environment, never command arguments or logs.
$settings = @{
    DB_NAME = $databaseName; DB_USERNAME = $databaseUser
    DB_PORT = $databasePort; DB_PASSWORD = $databasePassword
    FLYWAY_URL = $databaseUrl; FLYWAY_USER = $databaseUser; FLYWAY_PASSWORD = $databasePassword
}
$previous = @{}
Push-Location $projectRoot
try {
    foreach ($name in $settings.Keys) {
        $previous[$name] = [Environment]::GetEnvironmentVariable($name, 'Process')
        [Environment]::SetEnvironmentVariable($name, $settings[$name], 'Process')
    }
    if ($Action -eq 'start') {
        & docker compose up -d --wait postgres
        if ($LASTEXITCODE -ne 0) { throw 'PostgreSQL failed to start. Check Docker Desktop, the port and the existing volume credentials.' }
    }
    $maven = if (Get-Command mvn -ErrorAction SilentlyContinue) { 'mvn' } else { Join-Path $projectRoot 'mvnw.cmd' }
    switch ($Action) {
        'test' { & $maven -B '-Dtest=DatabaseMigrationTests' "-Dmigration.test.url=$databaseUrl" test }
        'info' { & $maven -B flyway:info }
        default { & $maven -B flyway:migrate }
    }
    if ($LASTEXITCODE -ne 0) { throw 'Database command failed. No volumes were deleted and no Flyway repair/clean was attempted.' }
} finally {
    foreach ($name in $previous.Keys) {
        [Environment]::SetEnvironmentVariable($name, $previous[$name], 'Process')
    }
    Pop-Location
}
