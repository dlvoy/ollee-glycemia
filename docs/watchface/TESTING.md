# Testing the Forge watchface locally

You can see the whole chain working without a watch: the app on an Android emulator makes synthetic glucose readings, sends them the way it would send them to a Forge watch, and the [forge_emulator](https://github.com/olleewatch/forge_emulator) page shows what `watchface/glycemia.js` draws.

**Can the Android emulator and forge_emulator be connected directly?** No. forge_emulator is a static web page. Its Bluetooth panel is a text box, a Send button and a "phone connected" checkbox, with no Web Bluetooth, socket or serial bridge. And the Android emulator's virtual Bluetooth can't reach a physical watch. This repo bridges the gap with a debug-only relay in the app plus a small DevTools snippet:

```
Android emulator
  app (debug build, synthetic provider)
    ForgeDebugRelay on 127.0.0.1:8765        <- same payloads a watch would get
        | adb forward tcp:8765 tcp:8765
  PC browser: forge_emulator (served on 127.0.0.1:18080)
    forge-bridge.js (pasted in DevTools)     <- types each frame into the Bluetooth panel
      glycemia.js                            <- the watchface
```

What this covers: the frame protocol, the keepalive and pacing policy, the write queue (unit tests), and every mode of the face. What it can't cover: the real GATT write to a watch, BLE timing, power use, and the real LCD glyphs. Those need Beta 2 hardware (see the end).

## 1. Prerequisites (one time)

| Needed | Notes |
|---|---|
| Android SDK with the emulator and one AVD | `ANDROID_HOME` set, e.g. `C:\DEVEL\Android\sdk`. Check with `& "$env:ANDROID_HOME\emulator\emulator.exe" -list-avds` |
| JDK 17 or newer | The scripts use `JAVA_HOME` when it is new enough, else Android Studio's bundled JBR |
| Python 3 | Only to serve forge_emulator on localhost |
| Node 22+ | Only for the watchface unit tests |
| git, Chrome (or another Chromium browser) | |

If PowerShell refuses to run the scripts: `powershell -ExecutionPolicy Bypass -File .\scripts\forge\Start-ForgeTest.ps1`.

## 2. Quick start

From the repo root:

```powershell
.\scripts\forge\Start-ForgeTest.ps1
```

This boots the emulator if none is running, builds and installs the debug app, grants permissions, accepts the disclaimer, selects the synthetic `virtual_human` provider, turns the relay on, forwards the port, fetches forge_emulator into `.forge-test\`, serves it on `http://127.0.0.1:18080/`, copies the bridge snippet to your clipboard and opens the page. Options: `-Avd <name>`, `-Provider constant`, `-SkipBuild`, `-NoBrowser`, `-Port`, `-WebPort`.

Then three manual steps in the browser page:

1. **Load file** and pick `watchface\glycemia.js` (the script prints the full path), then press **Run**.
2. Open DevTools (F12), go to the **Console**. If Chrome blocks pasting, type `allow pasting` and press Enter once.
3. Paste (Ctrl+V) and press Enter. The console prints `[forge-bridge] relay reachable` and the first frame.

You should now see the tag and the age in the emulated LCD, and the BLE log in the Bluetooth panel filling with `G<age>,<tag>` frames.

Stop everything with:

```powershell
.\scripts\forge\Stop-ForgeTest.ps1 [-KillEmulator]
```

## 3. Scenario walkthrough

Every mode of the face, with the command that triggers it and what to expect. Run the commands from the repo root; `Set-ForgeScenario.ps1` talks to the debug-only receiver in the app.

| Mode | Trigger | Expected display |
|---|---|---|
| LIVE | Right after `Start-ForgeTest.ps1` (VirtualHuman emits at once, then every 5 min) | Main = tag such as `120 -5`, Day = age (` 0` to ` 4`), LAP on, PM off |
| Age beyond 39 min | Type `G2400,120 -5` in the Bluetooth box and Send | Day `--` (and ` old  ` flash, PM on) |
| NO_DATA | Type `G-,--- --` and Send | Main `--- --`, Day `--`, LAP on, no flash |
| OUTDATED | `.\scripts\forge\Set-ForgeScenario.ps1 -Silence on` (readings stop, keepalives keep arriving with a growing age), then wait about 10.5 min. Faster: type `G700,120 -5` | PM on, ` old  ` flashes on Main for 2 s each minute, Day = age |
| NO_APP | `.\scripts\forge\Set-ForgeScenario.ps1 -Mute on` (the app stops sending anything, but the Bluetooth link stays up), then wait 11 minutes. There is no shortcut: the face measures real time | `no APP` flashes, LAP off, Day counts minutes since the last frame. `-Mute off` brings the live display back |
| NO_LINK | `.\scripts\forge\Set-ForgeScenario.ps1 -Link off` (or untick **phone connected**) | `no bLE` flashes, LAP off, Day counts minutes since the disconnect |
| Recovery | `.\scripts\forge\Set-ForgeScenario.ps1 -Link on` | LAP on, tag back, Day resets |
| PAUSED | `.\scripts\forge\Set-ForgeScenario.ps1 -Pause on` (then `-Pause off`) | Main = ` pause`, Day `--`, LAP and PM off, no flash even if the link drops |

Tips for forge_emulator:

- **Minute tick** fires `clock_minute()` at once, without moving the clock. The Day value is computed from the real PC clock, so to age a reading quickly type a frame with a large age, for example `G1500,120 -5` (25 minutes).
- **M** toggles the face off and on (calls `exit()` / `enter()`), which is how you see the flash start again.
- The face receives frames while another face is shown, too (background subscription); switch away with **M**, send a frame, switch back.
- The Bluetooth log marks dropped writes (face not subscribed, phone not connected, over 20 bytes).

Useful commands:

```powershell
.\scripts\forge\Set-ForgeScenario.ps1 -Status                        # relay state and the last frame
.\scripts\forge\Set-ForgeScenario.ps1 -Provider constant -Value 180   # a fixed value
.\scripts\forge\Set-ForgeScenario.ps1 -Provider virtual_human         # back to the generator
adb logcat -s ForgeDebug WatchConnection                              # what the app sends
```

## 4. Without the scripts

1. Open `index.html` of forge_emulator in a browser, or serve it: `python -m http.server 18080 --bind 127.0.0.1`.
2. Load `watchface/glycemia.js`, press Run.
3. Type frames by hand into the Bluetooth box, for example `G83,120 -5`, `P, pause`, `G700,120 -5`. The frame format is in [PROTOCOL.md](PROTOCOL.md).
4. Untick **phone connected** to lose the link.

To feed from the app instead: build and install the debug app, unlock Developer options (tap the version 7 times in Settings), switch on **Forge relay** and run `adb forward tcp:8765 tcp:8765`; then paste `tools/forge-bridge/forge-bridge.js` into the DevTools console.

## 5. Automated tests

```powershell
.\scripts\forge\Invoke-ForgeTests.ps1       # parse check, node tests, gradle tests
node --test "watchface/test/*.test.js"      # the face only, no Android needed
.\gradlew.bat testDebugUnitTest             # the app only
```

The face tests run `glycemia.js` in a Node `vm` with a fake Forge runtime (`watchface/test/harness.js`): draw calls are ignored off screen, four timer slots return `-1` when full, the clock is controllable. They cover each mode, the 40-minute cut-off, malformed frames, background delivery and the 8158 byte limit. `watchface/test/fixtures/frames.json` holds the protocol vectors used by both the Node and Kotlin tests.

## 6. A real phone instead of the emulator

The same relay works with a phone over USB: install the debug build, switch on **Forge relay** in Developer options (or run `Start-ForgeTest.ps1 -SkipBuild` with the phone connected and no emulator running; it uses the online device), and continue as above. Set `FORGE_SERIAL` if several devices are attached.

## 7. A real watch on current firmware (regression check)

Watches without Forge must behave as before. Pair a watch, let it receive a reading, and check:

- the screensaver / long-press text updates as usual
- the watch row shows no "Watchface" marker
- pausing and resuming a watch still writes the pause label

## 8. Before Beta 2 hardware: an optional GATT check (untested idea)

To exercise the real GATT path, a second Android phone can act as the watch: run a GATT server app (for example nRF Connect's Advertiser/Server) with service `C6FB0001-A37D-40AC-89BE-887D25E1F5B3` and a writable characteristic `C6FB0002-A37D-40AC-89BE-887D25E1F5B3`, give that phone a Bluetooth name containing "Ollee", and pair it in the app. The watch row should show the "Watchface" marker and nRF Connect should log the frames. The Nordic UART service must be present too, or the app has nothing to write the tag text to.

## 9. What only real hardware can show

- BLE timing and reconnect behaviour with the real firmware
- the position-2 glyphs on the real LCD (`-`, `1`, `2`, `3` on the first Day digit) and the status words on positions 4 and 6, where the LCD remaps some letters
- battery use of the background subscription
- Espruino limits that the browser emulator doesn't enforce (memory, no function hoisting)
- how a face is uploaded and whether BLE must be started by hand (forge_emulator issue #3); Beta 2 documents this

## Troubleshooting

| Symptom | Check |
|---|---|
| `adb not found` / `emulator.exe not found` | `ANDROID_HOME` must point at the SDK |
| Gradle: unsupported Java version | Use JDK 17+; the scripts pick Android Studio's JBR automatically |
| `App setup failed` | The receiver exists in debug builds only; run without `-SkipBuild` |
| Console says `relay not reachable` | `adb forward --list` shows `tcp:8765`; `Set-ForgeScenario.ps1 -Status` says `running=true`; the browser page is served from `127.0.0.1` |
| Nothing arrives in the Bluetooth log | The face must be Run and on screen (or subscribed in the background); **phone connected** must be ticked |
| The face stops reacting | An uncaught error disables a face. Check the browser console and press Run again |
| `Cannot find module 'watchface\test'` | Quote the glob: `node --test "watchface/test/*.test.js"` |
| Port already in use | `Start-ForgeTest.ps1 -WebPort 8081 -Port 8766` |
