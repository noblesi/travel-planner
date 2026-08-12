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

function Assert-NotNull {
    param(
        $Actual,
        [string]$Message
    )

    if ($null -eq $Actual) {
        throw $Message
    }
}

function Invoke-ExpectJsonError {
    param(
        [scriptblock]$Action,
        [int]$ExpectedStatus,
        [string]$ExpectedCode,
        [string]$Message
    )

    try {
        & $Action | Out-Null
    }
    catch {
        $response = $_.Exception.Response
        if ($null -eq $response) {
            throw "$Message (HTTP response was not available: $($_.Exception.Message))"
        }

        $actualStatus = [int]$response.StatusCode
        $rawBody = $_.ErrorDetails.Message
        if ([string]::IsNullOrWhiteSpace($rawBody)) {
            $reader = New-Object System.IO.StreamReader($response.GetResponseStream())
            try {
                $rawBody = $reader.ReadToEnd()
            }
            finally {
                $reader.Dispose()
            }
        }
        $payload = $rawBody | ConvertFrom-Json

        Assert-Equal $actualStatus $ExpectedStatus "$Message status mismatch"
        Assert-Equal $payload.code $ExpectedCode "$Message code mismatch"
        return $payload
    }

    throw "$Message (request unexpectedly succeeded)"
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

    $today = [TimeZoneInfo]::ConvertTimeBySystemTimeZoneId(
        [DateTimeOffset]::UtcNow,
        'Korea Standard Time'
    ).Date
    $startDate = $today.AddDays(3).ToString('yyyy-MM-dd')
    $initialEndDate = $today.AddDays(4).ToString('yyyy-MM-dd')
    $extendedEndDate = $today.AddDays(5).ToString('yyyy-MM-dd')

    $plan = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans" -Context $owner -Body @{
        regionCode = '1'
        startDate = $startDate
        endDate = $initialEndDate
        visibility = 'PUBLIC'
    }
    $planId = $plan.data.planId
    $dayId = $plan.data.days[0].planDayId
    Assert-Equal $plan.data.publishStatus 'DRAFT' 'New plan must start as draft'
    Assert-Equal $plan.data.versionNo 0 'New plan version mismatch'

    $myPlans = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/mine" -WebSession $owner.Session
    $ownedPlan = @($myPlans.data.plans | Where-Object { $_.planId -eq $planId })[0]
    Assert-NotNull $ownedPlan 'New plan was not returned by my plans'
    Assert-Equal $ownedPlan.currentMemberRole 'CREATOR' 'My plans owner role mismatch'
    Assert-Equal $ownedPlan.publishStatus 'DRAFT' 'My plans draft status mismatch'

    Invoke-ExpectJsonError -ExpectedStatus 409 -ExpectedCode 'PLAN_SCHEDULE_REQUIRED' `
        -Message 'Empty plan publication must be rejected' -Action {
            Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId/publication" -Context $owner -Body @{
                publishStatus = 'PUBLISHED'
                versionNo = 0
            }
        } | Out-Null

    $dates = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId/dates" -Context $owner -Body @{
        startDate = $startDate
        endDate = $extendedEndDate
        versionNo = 0
        force = $false
    }
    Assert-Equal $dates.data.plan.endDate $extendedEndDate 'Oracle date extension failed'
    Assert-Equal $dates.data.plan.versionNo 1 'Date extension version mismatch'

    $verificationTitle = "P0 Oracle Lifecycle $planId"
    $metadata = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId" -Context $owner -Body @{
        title = $verificationTitle
        visibility = 'PUBLIC'
        versionNo = 1
    }
    Assert-Equal $metadata.data.plan.planId $planId 'Owner metadata update failed'
    Assert-Equal $metadata.data.plan.versionNo 2 'Metadata update version mismatch'

    $invitation = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans/$planId/invitations" -Context $owner -Body @{
        inviteeEmails = @('e2e.invitee@withtrip.test')
    }
    $token = $invitation.data.invitations[0].token

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

	# Pre-encoded UTF-8 for the Korean keyword so Windows PowerShell 5 can parse this UTF-8 file without a BOM.
	$placeKeyword = '%EA%B2%BD%EB%B3%B5%EA%B6%81'
	$placeSearch = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/places/search?keyword=$placeKeyword&page=1&size=10" -WebSession $invitee.Session
	$verifiedPlace = $null
	foreach ($candidate in @($placeSearch.data.places)) {
		if ([string]::IsNullOrWhiteSpace($candidate.imageUrl)) {
			continue
		}
		if ($candidate.placeType -eq 'RESTAURANT' -or $candidate.placeType -eq 'ACCOMMODATION') {
			continue
		}
		$verifiedPlace = $candidate
		break
	}
    Assert-NotNull $verifiedPlace 'TourAPI did not return an image-backed place for thumbnail verification'

    $item = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans/$planId/days/$dayId/items" -Context $invitee -Body @{
        operationId = [Guid]::NewGuid().ToString()
        scheduleVersion = 0
        timeSlot = 'MORNING'
		placeProvider = $verifiedPlace.placeProvider
		externalPlaceId = $verifiedPlace.externalPlaceId
		placeName = 'Client-controlled name must be ignored'
		categoryName = 'CLIENT_CONTROLLED_CATEGORY'
		address = 'Client-controlled address'
		latitude = 0
		longitude = 0
		imageUrl = 'https://invalid.example/client-controlled.jpg'
		description = 'Client-controlled description'
    }
    Assert-Equal $item.data.resultScheduleVersion 1 'Invited member schedule edit failed'
	Assert-Equal $item.data.editor.days[0].items[0].placeName $verifiedPlace.placeName 'Server place snapshot was not used'
	Assert-Equal $item.data.editor.days[0].items[0].imageUrl $verifiedPlace.imageUrl 'Server image snapshot was not used'

    $published = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId/publication" -Context $owner -Body @{
        publishStatus = 'PUBLISHED'
        versionNo = 2
    }
    Assert-Equal $published.data.plan.publishStatus 'PUBLISHED' 'Plan publication failed'
    Assert-Equal $published.data.plan.versionNo 3 'Plan publication version mismatch'
	Assert-Equal $published.data.plan.thumbnailImageUrl $verifiedPlace.imageUrl 'Published thumbnail mismatch'

    $publicDetail = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId"
    Assert-Equal $publicDetail.data.plan.planId $planId 'Published plan detail was not available'
    Assert-Equal $publicDetail.data.plan.title $verificationTitle 'Published plan title mismatch'

    $updatedTitle = "$verificationTitle Updated"
    $liveUpdate = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId" -Context $owner -Body @{
        title = $updatedTitle
        visibility = 'PUBLIC'
        versionNo = 3
    }
    Assert-Equal $liveUpdate.data.plan.publishStatus 'PUBLISHED' 'Live edit changed publication status'
    Assert-Equal $liveUpdate.data.plan.versionNo 4 'Live edit version mismatch'

    $updatedPublicDetail = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId"
    Assert-Equal $updatedPublicDetail.data.plan.title $updatedTitle 'Published auto-save was not reflected immediately'

    $searchKeyword = [Uri]::EscapeDataString($updatedTitle)
    $search = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans?keyword=$searchKeyword&page=1&size=24"
    $searchedPlan = @($search.data.plans | Where-Object { $_.planId -eq $planId })[0]
    Assert-NotNull $searchedPlan 'Published plan was not returned by public search'

    $draft = Invoke-JsonRequest -Method Patch -Uri "$baseUrl/api/plans/$planId/publication" -Context $owner -Body @{
        publishStatus = 'DRAFT'
        versionNo = 4
    }
    Assert-Equal $draft.data.plan.publishStatus 'DRAFT' 'Draft transition failed'
    Assert-Equal $draft.data.plan.versionNo 5 'Draft transition version mismatch'

    Invoke-ExpectJsonError -ExpectedStatus 404 -ExpectedCode 'PLAN_NOT_FOUND' `
        -Message 'Draft plan must not have a public detail' -Action {
            Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId"
        } | Out-Null

    $draftSearch = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans?keyword=$searchKeyword&page=1&size=24"
    $draftSearchMatch = @($draftSearch.data.plans | Where-Object { $_.planId -eq $planId })[0]
    Assert-Equal $draftSearchMatch $null 'Draft plan must not be returned by public search'

    $deleted = Invoke-JsonRequest -Method Delete -Uri "$baseUrl/api/plans/$planId`?versionNo=5" -Context $owner -Body $null
    Assert-Equal $deleted.data.planStatus 'DELETED' 'Plan deletion failed'
    Assert-Equal $deleted.data.versionNo 6 'Plan deletion version mismatch'

    $deletedPlans = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/mine" -WebSession $owner.Session
    $deletedPlan = @($deletedPlans.data.plans | Where-Object { $_.planId -eq $planId })[0]
    Assert-NotNull $deletedPlan 'Deleted plan was not returned by my plans'
    Assert-Equal $deletedPlan.planStatus 'DELETED' 'Deleted plan status mismatch in my plans'

    Invoke-ExpectJsonError -ExpectedStatus 404 -ExpectedCode 'PLAN_NOT_FOUND' `
        -Message 'Deleted plan editor access must be rejected' -Action {
            Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId/editor" -WebSession $owner.Session
        } | Out-Null

    $restored = Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/plans/$planId/restore" -Context $owner -Body @{
        versionNo = 6
    }
    Assert-Equal $restored.data.planStatus 'ACTIVE' 'Plan restore failed'
    Assert-Equal $restored.data.versionNo 7 'Plan restore version mismatch'

    $restoredEditor = Invoke-RestMethod -Method Get -Uri "$baseUrl/api/plans/$planId/editor" -WebSession $owner.Session
    Assert-Equal $restoredEditor.data.plan.planId $planId 'Restored plan editor access failed'
    Assert-Equal $restoredEditor.data.plan.publishStatus 'DRAFT' 'Restored plan publication status mismatch'

    Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/logout" -Context $invitee -Body $null | Out-Null
    Invoke-JsonRequest -Method Post -Uri "$baseUrl/api/auth/logout" -Context $owner -Body $null | Out-Null

    [ordered]@{
        result = 'PASS'
        ownerMemberId = $ownerLogin.data.member.memberId
        inviteeMemberId = $inviteeLogin.data.member.memberId
        planId = $planId
        scheduleItemId = $item.data.scheduleItemId
        invitationStatus = $accepted.data.status
        scheduleVersion = $item.data.resultScheduleVersion
        publishedAutoSaveTitle = $updatedPublicDetail.data.plan.title
        finalPlanStatus = $restored.data.planStatus
        finalPublishStatus = $restoredEditor.data.plan.publishStatus
        finalVersionNo = $restored.data.versionNo
    } | ConvertTo-Json
}
finally {
    if ($null -ne $serverProcess -and -not $serverProcess.HasExited) {
        Stop-Process -Id $serverProcess.Id -Force
        $serverProcess.WaitForExit()
    }
}
