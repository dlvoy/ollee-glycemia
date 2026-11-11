# Forge watchface protocol (v1.1)

How the app feeds the glycemia watchface (`watchface/glycemia.js`) on a watch that runs Ollee's Forge beta. Design notes are in [spec 15](../specs/15-forge-glycemia-watchface/plan.md). The handshake (v1.1) is in [spec 16](../specs/16-forge-face-handshake/plan.md).

## Transport

The Forge BLE service sits on the same GATT server as the Nordic UART service the app already uses for the screensaver text, so one connection carries both.

| | UUID |
|---|---|
| Forge service | `C6FB0001-A37D-40AC-89BE-887D25E1F5B3` |
| RX (phone writes) | `C6FB0002-A37D-40AC-89BE-887D25E1F5B3` (inferred from the service pattern; forge_emulator only prints `C6FB0002-...`) |
| TX (watch notifies) | `C6FB0003-A37D-40AC-89BE-887D25E1F5B3` (inferred the same way; only the handshake reply uses it) |

- One frame per write, at most 20 bytes. The watch hands each write to the face as one `ble_rx(data)` call.
- Frames are printable ASCII (0x20 to 0x7E), so any frame can be typed into forge_emulator's Bluetooth text box.
- No acknowledgement of `G`/`P` frames and no framing layer. The app serializes its GATT writes (`GattWriteQueue`) because Android allows one in flight.

## Frames

| Frame | Meaning |
|---|---|
| `G<age>,<text>` | Live state |
| `P,<text>` | The watch is paused or stopped in the app |

- `<age>`: whole seconds from the measurement to the moment the frame is written, decimal `0` to `86400` (larger values are clamped), or `-` when there is no measurement.
- `<text>`: the 6-character tag the app also sends as the screensaver text, for example `120 -5`, `120  5`, `--- --` or `Err   `. It is the last field, so it needs no escaping (it may contain commas).
- `P` carries the pause/stop label (`" pause"`, `"  stop"` by default).

Examples:

| Frame | Bytes | The face shows |
|---|---|---|
| `G83,120 -5` | 10 | Main `120 -5`, Day ` 1`, LAP on |
| `G-,--- --` | 9 | Main `--- --`, Day `--`, LAP on |
| `G700,120 -5` | 11 | Main `120 -5` after a ` old  ` flash, Day `11`, LAP and PM on |
| `P, pause` | 8 | Main ` pause`, Day `--`, LAP and PM off |

Machine-readable vectors, shared by the Kotlin and Node tests: [`watchface/test/fixtures/frames.json`](../../watchface/test/fixtures/frames.json).

The face ignores frames it can't parse and frames with an unknown type letter, so new frame types can be added later without breaking old faces.

## When the app sends

- on every change of the shown value (a new reading, "Err", cleared, a resume)
- once after each (re)connect, when the Forge service is found
- at least every 5 minutes (keepalive, driven by the service's existing 60 s timer), so the face can tell a quiet app from a lost one
- once more when the user taps sync on the watch row

The age is computed when the write is dequeued, so it is accurate to the second at the time of the write. The face stores `measuredAt = now - age` on its own RTC and counts forward from there. No clock sync between phone and watch is needed.

## Handshake (v1.1)

A Forge watch accepts writes whether or not the glycemia face is installed, because the firmware acknowledges them, not the face. The handshake lets the app find out.

| Frame | Direction | Meaning |
|---|---|---|
| `Q` | phone to watch, RX write | Which face is listening? The face only looks at the first letter |
| `H<protocol>,<face id>,<face version>` | watch to phone, TX notification | The reply. Today `H1,gly,2` |

- `<protocol>` is `1`, the v1 frame family. `<face id>` is `gly`, so replies from other Forge faces are ignored. `<face version>` is `FACE_VERSION` in `glycemia.js`. Version 1 was the face in app v1.7.0, which has no handshake.
- The app accepts extra fields after the version, so later faces can add some.
- The face answers on and off screen (background subscription). It also says hello from `init()` when a phone is connected, which covers a face installed while the app is connected.
- `Q` doesn't count as app traffic on the face: the tag, Day, LAP and the 11-minute NO_APP timer ignore it.

The app side (`ForgeHandshake`, `WatchConnection`):
1. After service discovery, an ACTIVE watch with the Forge RX and TX characteristics gets TX notifications turned on (a CCCD descriptor write through the same queue).
2. Then `Q`. A valid `H` marks the face as detected. No reply within 5 s of the write marks it as not detected.
3. While the face is not detected, every `G` frame (a reading or a keepalive) comes with another `Q`, so a face installed later shows up within 5 minutes. Tapping sync asks again even when the face was detected.
4. The result is stored per watch (`PairedWatch.faceDetected`) and drives the watchface icon on the watch row. A disconnect before the answer keeps the last known value.
5. Paused and stopped watches skip the handshake: they hang up right after the label, before any reply could arrive. Firmware without Forge gets no TX subscription and no `Q`. Forge without a usable TX characteristic counts as not detected.

If the watch does not expose the Forge service, none of this happens and the app behaves exactly as before: only the screensaver packet is written (see `OapProtocol`).

## What the face does with it

| Mode | Condition | Main | Day | LAP | PM | Flash |
|---|---|---|---|---|---|---|
| PAUSED | last frame was `P` | label | `--` | off | off | none |
| NO_LINK | no BLE connection | last text | minutes since the disconnect | off | off | `no bLE` |
| NO_APP | connected, but no frame yet or none for 11 min | last text | minutes since the last frame | off | off | `no APP` |
| OUTDATED | live, reading at least 10.5 min old | text | age | on | on | ` old  ` |
| NO_DATA | live, age `-` | text | `--` | on | off | none |
| LIVE | live, reading under 10.5 min old | text | age | on | off | none |

- Day shows whole minutes 0 to 39, right-aligned. From 40 minutes up, or when unknown, it shows `--`: the first Day digit on the real LCD only renders `1` to `3`.
- A flash replaces Main with the status word for 2 s, on each minute tick, on entering the face and on each mode change, then the tag comes back.
- The face redraws on `clock_minute()`, so the shown age can lag the real age by up to 59 s. LIVE uses no timers at all.

## Limits and open points

- Forge faces get BLE data only while BLE is on. forge_emulator issue #3 reports that BLE may have to be started by hand from the home face. Re-check on Beta 2.
- Android may cache the GATT table of a bonded watch, hiding a Forge service that appeared after a firmware update. Toggle Bluetooth or re-pair if the watch row never shows the watchface icon.
- The TX UUID is inferred, like RX. If it is wrong, the face is never detected: the icon stays gray and the log says "No usable Forge TX characteristic". Check it with nRF Connect (see TESTING.md).
