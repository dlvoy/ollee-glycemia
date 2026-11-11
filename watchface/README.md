# Glycemia watchface

`glycemia.js` is a watchface for the [Ollee Forge beta](https://github.com/olleewatch/forge_emulator). It shows what the app also sends as the screensaver text, plus how old the reading is, and says so when the reading is outdated or the app link is lost.

| Field | Shows |
|---|---|
| Main | the 6-character tag, for example `120 -5` |
| Day | minutes since the measurement (0 to 39), or minutes since the link was lost, or `--` |
| LAP | the app is feeding live data |
| PM | the reading is outdated (10.5 minutes or more) |
| flash on Main | `no bLE` (link lost), `no APP` (app silent for 11 min), ` old  ` (reading outdated) |

The face also answers the app's handshake query `Q` with the notification `H1,gly,<FACE_VERSION>` (and says hello from `init()` when a phone is connected), so the app can show that the face is installed. Bump `FACE_VERSION` when a change matters to the app.

Frame format and behaviour table: [docs/watchface/PROTOCOL.md](../docs/watchface/PROTOCOL.md).

## Status

Forge Beta 2 uploads faces to the watch through the Ollee Watch app (Settings > Forge Apps), see upstream's [Deploying to your watch](https://github.com/olleewatch/forge_emulator/blob/main/docs/guides/deploying-to-watch.md). This face hasn't been run on a real watch yet. Locally it runs in forge_emulator (git submodule `tools/forge_emulator`), fed by the app through the debug relay. See [docs/watchface/TESTING.md](../docs/watchface/TESTING.md).

## Files

- `glycemia.js`: the face, one plain file (limit 8158 bytes), Espruino-safe ES5
- `test/`: Node tests (`node --test "watchface/test/*.test.js"`) with a small fake Forge runtime in `test/harness.js`
- `test/fixtures/frames.json`: protocol vectors shared with the Kotlin tests
- `test/emulator-compat.test.js`: checks the harness and the bridge against the pinned forge_emulator submodule (skipped when it isn't checked out)

## Writing rules this file follows

- hooks are top-level `function` declarations, with no top-level calls (Espruino doesn't hoist)
- every hook catches its own errors, because an uncaught error disables the face
- no `Map`, `Set`, `Promise`, arrow functions or `let`/`const`; timers only through `Ollee.setTimeout`
- time is computed from `new Date().getTime()`, never by counting ticks
