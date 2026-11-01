<#
.SYNOPSIS
  Runs every automated check for the Forge watchface work and prints a one-line summary for each:
  the PowerShell parse check, the Node watchface tests and the Gradle unit tests.
#>
[CmdletBinding()]
param([switch]$SkipGradle)

$ErrorActionPreference = 'Continue'
. "$PSScriptRoot\_Common.ps1"

$failed = @()

# PowerShell syntax of the scripts in this folder
Write-Step 'PowerShell scripts: parse check'
$parseErrors = 0
foreach ($file in Get-ChildItem $PSScriptRoot -Filter *.ps1) {
    $errors = $null
    [System.Management.Automation.Language.Parser]::ParseFile($file.FullName, [ref]$null, [ref]$errors) | Out-Null
    foreach ($e in $errors) { Write-Warn "$($file.Name):$($e.Extent.StartLineNumber): $($e.Message)"; $parseErrors++ }
}
if ($parseErrors -eq 0) { Write-Ok 'ok' } else { $failed += 'powershell' }

# Watchface (Node)
Write-Step 'Watchface: node --test'
$node = Get-Command node -ErrorAction SilentlyContinue
if (-not $node) {
    Write-Warn 'node not found, skipped'
} else {
    Push-Location $script:RepoRoot
    try {
        $out = & $node.Source --test --test-reporter=tap 'watchface/test/*.test.js' 2>&1 | ForEach-Object { $_.ToString() }
        $code = $LASTEXITCODE
    } finally { Pop-Location }
    $summary = $out | Where-Object { $_ -match '^# (tests|pass|fail)' }
    Write-Host "    $($summary -join '  ')"
    if ($code -ne 0) { $failed += 'node'; $out | Select-Object -Last 40 | ForEach-Object { Write-Host "    $_" } }
}

# App (Gradle)
if ($SkipGradle) {
    Write-Step 'Gradle: skipped'
} else {
    Write-Step 'App: gradlew testDebugUnitTest'
    $env:JAVA_HOME = Get-JavaHome
    Push-Location $script:RepoRoot
    try {
        & "$($script:RepoRoot)\gradlew.bat" --console=plain testDebugUnitTest
        $code = $LASTEXITCODE
    } finally { Pop-Location }
    $tests = 0; $bad = 0
    foreach ($f in Get-ChildItem (Join-Path $script:RepoRoot 'app\build\test-results\testDebugUnitTest') -Filter *.xml -ErrorAction SilentlyContinue) {
        $suite = ([xml](Get-Content $f.FullName)).testsuite
        $tests += [int]$suite.tests; $bad += [int]$suite.failures + [int]$suite.errors
    }
    Write-Host "    tests=$tests failed=$bad"
    if ($code -ne 0 -or $bad -gt 0) { $failed += 'gradle' }
}

Write-Host ''
if ($failed.Count -eq 0) { Write-Host 'All checks passed.' -ForegroundColor Green; exit 0 }
Write-Host "Failed: $($failed -join ', ')" -ForegroundColor Red
exit 1
