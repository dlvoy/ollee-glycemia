# forge-bridge

Replays what the app would send to a Forge watch into [forge_emulator](https://github.com/olleewatch/forge_emulator) (the submodule next to this folder, `tools/forge_emulator`), so the watchface can be tried without a watch.

```
app (debug build) -> ForgeDebugRelay 127.0.0.1:8765 -> adb forward -> forge-bridge.js (DevTools) -> forge_emulator Bluetooth panel -> glycemia.js
```

- `forge-bridge.js` is pasted into the DevTools console of the forge_emulator page. It polls `GET /frames?after=<seq>`, types each frame into the emulator's Bluetooth text box, and mirrors the relay's link state onto the "phone connected" checkbox.
- The relay is debug-build only: Settings, Developer options, "Forge relay" (or `Start-ForgeTest.ps1`, which switches it on).
- The element ids it uses (`ble-input`, `ble-send-btn`, `ble-connected`) are checked against the pinned emulator by `watchface/test/emulator-compat.test.js`.

The step-by-step manual is in [docs/watchface/TESTING.md](../../docs/watchface/TESTING.md). `scripts/forge/Start-ForgeTest.ps1` copies this snippet to the clipboard for you.
