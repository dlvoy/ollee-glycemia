# 15: Forge glycemia watchface

## Problem
The app pushes one thing to the watch: a 6-character "screensaver/tag" string such as `120 -5`, sent over Nordic UART (`WatchConnection.kt`). The firmware owns that text, so:
- The watch can't show how old the reading is.
- When the phone link drops, the last value stays on screen and looks current (see the disclaimer in `README.md`).
- Users have to open the phone to learn whether the data is fresh or the app is still connected.

Ollee's Forge beta (https://github.com/olleewatch/forge_emulator) adds custom JavaScript watchfaces with a BLE data channel, connection events and an RTC clock. The firmware doesn't support it yet (Beta 1 is emulator only, watch support comes in Beta 2), but the spec and an emulator are public. The app doesn't use any of it.

## Goal
A Forge watchface that shows exactly the text the app sends today as the screensaver tag, plus a measurement age that keeps counting on the watch. It says so when the reading is outdated or the app link is lost. The app feeds it over the Forge BLE service and behaves as before on firmware without Forge.

## Scope
- An ASCII frame protocol v1 on the Forge RX characteristic, at most 20 bytes per write, so any frame can be typed into forge_emulator by hand.
- App side:
  - Forge service detection after GATT discovery
  - a serialized GATT write queue (each update becomes two writes)
  - a frame feed with a keepalive
  - the measurement time passed through to the watch layer
- The watchface `watchface/glycemia.js`:
  - Main shows the tag 1:1 (`Ollee.drawMain`)
  - Day shows the age in minutes, the minutes since the link was lost, or `--`
  - LAP and PM indicators, plus a short status-word flash on Main
- A "watchface" marker in the watch list when the watch exposes the Forge service. EN, PL and FR strings.
- Local testing:
  - a Node test harness for the face
  - a debug-only relay from the app to forge_emulator
  - a debug-only broadcast receiver, so scenarios (link lost, stale data, paused watch) can be triggered from `adb` instead of tapping through the UI
  - Windows PowerShell scripts in `scripts/forge/` that bring the loop up and down: Android emulator, app, relay, forge_emulator and the face
  - a step-by-step manual, `docs/watchface/TESTING.md`: it answers whether the Android emulator and forge_emulator can work together, and walks through every watchface mode with the script command that triggers it and what the display should show
- Docs: `docs/watchface/PROTOCOL.md`, `watchface/README.md`, a README section and a CHANGELOG entry.

## Out of scope
- Uploading or installing the face from the app. Forge deployment is not documented until Beta 2.
- Watch-to-phone messages (TX notifications, refresh requests).
- High/low alarms, beeps or LED alerts.
- Changing the tag format (`formatBg`), mmol/L formatting or the Nightscout trend mapping.
- Running the new tests in CI. CI only builds today, so wiring Gradle and Node tests into it is its own spec.

## Acceptance criteria
- [ ] On firmware without Forge the app behaves exactly as before. The screensaver packet bytes don't change (golden test).
- [ ] When the Forge service is present, each tag change sends one frame, each (re)connect sends one, and a keepalive goes out at least every 5 minutes.
- [ ] The face's Main shows exactly the 6 characters of the tag.
- [ ] Day shows the age in whole minutes and advances on each minute tick with no new BLE data. Ages of 40 minutes and up, or an unknown age, show `--`.
- [ ] On a BLE disconnect, Day counts minutes since the disconnect, LAP goes off and Main flashes `no bLE`.
- [ ] When BLE is up but no frame arrives for 11 minutes, Main flashes `no APP` and Day counts minutes since the last frame.
- [ ] A reading older than 10.5 minutes turns PM on and flashes ` old  `.
- [ ] Pause/stop labels show on Main with Day `--` and no lost-link flash.
- [ ] No malformed frame makes the face throw, and the script is at most 8158 bytes.
- [ ] `node --test watchface/test/` and `gradlew testDebugUnitTest` pass.
- [ ] On Windows, `scripts/forge/Start-ForgeTest.ps1` brings up the full loop (Android emulator with a synthetic provider, app, relay, forge_emulator) and `Stop-ForgeTest.ps1` tears it down, with no manual steps beyond loading the face and pasting the bridge snippet.
- [ ] `TESTING.md` has a walkthrough that reproduces every watchface mode (LIVE, OUTDATED, NO_DATA, NO_APP, NO_LINK, PAUSED) in forge_emulator, each with the script command that triggers it and the expected display.

## Open questions
- The full RX/TX UUIDs (`C6FB0002-…`, `C6FB0003-…`) follow the service UUID pattern, but the forge_emulator repo never spells them out. Confirm them against Beta 2.
- Can a Forge face receive data without the user starting BLE from the home face? forge_emulator issue #3 reports it can't, and there is no API to turn BLE on from a face.
- On a bonded device Android may cache the old GATT table after a firmware update and hide the Forge service. A Bluetooth toggle or a re-pair may be needed. Check on Beta 2.
- Do `-`, `1`, `2` and `3` render on the real first Day digit? Do the status words read correctly on positions 4 and 6, which remap some letters?
