<#
.SYNOPSIS
  Triggers test scenarios in the debug app (via its debug-only broadcast receiver).

.EXAMPLE
  .\Set-ForgeScenario.ps1 -Link off          # simulate losing the BLE link  (face: "no bLE", LAP off)
  .\Set-ForgeScenario.ps1 -Link on           # reconnect                    (face: live again)
  .\Set-ForgeScenario.ps1 -Silence on        # stop forwarding readings     (age grows -> " old  ", then "no APP")
  .\Set-ForgeScenario.ps1 -Mute on           # app silent, link stays up    (after 11 min: "no APP", LAP off)
  .\Set-ForgeScenario.ps1 -Pause on          # the user paused the watch    (face: label, Day "--")
  .\Set-ForgeScenario.ps1 -Provider constant -Value 180
  .\Set-ForgeScenario.ps1 -Status

  Several switches can be combined; they run in the order Link, Silence, Mute, Pause, Provider, Status.
#>
[CmdletBinding()]
param(
    [ValidateSet('on', 'off')][string]$Link,
    [ValidateSet('on', 'off')][string]$Silence,
    [ValidateSet('on', 'off')][string]$Mute,
    [ValidateSet('on', 'off')][string]$Pause,
    [ValidateSet('virtual_human', 'constant', 'xdrip', 'nightscout')][string]$Provider,
    [ValidateRange(40, 400)][int]$Value,
    [switch]$Status
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_Common.ps1"

if (-not ($Link -or $Silence -or $Mute -or $Pause -or $Provider -or $Status)) {
    Get-Help $MyInvocation.MyCommand.Path -Examples
    return
}

function Invoke-Step([string]$Label, [string]$Cmd, [hashtable]$Extras = @{}) {
    $result = Send-ForgeBroadcast $Cmd $Extras
    if ($result.StartsWith('error')) { Write-Warn "${Label}: $result" } else { Write-Ok "${Label}: $result" }
}

if ($Link)    { Invoke-Step 'link'    'link'    @{ on = $Link } }
if ($Silence) { Invoke-Step 'silence' 'silence' @{ on = $Silence } }
if ($Mute)    { Invoke-Step 'mute'    'mute'    @{ on = $Mute } }
if ($Pause)   { Invoke-Step 'pause'   'pause'   @{ on = $Pause } }
if ($Provider) {
    $extras = @{ id = $Provider }
    if ($PSBoundParameters.ContainsKey('Value')) { $extras['value'] = $Value }
    Invoke-Step 'provider' 'provider' $extras
}
if ($Status)  { Invoke-Step 'status'  'status' }
