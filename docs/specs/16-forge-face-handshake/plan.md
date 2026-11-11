# 16: Forge face handshake (plan)

## Approach

### Frames
| Frame | Direction | Meaning |
|---|---|---|
| `Q` | phone to watch, RX write | "Which face is listening?" The face looks at the first letter only, so `Q` can grow fields later |
| `H<protocol>,<face id>,<face version>` | watch to phone, TX notification | The reply. Today `H1,gly,2`: protocol 1 (the v1 frame family), face id `gly`, face version 2 |

- Face version 1 is the face shipped in v1.7.0, which has no handshake. The new face is 2. Bump it whenever the app may need to tell faces apart.
- The app accepts an `H` only with id `gly`, a numeric protocol and a numeric face version. Extra fields after the version are allowed (forward compatible). Anything else on TX is ignored.
- The face doesn't count `Q` as app traffic: it doesn't touch `lastRxAt`, the tag or the mode. The `G` frames keep doing that.
- The vectors go in `watchface/test/fixtures/frames.json` under `handshake` (query, hello, parse cases), shared by Node and Kotlin.

### Watchface
- `FACE_ID = 'gly'`, `FACE_VERSION = 2`, and `hello()` calls `Ollee.send('H1,gly,2')`. It is guarded with `typeof Ollee.send === 'function'`, so firmware without `send` doesn't disable the face.
- `ble_rx`: data starting with `Q` calls `hello()` and returns before frame parsing. The background subscription already delivers RX off screen, so the face answers while another face is showing too.
- `init()`: `hello()` when `Ollee.connected()`.

### App
New `ForgeHandshake` (pure, like `ForgeFeed`) holds the per-link state:
- `reset()` on every new link
- `onNotificationsEnabled()` makes a query due
- `requestQuery(force)`: always when forced (sync tapped), else only while the face is not detected
- `queryDue()`, `onQueryQueued()`, `onQueryFailed()`
- `onQuerySent()` returns true when the reply timeout should start, false when the reply already beat the write callback
- `onNotification(bytes)` returns `DETECTED` for a valid hello, null otherwise
- `onReplyTimeout()` returns `NOT_DETECTED` if the written query is still unanswered. The 5 s timer itself is a `Runnable` on `WatchConnection`'s handler (changed during the work, see the progress log)

`WatchConnection`:
- After discovery, it also looks up TX `C6FB0003-…` (NOTIFY property and a CCCD descriptor required). Together with the existing 1 s delayed flush, an ACTIVE watch with RX and TX gets a queued `KEY_FORGE_NOTIFY` write: `setCharacteristicNotification` plus a CCCD write of `ENABLE_NOTIFICATION_VALUE`.
  - The `GattWriteQueue` mechanics stay the same. `issueWrite` dispatches on the key, and the new `onDescriptorWrite` callback completes the write like `onCharacteristicWrite` does.
  - Paused or stopped watches skip the handshake. They hang up as soon as the label is out, so a reply could never arrive.
- Notifications enabled leads to `Q` (`KEY_FORGE_QUERY`, on RX, after the OAP and `G` writes). When the write completes, the 5 s reply timeout starts (a `Runnable` on the main handler).
- Each `G` frame queued while the face is not detected also queues a `Q`. So a face installed later answers within one reading or keepalive, at most 5 minutes.
- `resendToForge()` (sync tapped) forces a query even when the face was detected.
- `onCharacteristicChanged`: the API 33+ overload and the deprecated one (used below 33 only) hop to the main thread and hand TX notifications to `ForgeHandshake`.
- The result is stored with `WatchStore.setFaceDetected`, and the status is republished (like `forgeSupported`). No result (a disconnect before the timeout, or a paused watch) keeps the last known value.
- Forge service gone, or Forge without a usable TX characteristic: stored as not detected, with a log line (this covers a wrong TX UUID guess).
- Disconnect: remove the timeout runnable, reset the handshake, drop `forgeTx`.

## Files
- `watchface/glycemia.js`, `watchface/test/harness.js` (fake `Ollee.send` like the emulator's: false when not connected, empty or over 256 bytes; records sent data), `watchface/test/glycemia.test.js`, `watchface/test/fixtures/frames.json`, `watchface/test/emulator-compat.test.js` (handshake frames fit one BLE write)
- `ForgeProtocol.kt`: `TX_UUID`, `CCCD_UUID`, `QUERY_FRAME`, `FACE_ID`, `Hello`, `parseHello()`
- `ForgeHandshake.kt` (new)
- `WatchConnection.kt`: TX lookup, notify and query writes, notification callbacks, timeout, persistence
- `GattWriteQueue.kt`: doc comment (descriptor writes go through it too)
- Docs: `docs/watchface/PROTOCOL.md` (handshake section, TX row), `docs/watchface/TESTING.md` (emulator and nRF Connect checks), `watchface/README.md`, `README.md`, `CHANGELOG.md`

## Steps
1. Fixtures, `ForgeProtocol` additions and tests.
2. Face changes, harness `send`, Node tests.
3. `ForgeHandshake` and its tests.
4. `WatchConnection` wiring.
5. Docs, CHANGELOG.
6. `Invoke-ForgeTests.ps1`, then the face in forge_emulator: type `Q`, expect `H1,gly,2` in the BLE log.

## Tests
- `ForgeProtocolTest`: the hello parse vectors, the query and hello fixtures, both at most 20 bytes.
- `ForgeHandshakeTest` (plain JUnit):
  - nothing is due before notifications are on
  - query, hello, detected
  - timeout gives not detected, only once
  - a reply before the write callback doesn't start the timeout
  - a failed write makes the query due again
  - keepalive requests are ignored once detected, forced ones aren't
  - a forced query without a reply clears detected
  - foreign and malformed notifications are ignored
  - an unsolicited hello counts
  - `reset()` clears everything
- Node:
  - `Q` gets the hello, on and off screen
  - `Q` leaves the tag and mode alone
  - `init` says hello only when connected
  - firmware without `send` doesn't disable the face
  - loading still makes no `Ollee` calls
  - the size limit still holds
- Manual: forge_emulator (`Q` in the Bluetooth box; Run with "phone connected" ticked shows the hello). A real Beta 2 watch with nRF Connect (enable TX notifications, write `Q`), then the app's watch row turning the icon on. The real-watch part stays open until hardware is at hand.
