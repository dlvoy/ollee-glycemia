# Glycemia watchface

`glycemia.js` is a watchface for the [Ollee Forge beta](https://github.com/olleewatch/forge_emulator). It shows what the app also sends as the screensaver text, plus how old the reading is, and says so when the reading is outdated or the app link is lost.

| Field | Shows |
|---|---|
| Main | the 6-character tag, for example `120 -5` |
| Day | minutes since the measurement (0 to 39), or minutes since the link was lost, or `--` |
| LAP | the app is feeding live data |
| PM | the reading is outdated (10.5 minutes or more) |
| flash on Main | `no bLE` (link lost), `no APP` (app silent for 11 min), ` old  ` (reading outdated) |

Frame format and behaviour table: [docs/watchface/PROTOCOL.md](../docs/watchface/PROTOCOL.md).

## Status

Forge Beta 1 only runs faces in the browser emulator. Watch support and the upload flow arrive with Beta 2; until then the face is tried in forge_emulator, fed by the app through the debug relay. See [docs/watchface/TESTING.md](../docs/watchface/TESTING.md).

## Files

- `glycemia.js`: the face, one plain file (limit 8158 bytes), Espruino-safe ES5
- `test/`: Node tests (`node --test "watchface/test/*.test.js"`) with a small fake Forge runtime in `test/harness.js`
- `test/fixtures/frames.json`: protocol vectors shared with the Kotlin tests

## Writing rules this file follows

- hooks are top-level `function` declarations, with no top-level calls (Espruino doesn't hoist)
- every hook catches its own errors, because an uncaught error disables the face
- no `Map`, `Set`, `Promise`, arrow functions or `let`/`const`; timers only through `Ollee.setTimeout`
- time is computed from `new Date().getTime()`, never by counting ticks
