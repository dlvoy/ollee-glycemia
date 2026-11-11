# 17: Watch row status icons (progress)

**Status:** done
**Release:** v2.0.0

## Checklist
- [x] `PairedWatch.faceDetected` and `WatchStore` persistence, with tests
- [x] `WatchStatusStyle.kt` and its test
- [x] `MainScreen.kt` layout, strings (EN, PL, FR)
- [x] CHANGELOG entry
- [x] Manual check of the watch list: five fake watches seeded into the debug app's prefs on the Android emulator. Connecting, Paused and Stopped rendered with their icons. The watchface icon showed in blue (Forge + face), gray crossed out (Forge, no face) and not at all (plain watch). Synced, Offline and Error can't occur on the emulator; `WatchStatusStyleTest` covers them

## Log
- 2026-11-11: intent and plan written; the user chose to build the icons now and leave detection to spec 16
- 2026-11-11: implemented. `WatchStatusStyleTest` (7) and `WatchStoreTest` (9) green, full suite 176 green. Not committed yet
- 2026-11-11: spec 16 sets `faceDetected` now. Visual check on the emulator with seeded watches (prefs restored afterwards); closed
