# Testing the Forge watchface locally

You can see the whole chain working without a watch: the app on an Android emulator makes synthetic glucose readings, sends them the way it would send them to a Forge watch, and the [forge_emulator](https://github.com/olleewatch/forge_emulator) page shows what `watchface/glycemia.js` draws. forge_emulator is a git submodule of this repo in `tools/forge_emulator`, pinned to a known commit (Beta 2, tag `Fall_Q4_2026_Beta`).

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
| git | The emulator is a submodule. `Start-ForgeTest.ps1` checks it out if needed, or run `git submodule update --init tools/forge_emulator` (or clone with `--recurse-submodules`) |
| Chrome (or another Chromium browser) | |

If PowerShell refuses to run the scripts: `powershell -ExecutionPolicy Bypass -File .\scripts\forge\Start-ForgeTest.ps1`.

## 2. Quick start

From the repo root:

```powershell
.\scripts\forge\Start-ForgeTest.ps1
```

This boots the emulator if none is running, builds and installs the debug app, grants permissions, accepts the disclaimer, selects the synthetic `virtual_human` provider, turns the relay on, forwards the port, checks out the forge_emulator submodule if it is missing, serves `tools\forge_emulator` on `http://127.0.0.1:18080/`, copies the bridge snippet to your clipboard and opens the page. Options: `-Avd <name>`, `-Provider constant`, `-SkipBuild`, `-NoBrowser`, `-Port`, `-WebPort`.

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
| Handshake | Type `Q` and Send. Pressing **Run** with **phone connected** ticked also works (the `init()` hello) | The log shows `BLE notify -> phone: "H1,gly,2"`; the display doesn't change |

The relay doesn't send `Q` and forge-bridge doesn't carry the reply back to the app, so in this loop the handshake is checked by hand only. The app side has unit tests (`ForgeHandshakeTest`), and the real check needs a watch (section 8).

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

1. Open `tools\forge_emulator\index.html` in a browser, or serve that folder: `python -m http.server 18080 --bind 127.0.0.1`.
2. Load `watchface/glycemia.js`, press Run.
3. Type frames by hand into the Bluetooth box, for example `G83,120 -5`, `P, pause`, `G700,120 -5`. The frame format is in [PROTOCOL.md](PROTOCOL.md).
4. Untick **phone connected** to lose the link.

To feed from the app instead: build and install the debug app, unlock Developer options (tap the version 7 times in Settings), switch on **Forge relay** and run `adb forward tcp:8765 tcp:8765`; then paste `tools/forge-bridge/forge-bridge.js` into the DevTools console.

## 5. Automated tests

```powershell
.\scripts\forge\Invoke-ForgeTests.ps1       # parse check, submodule state, node tests, gradle tests
node --test "watchface/test/*.test.js"      # the face only, no Android needed
.\gradlew.bat testDebugUnitTest             # the app only
```

The face tests run `glycemia.js` in a Node `vm` with a fake Forge runtime (`watchface/test/harness.js`): draw calls are ignored off screen, four timer slots return `-1` when full, the clock is controllable. They cover each mode, the 40-minute cut-off, malformed frames, background delivery and the 8158 byte limit. `watchface/test/fixtures/frames.json` holds the protocol vectors used by both the Node and Kotlin tests.

`watchface/test/emulator-compat.test.js` checks the stand-ins against the pinned emulator: the Bluetooth panel ids that `forge-bridge.js` uses, the event bits in the harness, the script byte limit and the BLE write size. It skips itself when the submodule isn't checked out.

### Moving the emulator pin

The pin only moves when someone commits a new one:

```powershell
git submodule update --remote tools/forge_emulator   # to the latest upstream main
.\scripts\forge\Invoke-ForgeTests.ps1                # emulator-compat shows what broke
git add tools/forge_emulator                         # then commit the new pin
```

`git submodule update tools/forge_emulator` goes back to the pinned commit after trying another one.

## 6. A real phone instead of the emulator

The same relay works with a phone over USB: install the debug build, switch on **Forge relay** in Developer options (or run `Start-ForgeTest.ps1 -SkipBuild` with the phone connected and no emulator running; it uses the online device), and continue as above. Set `FORGE_SERIAL` if several devices are attached.

## 7. A real watch on current firmware (regression check)

Watches without Forge must behave as before. Pair a watch, let it receive a reading, and check:

- the screensaver / long-press text updates as usual
- the watch row shows no watchface icon next to the status icon
- pausing and resuming a watch still writes the pause label

## 8. A Forge watch with the face (Beta 2)

1. Upload `watchface/glycemia.js` to a Forge slot: forge_emulator's **Upload** (or **Save script**), then the Ollee Watch app, Settings > Forge Apps. See upstream's [Deploying to your watch](https://github.com/olleewatch/forge_emulator/blob/main/docs/guides/deploying-to-watch.md).
2. Before involving this app, confirm the UUIDs with nRF Connect: connect to the watch, find service `C6FB0001-…`, enable notifications on `C6FB0003-A37D-40AC-89BE-887D25E1F5B3`, write the text `Q` to `C6FB0002-A37D-40AC-89BE-887D25E1F5B3`. Expect the notification `H1,gly,2`. If the last digits differ from the ones in `ForgeProtocol.kt`, fix them there first. Disconnect nRF Connect afterwards.
3. Pair the watch in the app (or tap sync on its row). Within a few seconds the watchface icon next to the status icon turns from a gray crossed-out watch into a watch in the status color.
4. `adb logcat -s WatchConnection` shows `Glycemia face v2 answered on …`, or `No glycemia face answered on …` after 5 s without a reply.
5. Remove the face from its slot and tap sync: the icon goes gray again. Install it again: it comes back with the next reading (at most 5 minutes) or on sync.

## 9. Without a Forge watch: an optional GATT check (untested idea)

To exercise the real GATT path, a second Android phone can act as the watch: run a GATT server app (for example nRF Connect's Advertiser/Server) with service `C6FB0001-A37D-40AC-89BE-887D25E1F5B3` and a writable characteristic `C6FB0002-A37D-40AC-89BE-887D25E1F5B3`, give that phone a Bluetooth name containing "Ollee", and pair it in the app. The watch row should show the gray watchface icon (Forge found, face not detected) and nRF Connect should log the frames. The Nordic UART service must be present too, or the app has nothing to write the tag text to. To play the face in the handshake, add a notify characteristic `C6FB0003-A37D-40AC-89BE-887D25E1F5B3` and send the notification `H1,gly,2` when the `Q` write arrives (or right after the app subscribes): the icon turns on.

## 10. What only real hardware can show

- BLE timing and reconnect behaviour with the real firmware
- the position-2 glyphs on the real LCD (`-`, `1`, `2`, `3` on the first Day digit) and the status words on positions 4 and 6, where the LCD remaps some letters
- battery use of the background subscription
- Espruino limits that the browser emulator doesn't enforce (memory, no function hoisting)
- whether BLE must be started by hand for the face to get data (forge_emulator issue #3). Uploading itself is documented in Beta 2: [Deploying to your watch](https://github.com/olleewatch/forge_emulator/blob/main/docs/guides/deploying-to-watch.md)

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
| `tools\forge_emulator` is empty | The submodule isn't checked out: `git submodule update --init tools/forge_emulator` |
| Start script warns the emulator is not at the pinned commit | You moved the submodule. `git submodule update tools/forge_emulator` goes back |
