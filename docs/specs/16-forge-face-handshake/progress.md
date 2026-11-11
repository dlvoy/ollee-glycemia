# 16: Forge face handshake (progress)

**Status:** in-progress
**Release:** v2.0.0

## Checklist
- [x] Intent agreed
- [x] Plan written
- [x] Fixtures, `ForgeProtocol` additions and tests
- [x] Face changes, harness `send`, Node tests
- [x] `ForgeHandshake` and its tests
- [x] `WatchConnection` wiring
- [x] Docs, CHANGELOG
- [x] `Invoke-ForgeTests.ps1` and the face in forge_emulator
- [ ] Real Beta 2 watch: confirm the TX UUID with nRF Connect, then the app's icon (TESTING.md section 8)

## Log
- 2026-11-11: intent drafted, waiting for review. The UI side (watchface icon and the stored `faceDetected` flag) moved to spec 17 so it can ship first
- 2026-11-11: the user said "implement intent 16"; plan written and implemented in the same session
- 2026-11-11: plan change during the work: `ForgeHandshake` doesn't compute time. `WatchConnection`'s handler owns the 5 s timer, and the class tracks a pending query instead. Reason: a notification can arrive before the `Q` write callback, and the first design would then time out a face that had already answered
- 2026-11-11: unit tests green (Gradle 191, including `ForgeHandshakeTest` 13 and `ForgeProtocolTest` 6; Node 34). In the pinned forge_emulator, Run with "phone connected" logged `BLE notify -> phone: "H1,gly,2"`, and so did typing `Q` after a `G` frame; the display stayed on `120 -5`. Face size 5479 of 8158 bytes. Not committed yet

### Acceptance criteria status
- Detected icon within 10 s, kept while offline: code path and storage are unit-tested. Needs the real watch
- Gray icon for a missing face or the v1.7.0 face: unit-tested (timeout path). Needs the real watch
- A face installed while connected shows up within 5 min (or at once via the `init()` hello): unit-tested (keepalive re-query, unsolicited hello)
- Non-Forge firmware: no TX subscription and no `Q` (the code path is gated on `forgeRx`). `OapProtocolTest` golden bytes unchanged
- Foreign or malformed notifications are ignored: unit-tested
- Size limit and test suites: done
- Real Beta 2 watch: open
