<#
.SYNOPSIS
  Tears down what Start-ForgeTest.ps1 started: web server, adb forward, relay (and optionally the emulator).
#>
[CmdletBinding()]
param(
    [int]$Port = 8765,
    [switch]$KillEmulator
)

$ErrorActionPreference = 'Continue'
. "$PSScriptRoot\_Common.ps1"

Write-Step 'Web server'
$pidFile = Join-Path $script:StateDir 'webserver.pid'
if (Test-Path $pidFile) {
    $procId = [int](Get-Content $pidFile | Select-Object -First 1)
    if ($procId -gt 0 -and (Test-ProcessAlive $procId)) {
        Stop-Process -Id $procId -Force
        Write-Ok "Stopped pid $procId"
    } else {
        Write-Ok 'Already stopped'
    }
    Remove-Item $pidFile -Force
} else {
    Write-Ok 'Not running'
}

$serial = Get-Serial
if ($serial) {
    Write-Step 'App relay'
    try { Write-Ok (Send-ForgeBroadcast 'teardown') } catch { Write-Warn "Could not reach the app: $($_.Exception.Message)" }
    Write-Step "adb forward tcp:$Port"
    Invoke-Adb forward --remove "tcp:$Port" 2>&1 | Out-Null
    Write-Ok 'Removed'
    if ($KillEmulator) {
        Write-Step 'Emulator'
        Invoke-Adb emu kill 2>&1 | Out-Null
        Write-Ok 'Shutdown requested'
    }
} else {
    Write-Warn 'No device online; nothing to clean up on the Android side.'
}
Write-Host 'Done.' -ForegroundColor Green
