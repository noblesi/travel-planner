[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$SqlFile,
    [string]$EnvironmentFile
)

$ErrorActionPreference = 'Stop'

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $projectRoot '.env.local'
}

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw "Environment file not found: $EnvironmentFile"
}

$resolvedSqlFile = [System.IO.Path]::GetFullPath($SqlFile)
if (-not (Test-Path -LiteralPath $resolvedSqlFile -PathType Leaf)) {
    throw "SQL file not found: $resolvedSqlFile"
}

$environment = @{}
foreach ($rawLine in Get-Content -LiteralPath $EnvironmentFile -Encoding UTF8) {
    $line = $rawLine.Trim()
    if ([string]::IsNullOrWhiteSpace($line) -or $line.StartsWith('#')) {
        continue
    }
    $separatorIndex = $line.IndexOf('=')
    if ($separatorIndex -lt 1) {
        continue
    }
    $name = $line.Substring(0, $separatorIndex).Trim()
    $value = $line.Substring($separatorIndex + 1).Trim().Trim('"').Trim("'")
    $environment[$name] = $value
}

foreach ($requiredName in @('ORACLE_USERNAME', 'ORACLE_PASSWORD', 'ORACLE_HOST', 'ORACLE_PORT', 'ORACLE_SERVICE_NAME')) {
    if ([string]::IsNullOrWhiteSpace($environment[$requiredName])) {
        throw "Required Oracle setting is missing: $requiredName"
    }
}

$sqlPlus = Get-Command sqlplus -ErrorAction Stop
$connectIdentifier = "//$($environment.ORACLE_HOST):$($environment.ORACLE_PORT)/$($environment.ORACLE_SERVICE_NAME)"
$escapedPassword = $environment.ORACLE_PASSWORD.Replace('"', '""')

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $sqlPlus.Source
$startInfo.Arguments = '-S /nolog'
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardInput = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
$startInfo.CreateNoWindow = $true
[Environment]::SetEnvironmentVariable('NLS_LANG', 'KOREAN_KOREA.AL32UTF8', 'Process')

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
if (-not $process.Start()) {
    throw 'Failed to start SQL*Plus.'
}

$process.StandardInput.WriteLine('SET ECHO OFF')
$process.StandardInput.WriteLine('SET DEFINE OFF')
$process.StandardInput.WriteLine('WHENEVER SQLERROR EXIT SQL.SQLCODE ROLLBACK')
$process.StandardInput.WriteLine("CONNECT $($environment.ORACLE_USERNAME)/`"$escapedPassword`"@$connectIdentifier")
$process.StandardInput.WriteLine("@$resolvedSqlFile")
$process.StandardInput.WriteLine('EXIT')
$process.StandardInput.Close()

$standardOutput = $process.StandardOutput.ReadToEnd()
$standardError = $process.StandardError.ReadToEnd()
$process.WaitForExit()

$safeOutput = ($standardOutput + [Environment]::NewLine + $standardError).Replace(
    $environment.ORACLE_PASSWORD,
    '[REDACTED]'
).Trim()
if (-not [string]::IsNullOrWhiteSpace($safeOutput)) {
    Write-Output $safeOutput
}
if ($process.ExitCode -ne 0) {
    throw "SQL*Plus failed with exit code $($process.ExitCode)."
}
