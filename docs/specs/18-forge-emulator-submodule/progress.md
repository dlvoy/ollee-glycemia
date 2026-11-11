# 18: forge_emulator as a git submodule (progress)

**Status:** done
**Release:** v2.0.0

## Checklist
- [x] Submodule at the pin, old clone removed
- [x] Script changes
- [x] Harness exports and compat test
- [x] Docs, CHANGELOG entry
- [x] Test run: `Invoke-ForgeTests.ps1` all green (parse, submodule at `0857ea7`, node 34, gradle 191)
- [x] Full `Start-ForgeTest.ps1 -SkipBuild -NoBrowser` run starting from a de-initialized submodule: it checked out `0857ea7`, served the page (HTTP 200), relay up; `Stop-ForgeTest.ps1` tore it down
- [x] The "not at the pinned commit" state (`+`) is detected, and `git submodule update` goes back to the pin

## Log
- 2026-11-11: intent and plan written, approved by the user together with spec 17
- 2026-11-11: submodule added and staged (`.gitmodules`, gitlink at `0857ea7`, tag `Fall_Q4_2026_Beta`); `.forge-test/forge_emulator` deleted (its only local files were copies of `glycemia.js` and `forge-bridge.js`, identical to the repo's). Not committed yet
- 2026-11-11: fresh-clone path simulated with `git submodule deinit -f` (submodule work tree clean, staged pin kept). The first run failed: git's progress output from `submodule update` leaked into `Initialize-ForgeEmulator`'s return value. Fixed with `| Out-Host` in `_Common.ps1`; the rerun passed. Closed
