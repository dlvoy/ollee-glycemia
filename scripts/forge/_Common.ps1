# Shared helpers for the scripts/forge tools. Dot-source it: . "$PSScriptRoot\_Common.ps1"
# Works in Windows PowerShell 5.1 and PowerShell 7.

Set-StrictMode -Version Latest

$script:RepoRoot  = (Resolve-Path (Join-Path $PSScriptRoot '..\..')).Path
$script:StateDir  = Join-Path $script:RepoRoot '.forge-test'   # web server pid and logs
$script:EmulatorRel = 'tools/forge_emulator'                   # git submodule, pinned
$script:EmulatorDir = Join-Path $script:RepoRoot 'tools\forge_emulator'
$script:PackageId = 'pl.cukrzycowy.ollee.glycemia'
$script:Receiver  = "$($script:PackageId)/.ForgeDebugReceiver"
$script:ReceiverAction = "$($script:PackageId).debug.FORGE"

function Write-Step([string]$Message) { Write-Host "==> $Message" -ForegroundColor Cyan }
function Write-Ok([string]$Message)   { Write-Host "    $Message" -ForegroundColor Green }
function Write-Warn([string]$Message) { Write-Host "    $Message" -ForegroundColor Yellow }

# State of the forge_emulator submodule from `git submodule status`, or $null without git.
# State: ' ' at the pinned commit, '+' checked out at another commit, '-' not initialized.
function Get-ForgeEmulatorStatus {
    $git = Get-Command git -ErrorAction SilentlyContinue
    if (-not $git) { return $null }
    $line = & $git.Source -C $script:RepoRoot submodule status -- $script:EmulatorRel 2>$null | Select-Object -First 1
    if (-not $line -or $line -notmatch '^([ +\-U])([0-9a-f]{40}) ') { return $null }
    return [pscustomobject]@{ State = $Matches[1]; Commit = $Matches[2].Substring(0, 7) }
}

# Checks out the forge_emulator submodule if it is missing (fresh clone without --recurse-submodules)
function Initialize-ForgeEmulator {
    $index = Join-Path $script:EmulatorDir 'index.html'
    if (-not (Test-Path $index)) {
        $git = Get-Command git -ErrorAction SilentlyContinue
        if (-not $git) { throw 'git not found (needed to check out the forge_emulator submodule).' }
        Write-Ok "Checking out the $($script:EmulatorRel) submodule"
        # Out-Host: git's progress lines must not become part of this function's return value
        & $git.Source -C $script:RepoRoot submodule update --init -- $script:EmulatorRel 2>&1 | Out-Host
        if ($LASTEXITCODE -ne 0 -or -not (Test-Path $index)) {
            throw "Could not check out $($script:EmulatorRel). Run: git submodule update --init $($script:EmulatorRel)"
        }
    }
    return Get-ForgeEmulatorStatus
}

function Get-SdkRoot {
    $candidates = @($env:ANDROID_HOME, $env:ANDROID_SDK_ROOT)
    $props = Join-Path $script:RepoRoot 'local.properties'
    if (Test-Path $props) {
        $line = Select-String -Path $props -Pattern '^\s*sdk\.dir\s*=\s*(.+)$' | Select-Object -First 1
        if ($line) { $candidates += ($line.Matches[0].Groups[1].Value.Trim() -replace '\\:', ':' -replace '\\\\', '\') }
    }
    $candidates += (Join-Path $env:LOCALAPPDATA 'Android\Sdk')
    foreach ($c in $candidates) { if ($c -and (Test-Path $c)) { return $c } }
    return $null
}

function Get-AdbExe {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    $sdk = Get-SdkRoot
    if ($sdk) {
        $p = Join-Path $sdk 'platform-tools\adb.exe'
        if (Test-Path $p) { return $p }
    }
    throw 'adb not found. Install the Android SDK platform-tools and set ANDROID_HOME (see docs/watchface/TESTING.md).'
}

function Get-EmulatorExe {
    $sdk = Get-SdkRoot
    if ($sdk) {
        $p = Join-Path $sdk 'emulator\emulator.exe'
        if (Test-Path $p) { return $p }
    }
    throw 'emulator.exe not found. Install the Android Emulator via the SDK manager and set ANDROID_HOME.'
}

# The Microsoft Store "python" stub exits non-zero, so test that it really runs
function Get-PythonExe {
    foreach ($name in @('python', 'py')) {
        $cmd = Get-Command $name -ErrorAction SilentlyContinue
        if (-not $cmd) { continue }
        $pyArgs = @('--version')
        if ($name -eq 'py') { $pyArgs = @('-3', '--version') }
        & $cmd.Source @pyArgs *> $null
        if ($LASTEXITCODE -eq 0) { return $cmd.Source }
    }
    throw 'Python 3 not found (needed to serve forge_emulator). Install it from python.org.'
}

# Gradle with AGP 9 needs JDK 17+. Use JAVA_HOME when it is new enough, else Android Studio's bundled JBR.
function Get-JavaMajor([string]$JavaHome) {
    $java = Join-Path $JavaHome 'bin\java.exe'
    if (-not (Test-Path $java)) { return 0 }
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $text = (& $java -version 2>&1 | ForEach-Object { $_.ToString() }) -join "`n"
    } finally { $ErrorActionPreference = $previous }
    if ($text -match 'version "(\d+)(?:\.(\d+))?') {
        if ($Matches[1] -eq '1') { return [int]$Matches[2] }
        return [int]$Matches[1]
    }
    return 0
}

function Get-JavaHome {
    $candidates = @($env:JAVA_HOME,
        (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr'),
        (Join-Path $env:LOCALAPPDATA 'Programs\Android Studio\jbr'))
    foreach ($c in $candidates) {
        if ($c -and (Get-JavaMajor $c) -ge 17) { return $c }
    }
    throw 'No JDK 17+ found. Set JAVA_HOME to one, or install Android Studio (its bundled JBR is used automatically).'
}

# Online devices from `adb devices`; honours $env:FORGE_SERIAL, prefers emulators
function Get-DeviceSerials {
    $adb = Get-AdbExe
    $lines = & $adb devices | Select-Object -Skip 1
    $serials = @()
    foreach ($l in $lines) {
        if ($l -match '^(\S+)\s+device\s*$') { $serials += $Matches[1] }
    }
    return $serials
}

function Get-Serial {
    if ($env:FORGE_SERIAL) { return $env:FORGE_SERIAL }
    $serials = @(Get-DeviceSerials)
    if ($serials.Count -eq 0) { return $null }
    $emu = @($serials | Where-Object { $_ -like 'emulator-*' })
    if ($emu.Count -gt 0) { return $emu[0] }
    return $serials[0]
}

function Invoke-Adb {
    $adb = Get-AdbExe
    $serial = Get-Serial
    if (-not $serial) { throw 'No Android device online. Run Start-ForgeTest.ps1 first (or start an emulator).' }
    & $adb -s $serial @args
}

function Wait-ForBoot([int]$TimeoutSec = 240) {
    $adb = Get-AdbExe
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        $serial = Get-Serial
        if ($serial) {
            $done = (& $adb -s $serial shell getprop sys.boot_completed 2>$null) -join ''
            if ($done.Trim() -eq '1') { return $serial }
        }
        Start-Sleep -Seconds 3
    }
    throw "The emulator did not finish booting within $TimeoutSec s."
}

# Sends a command to the debug receiver in the app and returns its result text
function Send-ForgeBroadcast([string]$Cmd, [hashtable]$Extras = @{}) {
    $amArgs = @('shell', 'am', 'broadcast', '-n', $script:Receiver, '-a', $script:ReceiverAction, '--es', 'cmd', $Cmd)
    foreach ($key in $Extras.Keys) { $amArgs += @('--es', $key, [string]$Extras[$key]) }
    $out = (Invoke-Adb @amArgs) -join "`n"
    if ($out -match 'data="([^"]*)"') { return $Matches[1] }
    if ($out -match 'Error|Exception|not found') { throw "Broadcast failed: $out" }
    return ''
}

function Test-ProcessAlive([int]$ProcessId) {
    return [bool](Get-Process -Id $ProcessId -ErrorAction SilentlyContinue)
}
