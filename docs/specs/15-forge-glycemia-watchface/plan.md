# 15: Forge glycemia watchface (plan)

## Approach

### Decisions taken with the user (2026-11-01)
- **Status display**: indicators plus a flash. LAP = live app link, PM = outdated. In an error state Main shows a status word for about 2 s on each minute tick, on face enter and on each mode change, then returns to the tag.
- **Day field**: minutes 0 to 39, `--` from 40 minutes on. The first Day digit on the real LCD only renders `1` to `3`, so 40 and up would come out wrong.
- **Test tooling**: full end-to-end relay. A Node harness for the face, a debug-only HTTP relay in the app, and a DevTools snippet that feeds forge_emulator.

### What the Forge spec gives us
Source: https://github.com/olleewatch/forge_emulator (Beta 1, read 2026-11-01).
- A face is one plain `.js` file (at most 8158 bytes) on Espruino. No manifest, no build step.
- Hooks are found by their exact top-level function names: `init`, `enter`, `exit`, `ble_rx`, `ble_connect`, `ble_disconnect`, `clock_minute` and others. Event hooks only fire if their bit is in the top-level `subscribe`. `E_BLE_RX`, `E_BLE_CONNECT`, `E_BLE_DISCONNECT` and `E_CLOCK_MINUTE` can also be delivered off screen via `Ollee.subscribe(...)`.
- BLE service `C6FB0001-A37D-40AC-89BE-887D25E1F5B3`, RX (phone writes) `C6FB0002-...`, TX (watch notifies) `C6FB0003-...`. Each write is at most 20 bytes and arrives as one `ble_rx(data)` call, a string with one character per byte. There is no framing, queue or acknowledgement.
- Display: `Ollee.drawMain(text)` writes 6 characters on the clock digits, `Ollee.drawDay(text)` writes 2 on the day digits. Both keep the first N characters and right-align. `Ollee.drawLap(on)` and `Ollee.drawPM(on)` control the indicators. Draw calls do nothing while the face is off screen.
- Time: `new Date().getTime()` reads the RTC, which keeps running while the watch sleeps. `clock_minute()` fires every minute from the low-power scheduler. There is no uptime API. Compute from timestamps, don't count ticks.
- An uncaught error in any hook disables the face until it is reloaded. Espruino doesn't hoist function declarations. `Map`, `Set`, `Promise` and the global `setTimeout` don't exist. `Ollee.setTimeout` has 4 shared slots and returns `-1` when none is free.

### 1. Frame protocol v1
Forge RX characteristic, one frame per write, printable ASCII, at most 20 bytes.

| Frame | Meaning | Example |
|---|---|---|
| `G<age>,<text>` | Live state. `<age>` is the whole seconds from the reading's timestamp to the moment of the write, as decimal 0 to 86400 (clamped), or `-` when no reading is known. `<text>` is the 6-char tag, identical to the OAP payload. | `G83,120 -5` (10 bytes, 13 at most) |
| `P,<text>` | The watch is paused or stopped by the user. Show the label, set Day `--`, and don't treat the disconnect that follows as a lost link. | `P, pause` |

- The face ignores unknown type letters and malformed frames, so the format stays forward compatible.
- The text is the last field, so it needs no escaping.
- Sending an age instead of an absolute time avoids any clock sync between phone and watch. The face stores `measuredAt = now - age*1000` in its own RTC time.
- ASCII means any frame can be typed into forge_emulator's Bluetooth text box, which sends UTF-8 text only.

### 2. App transport
The Forge service lives on the same GATT server as Nordic UART, so one connection carries both.

- After discovery, `WatchConnection` looks up the Forge service and RX characteristic. If missing, Forge stays off for that connection.
- New `GattWriteQueue`:
  - runs one write at a time on the main looper
  - moves on in a new `onCharacteristicWrite` override, or after a 2 s timeout
  - clears itself on disconnect
  - picks the write type from the characteristic properties (WRITE, else WRITE_NO_RESPONSE)
  - is needed because each update now makes two writes (OAP, then Forge) and Android rejects a write while another is in flight
- `WatchConnection`'s public entry points post to its main-thread `Handler`. This also fixes an existing race: `NightscoutProvider` calls `submitReading` from a worker thread.
- New `WatchPayload(text: String, measuredAtMs: Long?, paused: Boolean)` replaces the bare `String` in `submitReading`. The OAP path keeps its `lastSent` text dedup unchanged. Forge sending is decided by `ForgeFeed`.
- New `ForgeFeed`, pure policy shared by `WatchConnection` and the debug relay:
  - emits a frame when the payload changes, after `reset()` (a reconnect), or when 5 minutes have passed since the last frame
  - builds the bytes when the queue dequeues the write, so the age is computed at write time
- Paused or stopped watches: OAP label, then the `P` frame, then `disconnectSoft()` once the queue drains. Today the code disconnects right after issuing the write.
- A successful Forge write also updates `lastSuccessfulSyncTimeMs`, so keepalives stop a steady value (Constant provider) from tripping the 30-minute offline timeout.

### 3. BleService wiring
- `measuredAtMs` comes from the pref `last_time` (0 means null). It is set only for newer readings and reset on a provider switch, which is the semantics we want.
- Build a `WatchPayload` at every place that submits today: `handleBg`, the Err timeout, clear, resend, `manualSyncWatch`, `syncConnectionsWithStore` and `setWatchActivity`.
- The existing 60 s timeout watcher also calls `connection.tick(now)` and `relay.tick(now)` for keepalives. No new timer.

### 4. Watchface `watchface/glycemia.js`
Espruino-safe ES5:
- hooks are top-level `function` declarations
- no top-level calls, because Espruino doesn't hoist
- every hook body in `try/catch`, because an uncaught error disables the face

Subscriptions:
- `var subscribe = E_BLE_RX + E_BLE_CONNECT + E_BLE_DISCONNECT + E_CLOCK_MINUTE;`
- In `init()`, `Ollee.subscribe(E_BLE_RX + E_BLE_CONNECT + E_BLE_DISCONNECT)`, so state stays current while another face is on screen.

State: `text` (starts as `--- --`), `measuredAt`, `lastRxAt`, `lostAt`, `paused`, `flashId`.

Constants:
- `OUTDATED_MS = 630000`: 10.5 min, the app's STALE boundary in `MainScreen.kt`
- `LINK_TIMEOUT_MS = 660000`: two missed keepalives plus 1 min
- `FLASH_MS = 2000`
- `DAY_MAX = 39`

| Mode | Condition | Main | Day | LAP | PM | Flash |
|---|---|---|---|---|---|---|
| PAUSED | last frame was `P` | label | `--` | off | off | none |
| NO_LINK | `!Ollee.connected()` | last text | min since disconnect | off | off | `no bLE` |
| NO_APP | connected, no frame yet or none for 11 min | last text | min since last frame, or `--` | off | off | `no APP` |
| OUTDATED | live, age at least 10.5 min | text | age | on | on | ` old  ` |
| NO_DATA | live, age `-` | text | `--` | on | off | none |
| LIVE | live, age under 10.5 min | text | age | on | off | none |

Day rendering:
- minutes `m = floor(ms/60000)`, clamped at 0 (covers the watch clock being set back)
- `m <= 39` gives `String(m)`, which `drawDay` right-aligns, so 7 shows as ` 7`
- anything else gives `--`

Redraw on `enter()`, `ble_rx`, `ble_connect`, `ble_disconnect` and `clock_minute`, on screen only. `clock_minute` runs on the low-power RTC scheduler, so the shown age can lag the true age by up to 59 s.

Flash: draw the word, then one `Ollee.setTimeout(FLASH_MS)` restores the tag. If no timer slot is free (`-1`), skip the flash. `exit()` clears the timer.

Power cost: no timers in the LIVE state, and one 2 s timer per minute in the error states.

The status words must be checked in the emulator's Character set panel, because positions 4 and 6 remap some letters.

### 5. Debug relay (debug builds only)
The Android emulator has no paired watch, so the relay is fed from `BleService` with its own `ForgeFeed`, the same way a watch is fed. It does not tap GATT writes.

`ForgeDebugRelay`:
- lives in the main source set, with every call guarded by `BuildConfig.DEBUG` so R8 strips it from release builds
- a plain `ServerSocket` bound to `127.0.0.1:8765` on a worker thread, no new dependency
- `GET /frames?after=<seq>` returns `{"link":bool,"seq":N,"frames":[{"seq":n,"data":"G83,120 -5"}]}` from a 50-frame ring buffer
- answers `OPTIONS` preflight with `Access-Control-Allow-Origin: *` and `Access-Control-Allow-Private-Network: true`

Dev options gain two switches, "Forge relay" (off by default) and "Relay: watch connected", stored in `DebugStore` and shown in the existing section that is already limited to debug builds. Turning "connected" back on calls `feed.reset()`, like a reconnect.

`tools/forge-bridge/forge-bridge.js` is pasted into the DevTools console on the forge_emulator page. It:
- polls the relay once a second
- sends each frame through the emulator's own UI: sets `#ble-input`, then clicks `#ble-send-btn`, so the emulator's 20-byte check, connection check and BLE log all apply
- mirrors `link` onto the `#ble-connected` checkbox and dispatches `change`, which fires `ble_connect` or `ble_disconnect`
- stops with `forgeBridge.stop()`

Host side: run `adb forward tcp:8765 tcp:8765`. This works for an emulator or for a phone on USB. If Chrome blocks `file://` pages from reaching localhost, serve forge_emulator with `python -m http.server`.

### 6. Local testing findings
Checked on the dev machine (Windows 10) on 2026-11-01: `ANDROID_HOME=C:\DEVEL\Android\sdk`, `emulator.exe` there with one AVD (`Pixel_10`, API 36), `adb` on PATH, Python 3.12, Node 24, `gradlew.bat` in the repo, ExecutionPolicy `RemoteSigned` for the machine (local scripts run).

**Can the Android emulator and forge_emulator be combined directly?** No.
- forge_emulator is a static HTML page. Its Bluetooth panel is a text box, a Send button and a "phone connected" checkbox. It has no Web Bluetooth, socket or serial bridge, no REPL and no headless runner.
- The Android emulator runs the app and the synthetic providers (VirtualHuman, Constant) fine. Its virtual Bluetooth can't reach a physical watch. Verify this against the current emulator during step 8.

With the relay from this spec they can be combined. The chain is: Android emulator with VirtualHuman, the app, `ForgeDebugRelay` on `127.0.0.1:8765`, `adb forward`, the DevTools snippet, forge_emulator's Bluetooth panel, then `glycemia.js`. It covers everything except the GATT hop. Unit tests cover the framing and the queue, and Beta 2 hardware covers the hop itself.

By-hand tricks for forge_emulator (used in the manual): load `glycemia.js` and Run, type frames such as `G83,120 -5`, send `G1500,120 -5` to fake a 25-minute age (the emulator's clock can't be set), untick "phone connected" to test a lost link, "Minute tick" forces a redraw, **M** toggles the face off and on.

Optional GATT check before Beta 2 hardware exists (untested idea): a second Android phone runs nRF Connect's GATT server with service `C6FB0001-...` and an RX characteristic, is renamed so its Bluetooth name contains "Ollee", and is paired. The app connects to it and nRF Connect logs each write.

Beta 2 hardware: upload the face (process to be documented). Toggle Bluetooth or re-pair if the GATT cache hides the service. Only real hardware can test BLE timing, the position-2 glyphs, power use and Espruino quirks (no hoisting, memory).

The document structure is in section 7.

### 7. Windows scripts and the manual
All scripts are PowerShell 7 or Windows PowerShell 5.1 compatible, live in `scripts/forge/`, resolve the SDK from `ANDROID_HOME`, and work from any current directory. State (the forge_emulator clone, the server PID, logs) goes in `/.forge-test/`, which is added to `.gitignore`.

**Debug control receiver.** `ForgeDebugReceiver` in the debug source set (`app/src/debug/`, with its own manifest overlay), so it doesn't exist in release builds at all. Exported, because `adb shell am broadcast` needs that. Action `pl.cukrzycowy.ollee.glycemia.debug.FORGE`, extra `cmd`:

| `cmd` | Extras | Effect |
|---|---|---|
| `setup` | `provider` (default `virtual_human`) | Marks the disclaimer accepted, selects the provider, turns the relay on, sets "watch connected" |
| `link` | `on` / `off` | Sets "Relay: watch connected". `on` also calls `feed.reset()` (a reconnect) |
| `provider` | `id`, optional `value` (for `constant`) | Switches provider through the normal `ACTION_SWITCH_PROVIDER` path |
| `silence` | `on` / `off` | Stops or resumes forwarding readings to the relay, so the age grows (OUTDATED) |
| `pause` | `on` / `off` | Relay sends the `P` frame for a paused watch, or goes back to `G` frames |
| `status` | | Logs relay state, last frame and seq to logcat tag `ForgeDebug` |

Whether an explicit-component broadcast reaches a freshly installed, never-started app on API 36 is verified during implementation. If not, the script starts the activity first.

**Scripts** (`scripts/forge/`):

| Script | What it does |
|---|---|
| `Start-ForgeTest.ps1 [-Avd name] [-Provider virtual_human] [-Port 8765] [-WebPort 8080] [-SkipBuild] [-NoBrowser]` | 1. Finds `adb`, `emulator.exe` and Python, with clear errors. 2. Starts the AVD if no device is online (default: the first one from `emulator -list-avds`), waits for `sys.boot_completed`. 3. Runs `gradlew.bat installDebug` unless `-SkipBuild`. 4. Grants `BLUETOOTH_CONNECT` and `POST_NOTIFICATIONS` with `pm grant`, launches the app, sends `cmd=setup`. 5. `adb forward tcp:<Port> tcp:<Port>`. 6. Clones or fast-forwards forge_emulator into `.forge-test/forge_emulator`. 7. Starts `python -m http.server <WebPort> --bind 127.0.0.1` in the background and records its PID. 8. Copies `tools/forge-bridge/forge-bridge.js` to the clipboard, opens the browser at `http://127.0.0.1:<WebPort>/`, and prints the remaining manual steps (load `watchface\glycemia.js` with Load file, Run, paste the snippet in the DevTools console). |
| `Set-ForgeScenario.ps1 -Link on\|off`, `-Provider id [-Value n]`, `-Silence on\|off`, `-Pause on\|off`, `-Status` | Thin wrapper over the broadcast, so a tester never types an `am broadcast` line |
| `Stop-ForgeTest.ps1 [-KillEmulator]` | Stops the web server by PID, removes the `adb forward`, sets the relay off, optionally runs `adb emu kill` |
| `Invoke-ForgeTests.ps1` | Runs `node --test watchface/test/` and `gradlew.bat testDebugUnitTest`, prints a one-line summary for each |

The web server is on `127.0.0.1`, so the browser treats it as a secure context and allows the page to fetch `http://127.0.0.1:<Port>` (no mixed-content block). The relay's CORS headers cover the rest. The scripts download nothing except the forge_emulator clone, which is an untrusted checkout: it is cloned into its own folder, never executed except as static files in the browser.

**Manual `docs/watchface/TESTING.md`** (replaces the outline in section 6 as the document's structure):
1. Prerequisites and one-time setup (Android Studio SDK, AVD, Python, Node, Chrome).
2. Quick start: `Start-ForgeTest.ps1`, the three manual steps, and a screenshot of what a working setup looks like.
3. Scenario walkthrough, one row per mode, so a tester sees every state:

| Mode to see | Trigger | Expected display |
|---|---|---|
| LIVE | `Start-ForgeTest.ps1` (VirtualHuman emits right away) | Main = tag, Day = age (0 to 4), LAP on |
| OUTDATED | `Set-ForgeScenario.ps1 -Silence on`, then wait 11 min, or type `G700,120 -5` in forge_emulator | PM on, ` old  ` flash, Day = age |
| NO_DATA | type `G-,--- --` in the Bluetooth box | Main `--- --`, Day `--`, LAP on |
| NO_APP | `-Silence on` and wait 11 min, or press "Minute tick" after a frame older than the timeout | `no APP` flash, LAP off, Day counts |
| NO_LINK | `-Link off` (or untick "phone connected") | `no bLE` flash, LAP off, Day counts from the disconnect |
| Recovery | `-Link on` | LAP on, tag back, Day resets |
| PAUSED | `-Pause on` | Main = label, Day `--`, no flash |
| Age beyond 39 min | type `G2400,120 -5` | Day `--` |

4. Manual-only variant with no scripts (forge_emulator in a browser, typing frames).
5. The same relay with a real phone over USB.
6. A real watch on current firmware, as a regression check.
7. Optional GATT check before Beta 2 hardware (the nRF Connect idea in this section).
8. Troubleshooting: emulator won't boot, `adb forward` already in use, browser blocks the fetch, no frames arriving (`-Status`, logcat tag `ForgeDebug`), face disabled by an exception (reload it).
9. What only real hardware can test.

## Files
New, app (in `app/src/main/java/pl/cukrzycowy/ollee/glycemia/`):
- `ForgeProtocol.kt`: UUIDs, `glycemiaFrame(ageSec, text)`, `pausedFrame(text)`, ASCII sanitize, the 20-byte check
- `OapProtocol.kt`: `crc16` and `screensaverPacket(text)`, moved out of `WatchConnection.kt` with no behaviour change
- `WatchPayload.kt`
- `ForgeFeed.kt`
- `GattWriteQueue.kt`
- `ForgeDebugRelay.kt`

Changed, app:
- `WatchConnection.kt`: queue, Forge detection, `onCharacteristicWrite`, payload, `tick`, main-thread confinement
- `BleService.kt`: payload building, keepalive tick, relay feed
- `WatchConnState.kt`: `WatchStatus.forgeSupported`
- `ui/screens/MainScreen.kt`: watchface marker in the watch row
- `ui/screens/SettingsScreen.kt`, `DebugStore.kt`: relay switches
- `PreferencesBackupManager.kt`: only if a new non-debug setting appears (none planned)
- `res/values/strings.xml`, `values-pl`, `values-fr`

Watchface and tooling:
- `watchface/glycemia.js`
- `watchface/README.md`
- `watchface/test/harness.js`: a Node `vm` context with firmware-like globals only, a fake `Ollee` (records draws only while active, has 4 timer slots, settable `connected`, `subscribe`) and a controllable `Date`
- `watchface/test/glycemia.test.js`
- `watchface/test/fixtures/frames.json`: protocol vectors shared with the Kotlin tests
- `tools/forge-bridge/forge-bridge.js`
- `tools/forge-bridge/README.md`
- `app/src/debug/AndroidManifest.xml` and `app/src/debug/java/pl/cukrzycowy/ollee/glycemia/ForgeDebugReceiver.kt`
- `scripts/forge/Start-ForgeTest.ps1`, `Set-ForgeScenario.ps1`, `Stop-ForgeTest.ps1`, `Invoke-ForgeTests.ps1`
- `.gitignore`: add `/.forge-test/`

Docs:
- `docs/watchface/PROTOCOL.md`
- `docs/watchface/TESTING.md`
- `README.md`: new "Forge watchface (beta)" section
- `CHANGELOG.md`: `- **Forge watchface**: ...` under `[Unreleased]`, in house style

## Steps
1. Protocol: `OapProtocol` extraction with a golden test, `ForgeProtocol`, the shared fixtures, `PROTOCOL.md`.
2. Watchface: `glycemia.js`, the harness and tests, then a manual run in forge_emulator.
3. Transport: `GattWriteQueue`, `ForgeFeed`, `WatchPayload`, the `WatchConnection` changes and main-thread confinement.
4. Service wiring: payloads at every submit point, keepalive in the timeout watcher.
5. UI: watchface marker and strings (EN, PL, FR).
6. Debug relay, dev-option switches, the debug control receiver and the bridge snippet.
7. PowerShell scripts in `scripts/forge/` and the `.gitignore` entry.
8. Docs: `TESTING.md` (the manual), `watchface/README.md`, README section, CHANGELOG.
9. End-to-end run on a clean state following only `TESTING.md` (every row of the scenario table), plus a regression check on current firmware with a real watch.

## Tests
Kotlin unit tests in `app/src/test/java/pl/cukrzycowy/ollee/glycemia/` (Robolectric, `sdk=[35]`):
- `OapProtocolTest`: `"120  5"` gives exactly today's bytes. CRC-16/CCITT-FALSE of `"123456789"` is `0x29B1`.
- `ForgeProtocolTest`: the shared fixtures, every frame at most 20 bytes, age clamping and `-`, non-ASCII and control characters become `?`.
- `ForgeFeedTest`: sends on change, no resend before the keepalive interval, keepalive at 5 min, `reset()` resends, age taken at build time.
- `GattWriteQueueTest` (main looper, fake writer): strict order, the next write waits for the completion callback, a timeout moves the queue on, `clear()` drops pending writes, the drain callback fires.
- `ForgeDebugRelayTest`: ephemeral port, JSON shape, CORS headers, `after=` filtering.

Watchface tests, run with `node --test watchface/test/`:
- the script is at most 8158 bytes
- loading the script makes no `Ollee` calls
- a G frame sets Main and Day
- Day advances on `clock_minute`
- 40 min shows `--`, and age `-` shows `--`
- OUTDATED: PM on, the flash appears and then reverts
- NO_LINK: Day counts from the disconnect
- NO_APP: no frame for 11 min
- PAUSED: no flash
- frames received in the background show up on `enter()`
- malformed frames (`""`, `G`, `Gx,`, `Z1`, 20 bytes of junk) don't throw
- a negative age clamps to 0
- no free timer slot is handled
- `exit()` clears the flash

`ForgeDebugReceiverTest` (Robolectric): each `cmd` changes the expected state, an unknown `cmd` is ignored.

Scripts: no automated tests. `Start-ForgeTest.ps1 -?` and a parse check (`[System.Management.Automation.Language.Parser]::ParseFile`) run in `Invoke-ForgeTests.ps1`. The real check is the end-to-end run in step 9.

Manual checks: forge_emulator by hand, the scripted relay loop (every row of the scenario table), and a real watch on current firmware (tag only, nothing else changes).

_Note: CLAUDE.md names `app/src/test/java/com/arthur/bgollee/` as the test location. The real path is the one above._
