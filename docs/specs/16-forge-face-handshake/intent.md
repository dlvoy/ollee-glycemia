# 16: Forge face handshake

## Problem
The app knows whether a watch's firmware has the Forge BLE service (`PairedWatch.forgeSupported`), but not whether `watchface/glycemia.js` is installed in one of the three Forge slots (FA, FB, FC). So:
- A Forge watch without the face accepts every `G` frame anyway. The firmware sends the GATT write response, not the face, so a write looks delivered whether or not a face receives it.
- The Forge API has no call that lists installed faces.
- The user can't tell from the phone whether the watch shows glycemia, or only the old screensaver text.

Spec 17 adds a watchface icon to the watch row (`Watch` in the status color, gray `WatchOff` otherwise). Until this spec lands it reads a stored flag that nothing sets, so the icon stays gray.

Forge Beta 2 (forge_emulator tag `Fall_Q4_2026_Beta`) documents `Ollee.send(data)`: the face can notify the phone over the TX characteristic, up to 256 bytes, sent as 20-byte notifications. That is the missing return channel.

## Goal
On each connection the app learns whether the glycemia face is installed and listening, and which face version runs. The result is stored per watch, so the watch row icon shows it while the watch is offline too.

## Scope
- Protocol v1.1, backward compatible with v1:
  - `Q` (query) frame, phone to watch, on RX as before
  - `H` (hello) reply, watch to phone, as a TX notification. It carries a face identifier, the protocol version and the face version, and fits in one 20-byte notification. The exact format is decided in the plan. The identifier matters because other Forge faces subscribed to `E_BLE_RX` may get the `Q` too and reply with something else.
- Watchface:
  - replies `H` to `Q`
  - also sends `H` from `init()` when a phone is connected, so a face installed while the app is connected shows up at once
  - stays at most 8158 bytes and keeps ignoring unknown frames
- App:
  - after service discovery, turn on notifications for the Forge TX characteristic. That is a CCCD descriptor write, so `GattWriteQueue` has to handle descriptor writes as well as characteristic writes
  - send `Q` once notifications are on, when the user taps sync, and with each keepalive while the face is not detected (so a face installed later shows up within 5 minutes)
  - accept only a well-formed `H` with our identifier and ignore every other notification
  - no reply within a short timeout after `Q` means "not detected"
  - store the result per watch through `WatchStore.setFaceDetected` (added by spec 17) and keep the face version for logs
  - firmware without Forge: no TX subscription, no `Q`, behaviour as today
- Tests:
  - Node: the harness gets a fake `Ollee.send`; cover the `Q`/`H` exchange, the `init()` hello and malformed queries
  - Kotlin: `H` parsing, descriptor writes in the queue, and the detection state (detected, not detected after the timeout, re-query on keepalive, reset on disconnect)
  - shared vectors in `watchface/test/fixtures/frames.json`
- Docs: a v1.1 section in `docs/watchface/PROTOCOL.md`. `docs/watchface/TESTING.md` gets the emulator check (type `Q`, expect `H…` in the log) and the real-watch check with nRF Connect.

## Out of scope
- Installing or updating the face from this app. The official Ollee Watch app does that (Settings > Forge Apps).
- Other watch-to-phone messages: refresh requests, button events, acknowledgements of `G` frames.
- Telling the user (notification, banner) that the face went missing.
- Carrying TX back through the debug relay. In the emulator loop the reply only shows in forge_emulator's log, and forge-bridge doesn't post it back to the app.
- Showing the face version in the UI.

## Acceptance criteria
- [ ] A watch running the new face shows the detected watch icon within 10 s of connecting, and keeps showing it while offline.
- [ ] A Forge watch without the face, or with the v1.7.0 face (no handshake), shows the gray icon after the timeout. `G` frames keep going out as before and nothing logs an error.
- [ ] A face installed while the app is connected is detected within 5 minutes at most, and at once when the `init()` hello gets through.
- [ ] On firmware without Forge the app makes no TX subscription and sends no `Q`; the screensaver packet bytes don't change.
- [ ] Notifications from other faces, and malformed `H` frames, are ignored and never mark the face as detected.
- [ ] The face stays at most 8158 bytes. `node --test "watchface/test/*.test.js"` and `gradlew testDebugUnitTest` pass.
- [ ] Checked on a real Beta 2 watch: the TX UUID is confirmed and the handshake works.

## Open questions
- The TX UUID `C6FB0003-A37D-40AC-89BE-887D25E1F5B3` is inferred from the service pattern, as RX was. The Beta 2 docs still print only `C6FB0003-...`. Confirm with nRF Connect on a watch.
- Does the firmware hand one RX write to every face that subscribed to `E_BLE_RX` (up to three slots)? If two faces answer, only ours counts, but the other face may misread `Q`.
- Can the watch hold a connection from the official Ollee Watch app and this app at the same time? Right after an upload the official app is connected, so the `init()` hello may go to it and never reach us. The keepalive re-query covers that case.
- Does `init()` run with BLE already up after an upload, so `Ollee.connected()` is true there?
