<#
.SYNOPSIS
  Brings up the local Forge watchface test loop: Android emulator + debug app + relay + forge_emulator.

.DESCRIPTION
  1. Starts the Android emulator if no device is online and waits for boot
  2. Builds and installs the debug app (gradlew installDebug)
  3. Grants permissions, accepts the disclaimer, selects the synthetic provider and turns the relay on
  4. Forwards the relay port with adb
  5. Clones/updates forge_emulator into .forge-test\ and serves it on 127.0.0.1
  6. Copies tools\forge-bridge\forge-bridge.js to the clipboard and opens the emulator page

  Afterwards three manual steps remain (printed at the end): load watchface\glycemia.js,
  press Run, paste the bridge snippet in the DevTools console.
  See docs\watchface\TESTING.md.

.PARAMETER Avd        Name of the Android virtual device (default: the first one from `emulator -list-avds`).
.PARAMETER Provider   Synthetic provider to select: virtual_human (default) or constant.
.PARAMETER Port       Relay port on the device and on this PC (default 8765).
.PARAMETER WebPort    Port that serves forge_emulator on this PC (default 18080).
.PARAMETER SkipBuild  Don't run gradlew installDebug (reuse the installed app).
.PARAMETER NoBrowser  Don't open the browser.
#>
[CmdletBinding()]
param(
    [string]$Avd,
    [ValidateSet('virtual_human', 'constant')][string]$Provider = 'virtual_human',
    [int]$Port = 8765,
    [int]$WebPort = 18080,
    [switch]$SkipBuild,
    [switch]$NoBrowser
)

$ErrorActionPreference = 'Stop'
. "$PSScriptRoot\_Common.ps1"

$adb = Get-AdbExe
$python = Get-PythonExe
$git = (Get-Command git -ErrorAction SilentlyContinue)
if (-not $git) { throw 'git not found (needed to fetch forge_emulator).' }
New-Item -ItemType Directory -Force -Path $script:StateDir | Out-Null

# 1. Device ------------------------------------------------------------------
Write-Step 'Android device'
$serial = Get-Serial
if ($serial) {
    Write-Ok "Using $serial"
} else {
    $emulator = Get-EmulatorExe
    if (-not $Avd) {
        $avds = @(& $emulator -list-avds | Where-Object { $_ -and $_ -notmatch '^(INFO|WARNING)' })
        if ($avds.Count -eq 0) { throw 'No AVD found. Create one in Android Studio (Device Manager) or pass -Avd.' }
        $Avd = $avds[0]
    }
    Write-Ok "Starting emulator '$Avd' (first boot can take a few minutes)"
    Start-Process -FilePath $emulator -ArgumentList @('-avd', $Avd) | Out-Null
    & $adb wait-for-device
    $serial = Wait-ForBoot
    Write-Ok "Booted: $serial"
}
$env:ANDROID_SERIAL = $serial
$env:FORGE_SERIAL = $serial

# 2. Build and install ---------------------------------------------------------
if ($SkipBuild) {
    Write-Step 'Skipping build (-SkipBuild)'
} else {
    Write-Step 'Building and installing the debug app'
    $env:JAVA_HOME = Get-JavaHome
    Push-Location $script:RepoRoot
    try {
        & "$($script:RepoRoot)\gradlew.bat" --console=plain installDebug
        if ($LASTEXITCODE -ne 0) { throw "gradlew installDebug failed (exit $LASTEXITCODE)" }
    } finally { Pop-Location }
}

# 3. App setup ----------------------------------------------------------------
Write-Step 'Preparing the app'
Invoke-Adb shell am force-stop $script:PackageId | Out-Null
foreach ($perm in @('android.permission.BLUETOOTH_CONNECT', 'android.permission.BLUETOOTH_SCAN', 'android.permission.POST_NOTIFICATIONS')) {
    Invoke-Adb shell pm grant $script:PackageId $perm 2>&1 | Out-Null  # older Android versions don't know some of them
}
$setup = ''
for ($i = 0; $i -lt 5 -and -not $setup.StartsWith('ok'); $i++) {
    $setup = Send-ForgeBroadcast 'setup' @{ provider = $Provider }
    if (-not $setup.StartsWith('ok')) { Start-Sleep -Seconds 2 }
}
if (-not $setup.StartsWith('ok')) { throw "App setup failed: '$setup'. Is this a debug build? (the receiver exists in debug builds only)" }
Write-Ok $setup
Invoke-Adb shell am start -n "$($script:PackageId)/.MainActivity" | Out-Null

# Wait for the first synthetic reading to reach the relay
$status = ''
$deadline = (Get-Date).AddSeconds(45)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Seconds 3
    $status = Send-ForgeBroadcast 'status'
    if ($status -match 'seq=([1-9]\d*)') { break }
}
if ($status -match 'seq=([1-9]\d*)') { Write-Ok "Relay has frames: $status" }
else { Write-Warn "No frame yet ($status). Check the app on the emulator; the logcat tag is ForgeDebug." }

# 4. Port forward -------------------------------------------------------------
Write-Step "Forwarding tcp:$Port"
Invoke-Adb forward "tcp:$Port" "tcp:$Port" | Out-Null
try {
    $probe = Invoke-RestMethod -Uri "http://127.0.0.1:$Port/frames?after=0" -TimeoutSec 5
    Write-Ok "Relay answers (seq=$($probe.seq), link=$($probe.link))"
} catch {
    Write-Warn "Relay did not answer on 127.0.0.1:$Port yet: $($_.Exception.Message)"
}

# 5. forge_emulator -----------------------------------------------------------
Write-Step 'forge_emulator'
$emuDir = Join-Path $script:StateDir 'forge_emulator'
if (Test-Path (Join-Path $emuDir '.git')) {
    & $git.Source -C $emuDir pull --ff-only --quiet 2>&1 | Out-Null
    if ($LASTEXITCODE -ne 0) { Write-Warn 'Could not update forge_emulator (offline?). Using the existing copy.' }
} else {
    & $git.Source clone --depth 1 https://github.com/olleewatch/forge_emulator $emuDir
    if ($LASTEXITCODE -ne 0) { throw 'git clone of forge_emulator failed.' }
}
$emuCommit = (& $git.Source -C $emuDir rev-parse --short HEAD)
Write-Ok "forge_emulator at $emuCommit"

$pidFile = Join-Path $script:StateDir 'webserver.pid'
$running = $false
if (Test-Path $pidFile) {
    $old = [int](Get-Content $pidFile -ErrorAction SilentlyContinue | Select-Object -First 1)
    $running = $old -gt 0 -and (Test-ProcessAlive $old)
}
if ($running) {
    Write-Ok "Web server already running (pid $old)"
} else {
    $busy = Get-NetTCPConnection -LocalPort $WebPort -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($busy) {
        $owner = (Get-Process -Id $busy.OwningProcess -ErrorAction SilentlyContinue).ProcessName
        throw "Port $WebPort is already used by '$owner' (pid $($busy.OwningProcess)). Pass -WebPort <free port>."
    }
    # -I: isolated mode, so nothing from the (untrusted) checkout is imported
    $server = Start-Process -FilePath $python -ArgumentList @('-I', '-m', 'http.server', "$WebPort", '--bind', '127.0.0.1') `
        -WorkingDirectory $emuDir -WindowStyle Hidden -PassThru `
        -RedirectStandardError (Join-Path $script:StateDir 'webserver.err.log') `
        -RedirectStandardOutput (Join-Path $script:StateDir 'webserver.out.log')
    Set-Content -Path $pidFile -Value $server.Id
    Start-Sleep -Seconds 2
    if (-not (Test-ProcessAlive $server.Id)) {
        throw "The web server exited at once. See $($script:StateDir)\webserver.err.log"
    }
    Write-Ok "Serving forge_emulator on http://127.0.0.1:$WebPort/ (pid $($server.Id))"
}
try {
    Invoke-WebRequest -Uri "http://127.0.0.1:$WebPort/index.html" -UseBasicParsing -TimeoutSec 5 | Out-Null
} catch {
    throw "forge_emulator is not reachable on http://127.0.0.1:$WebPort/ ($($_.Exception.Message)). Is the port taken? Try -WebPort."
}

# 6. Bridge snippet + browser -----------------------------------------------
$bridge = Get-Content (Join-Path $script:RepoRoot 'tools\forge-bridge\forge-bridge.js') -Raw
if ($Port -ne 8765) { $bridge = "window.FORGE_RELAY = 'http://127.0.0.1:$Port';`n" + $bridge }
Set-Clipboard -Value $bridge
if (-not $NoBrowser) { Start-Process "http://127.0.0.1:$WebPort/" }

$face = Join-Path $script:RepoRoot 'watchface\glycemia.js'
Write-Host ''
Write-Host 'Ready. Three manual steps in the forge_emulator page:' -ForegroundColor Green
Write-Host "  1. Load file -> $face   then press Run"
Write-Host '  2. Open DevTools (F12) -> Console. If Chrome blocks pasting, type "allow pasting" and Enter first.'
Write-Host '  3. Paste (Ctrl+V, the bridge snippet is on your clipboard) and press Enter'
Write-Host ''
Write-Host 'Scenarios:  .\scripts\forge\Set-ForgeScenario.ps1 -Link off | -Silence on | -Pause on | -Status'
Write-Host 'Tear down:  .\scripts\forge\Stop-ForgeTest.ps1 [-KillEmulator]'
