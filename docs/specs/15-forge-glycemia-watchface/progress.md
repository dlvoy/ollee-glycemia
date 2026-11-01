# 15: Forge glycemia watchface (progress)

**Status:** in-progress
**Release:** v1.7.0

## Checklist
- [x] Protocol and encoders (`OapProtocol`, `ForgeProtocol`, shared fixtures)
- [x] Watchface and Node tests (24 tests)
- [x] GATT queue, Forge detection, feed (`GattWriteQueue`, `ForgeFeed`, `WatchPayload`, `WatchConnection`)
- [x] Service wiring and keepalive
- [x] Watch list marker and strings (EN, PL, FR)
- [x] Debug relay, debug control receiver and forge-bridge snippet
- [x] PowerShell scripts (`scripts/forge/`) and `.gitignore` entry
- [x] Docs (PROTOCOL, TESTING manual, watchface README, README section, CHANGELOG)
- [ ] End-to-end run: done for LIVE, NO_LINK, recovery, PAUSED. Still open: OUTDATED/NO_APP waited out in real time, a release-build check, the regression check on a real watch with current firmware

## Log
- 2026-11-01: intent and plan drafted, waiting for review
- 2026-11-01: plan extended with the Windows scripts, the debug control receiver and the TESTING.md manual (scenario walkthrough)
- 2026-11-01: implementation started after approval. Steps 1 to 7 coded, all unit tests green (Gradle and `node --test`)
- 2026-11-01: plan deviations, see below
- 2026-11-01: end-to-end run on Windows with the Pixel_10 AVD: `Start-ForgeTest.ps1` built, installed and set up the app, the relay served frames, forge_emulator showed the tag, Day and LAP; `-Link off` gave `no bLE` with LAP off, `-Link on` recovered, `-Pause on` showed ` pause` and Day `--`. 167 unit tests green
- 2026-11-01: acceptance criteria checked against code and tests: 10 of 12 ticked (both suites green). Open: Start/Stop script run, real-time OUTDATED/NO_APP walkthrough
- 2026-11-01: released as v1.7.0

### Deviations from the plan
- The "watchface" marker reads `PairedWatch.forgeSupported` (persisted by `WatchStore`) instead of a new `WatchStatus` field, so the marker also shows while the watch is offline.
- `node --test watchface/test/` does not work on Node 24 (a directory is treated as a module path). The documented command is `node --test "watchface/test/*.test.js"`.
- Two receiver commands were added beyond the plan: `teardown` (used by `Stop-ForgeTest.ps1`) and `mute` (app silent with the link up, to reproduce NO_APP, which `silence` can't because keepalives continue).
- GATT callbacks now hop to the main thread before touching connection state (not only the public entry points).
- A GATT write the stack rejects now counts as failed. Before, `writeCharacteristic`'s return value was ignored and the value was marked as sent.
- `LINK_TIMEOUT` comparison is `>=` so exactly 11 minutes of silence flips to NO_APP.
- `Start-ForgeTest.ps1` serves forge_emulator on port 18080 by default (8080 was taken on the dev machine) and checks that the port is free and the server stays alive.
