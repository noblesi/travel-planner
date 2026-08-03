[CmdletBinding()]
param(
    [string]$EnvironmentFile,
    [int]$Port = 18080
)

$ErrorActionPreference = 'Stop'

$projectRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$backendDirectory = Join-Path $projectRoot 'backend'
$jarPath = Join-Path $backendDirectory 'build/libs/travel-planner.jar'

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
    $EnvironmentFile = Join-Path $projectRoot '.env.local'
}

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
    throw "Environment file not found: $EnvironmentFile"
}

if (-not (Test-Path -LiteralPath $jarPath -PathType Leaf)) {
    throw "Backend jar not found: $jarPath. Run backend/gradlew.bat bootJar first."
}

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
    $value = $line.Substring($separatorIndex + 1).Trim()
    if (
        $value.Length -ge 2 -and
        (($value.StartsWith('"') -and $value.EndsWith('"')) -or
        ($value.StartsWith("'") -and $value.EndsWith("'")))
    ) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
}

[Environment]::SetEnvironmentVariable('SERVER_PORT', $Port.ToString(), 'Process')
[Environment]::SetEnvironmentVariable('AUTH_ENFORCE_SECURITY', 'true', 'Process')
[Environment]::SetEnvironmentVariable('SESSION_COOKIE_SECURE', 'false', 'Process')

$baseUrl = "http://127.0.0.1:$Port"
$logPath = Join-Path $backendDirectory 'build/p3-oracle-session.log'
$errorLogPath = Join-Path $backendDirectory 'build/p3-oracle-session-error.log'
$processArguments = @{
    FilePath = 'java'
    ArgumentList = @('-jar', $jarPath)
    WorkingDirectory = $backendDirectory
    WindowStyle = 'Hidden'
    RedirectStandardOutput = $logPath
    RedirectStandardError = $errorLogPath
    PassThru = $true
}
$serverProcess = Start-Process @processArguments

function Assert-Equal {
    param(
        $Actual,
        $Expected,
        [string]$Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message (expected=$Expected, actual=$Actual)"
    }
}

function Get-CsrfContext {
    $session = New-Object Microsoft.PowerShell.Commands.WebRequestSession
    $response = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/auth/csrf" -WebSession $session
    return @{
        Session = $session
        HeaderName = $response.data.headerName
        Token = $response.data.token
    }
}

function Update-CsrfContext {
    param([hashtable]$Context)

    $response = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/auth/csrf" -WebSession $Context.Session
    $Context.HeaderName = $response.data.headerName
    $Context.Token = $response.data.token
}

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Context,
        $Body
    )

    $arguments = @{
        Method = $Method
        Uri = $Uri
        WebSession = $Context.Session
        Headers = @{ $Context.HeaderName = $Context.Token }
    }
    if ($null -ne $Body) {
        $arguments.ContentType = 'application/json'
        $arguments.Body = $Body | ConvertTo-Json -Depth 8
    }
    return Invoke-RestMethod @arguments
}

try {
    $ready = $false
    for ($attempt = 0; $attempt -lt 45; $attempt++) {
        if ($serverProcess.HasExited) {
            throw "Backend exited during startup. See $logPath and $errorLogPath"
        }
        try {
            $health = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/health"
            if ($health.success) {
                $ready = $true
                break
            }
        }
        catch {
            Start-Sleep -Seconds 1
        }
    }
    if (-not $ready) {
        throw "Backend did not become ready. See $logPath and $errorLogPath"
    }

    $owner = Get-CsrfContext
    $ownerLogin = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/login" -Context $owner -Body @{
        email = 'e2e.owner@withtrip.test'
        password = 'WithTrip-E2E-2026!'
    }
    Assert-Equal $ownerLogin.data.authenticated $true 'Owner login failed'
    Update-CsrfContext -Context $owner

    $plan = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans" -Context $owner -Body @{
        regionCode = '1'
        startDate = '2026-08-10'
        endDate = '2026-08-11'
        visibility = 'PRIVATE'
    }
    $planId = $plan.data.planId
    $dayId = $plan.data.days[0].planDayId

    $metadata = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId" -Context $owner -Body @{
        title = 'P3 Oracle Session Verification'
        visibility = 'PRIVATE'
        versionNo = 0
    }
    Assert-Equal $metadata.data.plan.planId $planId 'Owner metadata update failed'

    $invitation = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans/$planId/invitations" -Context $owner -Body @{
        inviteeEmails = @('e2e.invitee@withtrip.test')
    }
    $token = $invitation.data.invitations[0].token

    Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/logout" -Context $owner -Body $null | Out-Null

    $invitee = Get-CsrfContext
    $inviteeLogin = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/login" -Context $invitee -Body @{
        email = 'e2e.invitee@withtrip.test'
        password = 'WithTrip-E2E-2026!'
    }
    Assert-Equal $inviteeLogin.data.authenticated $true 'Invitee login failed'
    Update-CsrfContext -Context $invitee

    $invitationDetail = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plan-invitations/$token" -WebSession $invitee.Session
    Assert-Equal $invitationDetail.data.status 'PENDING' 'Invitation lookup failed'

    $accepted = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plan-invitations/$token/accept" -Context $invitee -Body $null
    Assert-Equal $accepted.data.status 'ACCEPTED' 'Invitation acceptance failed'

    $editor = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId/editor" -WebSession $invitee.Session
    Assert-Equal $editor.data.plan.planId $planId 'Invited member editor access failed'

    $item = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans/$planId/days/$dayId/items" -Context $invitee -Body @{
        operationId = [Guid]::NewGuid().ToString()
        scheduleVersion = 0
        timeSlot = 'MORNING'
        placeProvider = 'TOUR_API'
        externalPlaceId = 'P3-ORACLE-1'
        placeName = 'P3 Verification Place'
        categoryName = 'Verification'
        address = 'Seoul'
        latitude = 37.5665
        longitude = 126.9780
        imageUrl = $null
        description = 'Temporary P3 session verification item'
    }
    Assert-Equal $item.data.resultScheduleVersion 1 'Invited member schedule edit failed'

    Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/logout" -Context $invitee -Body $null | Out-Null

    [ordered]@{
        result = 'PASS'
        ownerMemberId = $ownerLogin.data.member.memberId
        inviteeMemberId = $inviteeLogin.data.member.memberId
        planId = $planId
        scheduleItemId = $item.data.scheduleItemId
        invitationStatus = $accepted.data.status
        scheduleVersion = $item.data.resultScheduleVersion
    } | ConvertTo-Json
}
finally {
    if ($null -ne $serverProcess -and -not $serverProcess.HasExited) {
        Stop-Process -Id $serverProcess.Id -Force
        $serverProcess.WaitForExit()
    }
}
