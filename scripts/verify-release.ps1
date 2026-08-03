[CmdletBinding()]
param(
    [string]$EnvironmentFile,
    [string]$FrontendEnvironmentFile,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backendDirectory = Join-Path $projectRoot 'backend'
$frontendDirectory = Join-Path $projectRoot 'frontend'

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $projectRoot '.env.local'
}
if ([string]::IsNullOrWhiteSpace($FrontendEnvironmentFile)) {
    $FrontendEnvironmentFile = Join-Path $frontendDirectory '.env.local'
}

function Read-EnvironmentFile {
    param([string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        throw "Environment file not found: $Path"
    }

    $values = @{}
    $lineNumber = 0
    foreach ($rawLine in Get-Content -LiteralPath $Path -Encoding UTF8) {
        $lineNumber++
        $line = $rawLine.Trim()
        if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
            continue
        }

        $separatorIndex = $line.IndexOf('=')
        if ($separatorIndex -lt 1) {
            throw "Invalid environment entry at ${Path}:$lineNumber"
        }

        $name = $line.Substring(0, $separatorIndex).Trim()
        $value = $line.Substring($separatorIndex + 1).Trim()
        if ($value.Length -ge 2 -and (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'"))
        )) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        $values[$name] = $value
    }
    return $values
}

function Test-ConfiguredValue {
    param(
        [hashtable]$Values,
        [string]$Name
    )

    if (-not $Values.ContainsKey($Name)) {
        return $false
    }
    $value = $Values[$Name]
    return -not [string]::IsNullOrWhiteSpace($value) -and $value -ne 'change-me'
}

function Assert-ConfiguredValues {
    param(
        [hashtable]$Values,
        [string[]]$Names,
        [string]$Scope
    )

    $missing = @($Names | Where-Object { -not (Test-ConfiguredValue $Values $_) })
    if ($missing.Count -gt 0) {
        throw "$Scope values are missing or use placeholders: $($missing -join ', ')"
    }
}

$backendValues = Read-EnvironmentFile $EnvironmentFile
$frontendValues = Read-EnvironmentFile $FrontendEnvironmentFile

Assert-ConfiguredValues $backendValues @(
    'ORACLE_URL',
    'ORACLE_USERNAME',
    'ORACLE_PASSWORD',
    'TOUR_API_SERVICE_KEY',
    'KAKAO_REST_API_KEY'
) 'Backend environment'
Assert-ConfiguredValues $frontendValues @(
    'VITE_API_BASE_URL',
    'VITE_KAKAO_MAP_KEY'
) 'Frontend environment'

if ($backendValues.ContainsKey('AUTH_ENFORCE_SECURITY') -and
    $backendValues.AUTH_ENFORCE_SECURITY -ne 'true') {
    throw 'AUTH_ENFORCE_SECURITY must be true for a release.'
}
if ($backendValues.ContainsKey('SESSION_COOKIE_SECURE') -and
    $backendValues.SESSION_COOKIE_SECURE -ne 'true') {
    throw 'SESSION_COOKIE_SECURE must be true for an HTTPS release.'
}
if ($frontendValues.VITE_API_BASE_URL -ne '/api') {
    Write-Warning 'VITE_API_BASE_URL is not /api. Confirm CORS and Cookie settings for a cross-origin API.'
}

Write-Host 'Release environment validation passed without printing secret values.'

if (-not $SkipBuild) {
    Push-Location $backendDirectory
    try {
        & (Join-Path $backendDirectory 'gradlew.bat') --no-daemon test bootJar --console=plain
        if ($LASTEXITCODE -ne 0) {
            throw "Backend release build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }

    foreach ($name in @('VITE_API_BASE_URL', 'VITE_API_PROXY_TARGET', 'VITE_KAKAO_MAP_KEY')) {
        if ($frontendValues.ContainsKey($name)) {
            [Environment]::SetEnvironmentVariable($name, $frontendValues[$name], 'Process')
        }
    }

    Push-Location $frontendDirectory
    try {
        & npm.cmd run test:unit -- --run
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend tests failed with exit code $LASTEXITCODE"
        }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) {
            throw "Frontend release build failed with exit code $LASTEXITCODE"
        }
    }
    finally {
        Pop-Location
    }
}

$jarPath = Join-Path $backendDirectory 'build/libs/travel-planner.jar'
$frontendIndexPath = Join-Path $frontendDirectory 'dist/index.html'
if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Release jar not found: $jarPath"
}
if (-not (Test-Path -LiteralPath $frontendIndexPath -PathType Leaf)) {
    throw "Frontend release entry not found: $frontendIndexPath"
}

Write-Host 'Release artifacts are ready: backend jar and frontend dist.'
