# 19: Forge face backlight and version tracking (plan)

## Approach
- `glycemia.js`: `E_L_SHORT` in the top-level `subscribe` (foreground only; buttons never reach a face off screen). `LIGHT_MS = 3000`, `lightId = -1`. `l_short()` clears a running light timer, calls `led_on()`, then `setTimeout(lightOff, LIGHT_MS)`; on `-1` it calls `led_off()` at once. `lightOff()` resets `lightId` and calls `led_off()`. `exit()` cancels the timer and calls `led_off()`. Two of the four timer slots are used at most (flash and light)
- Short press, not release: the light should come on as the button goes down, like the stock watch
- `FACE_VERSION = 3`, fixture hello `H1,gly,3`. The `current face` parse vector stays a valid example
- Release: one more line in `softprops/action-gh-release` `files`. The file keeps the name `glycemia.js`, so the Forge Apps app name stays "glycemia"
- Version table in `watchface/VERSIONS.md`, linked from `watchface/README.md`

## Files
- `watchface/glycemia.js`: the light, `FACE_VERSION`
- `watchface/test/harness.js`: `led_on`/`led_off` with `screen.led`, `E_L_SHORT` in `HOOK_BIT`, `press(name)`
- `watchface/test/glycemia.test.js`: light tests
- `watchface/test/fixtures/frames.json`: hello
- `watchface/VERSIONS.md` (new), `watchface/README.md`
- `.github/workflows/release.yml`: attach the face
- `docs/watchface/PROTOCOL.md`, `docs/watchface/TESTING.md`: hello value, L check in the emulator table
- `CLAUDE.md`, `CHANGELOG.md`, `docs/specs/README.md`

## Steps
1. Harness and tests
2. Face change and version bump, fixture
3. `VERSIONS.md`, README, docs
4. Release workflow
5. CLAUDE.md rule, CHANGELOG, spec index

## Tests
- Node: L lights and the light goes off after 3 s; a second press restarts the 3 s; no free timer slot leaves the LED off; `exit()` while lit turns it off; flash word and light together; L off screen does nothing; size limit
- Kotlin: `ForgeProtocolTest` reads the new hello from the fixtures
- Manual: forge_emulator, press `l` on the face; real watch later
