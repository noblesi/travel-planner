[CmdletBinding()]
param(
    [string]$EnvironmentFile,

    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArguments
)

$ErrorActionPreference = 'Stop'

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backendDirectory = Join-Path $projectRoot 'backend'
$gradleWrapper = Join-Path $backendDirectory 'gradlew.bat'

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $projectRoot '.env.local'
}

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw "Environment file not found: $EnvironmentFile`nCopy .env.example to .env.local and enter the shared local values."
}

if (-not (Test-Path -LiteralPath $gradleWrapper -PathType Leaf)) {
    throw "Gradle Wrapper not found: $gradleWrapper"
}

$backendVariableNames = @(
    'ORACLE_URL',
    'ORACLE_USERNAME',
    'ORACLE_PASSWORD',
    'SERVER_PORT',
    'SERVER_FORWARD_HEADERS_STRATEGY',
    'SESSION_COOKIE_SECURE',
    'AUTH_ENFORCE_SECURITY',
    'LOCAL_MEMBER_ID',
    'TOUR_API_BASE_URL',
    'TOUR_API_SERVICE_KEY',
    'TOUR_API_MOBILE_APP',
    'TOUR_API_CONNECT_TIMEOUT',
    'TOUR_API_READ_TIMEOUT',
    'KAKAO_REST_BASE_URL',
    'KAKAO_REST_API_KEY',
    'KAKAO_REST_CONNECT_TIMEOUT',
    'KAKAO_REST_READ_TIMEOUT',
    'KAKAO_JAVASCRIPT_KEY',
    'MAIL_HOST',
    'MAIL_PORT',
    'MAIL_USERNAME',
    'MAIL_PASSWORD',
    'MAIL_FROM',
    # application.yml의 SMTP timeout 변수를 전달해 로컬 실행도 운영과 같은 bounded I/O 설정을 사용한다.
    'MAIL_CONNECTION_TIMEOUT_MS',
    'MAIL_READ_TIMEOUT_MS',
    'MAIL_WRITE_TIMEOUT_MS',
    'MAIL_ASYNC_CORE_POOL_SIZE',
    'MAIL_ASYNC_MAX_POOL_SIZE',
    'MAIL_ASYNC_QUEUE_CAPACITY',
    'MAIL_ASYNC_AWAIT_TERMINATION',
    'FRONTEND_BASE_URL'
)

$loadedVariableNames = [System.Collections.Generic.HashSet[string]]::new(
    [System.StringComparer]::OrdinalIgnoreCase
)
$lineNumber = 0

foreach ($rawLine in Get-Content -LiteralPath $EnvironmentFile -Encoding UTF8) {
    $lineNumber++
    $line = $rawLine.Trim()

    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        continue
    }

    $separatorIndex = $line.IndexOf('=')
    if ($separatorIndex -lt 1) {
        throw "Invalid environment entry at line $lineNumber. Use KEY=VALUE format."
    }

    $name = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim()

    if ($name -notmatch '^[A-Za-z_][A-Za-z0-9_]*$') {
        throw "Invalid environment variable name at line $lineNumber`: $name"
    }

    if (
        $value.Length -ge 2 -and
        (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'")))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }

    if ($backendVariableNames -contains $name) {
        [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        [void]$loadedVariableNames.Add($name)
    }
}

function Test-ConfiguredValue {
    param([string]$Name)

    $value = [Environment]::GetEnvironmentVariable($Name, 'Process')
    return -not [string]::IsNullOrWhiteSpace($value) -and $value -ne 'change-me'
}

$requiredVariableNames = @('ORACLE_URL', 'ORACLE_USERNAME', 'ORACLE_PASSWORD')
$missingRequiredVariables = @($requiredVariableNames | Where-Object { -not (Test-ConfiguredValue $_) })

if ($missingRequiredVariables.Count -gt 0) {
    throw "Required environment variables are missing or still use example values: $($missingRequiredVariables -join ', ')"
}

$optionalSecretNames = @('TOUR_API_SERVICE_KEY', 'KAKAO_REST_API_KEY')
$missingOptionalSecrets = @($optionalSecretNames | Where-Object { -not (Test-ConfiguredValue $_) })

Write-Host "Loaded $($loadedVariableNames.Count) backend environment variables from: $EnvironmentFile"
if ($missingOptionalSecrets.Count -gt 0) {
    Write-Warning "Optional external API values are missing: $($missingOptionalSecrets -join ', ')"
}
Write-Host 'Starting the Spring Boot backend without printing environment values.'

$exitCode = 1
Push-Location $backendDirectory
try {
    & $gradleWrapper bootRun @GradleArguments
    $exitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}

exit $exitCode
