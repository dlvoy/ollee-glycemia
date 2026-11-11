# 19: Forge face backlight and version tracking

## Problem
- On the glycemia face the top-left **L** button does nothing. The Forge API hands L to the face, so the stock "L lights the display" habit is lost while the face is on screen, and the user can't read the watch in the dark.
- The face has three version numbers that nothing ties together: `FACE_VERSION` in `glycemia.js` (sent in the handshake), the major.minor version typed in Forge Apps when uploading, and the app version the face needs. They drift unless written down.
- Release pages only carry the APK. To install the face the user has to find `watchface/glycemia.js` in the repo, at a commit that matches their app.

## Goal
A short press of L lights the watch for a few seconds on the glycemia face. Every release ships the matching face file, and one table maps face versions to Forge Apps versions and the minimum app version.

## Scope
- Watchface:
  - subscribe to `E_L_SHORT`; `l_short()` calls `Ollee.led_on()` and schedules `Ollee.led_off()` after 3 s. A press while lit restarts the 3 s
  - if no timer slot is free, turn the LED off at once (never leave it on)
  - `exit()` cancels the timer and turns the LED off (timers are cancelled after `exit()`)
  - `FACE_VERSION` 2 to 3. The protocol doesn't change, only the number in the hello
- Tests: Node harness gets `led_on`/`led_off` and foreground button delivery; cover light on, off after 3 s, restart on a second press, full timer slots, `exit()` while lit, and that the flash timer still works next to it
- `watchface/VERSIONS.md`: table `FACE_VERSION` | Forge Apps version | minimum app version. Forge Apps version is `0.<FACE_VERSION>` until an official 1.0 face. Rows: 1 = 0.1 = 1.7.0, 2 = 0.2 = 2.0.0, 3 = 0.3 = 2.0.0 (the app needs no change for the light; the handshake needs 2.0.0)
- Release workflow: attach `watchface/glycemia.js` to the GitHub release, keeping the file name (Forge Apps takes the app name from it)
- `CLAUDE.md`: rule that any change to `watchface/glycemia.js` bumps `FACE_VERSION`, adds a `VERSIONS.md` row and updates the hello in the shared fixtures
- Docs: `watchface/README.md` (L button), `docs/watchface/PROTOCOL.md` and `TESTING.md` (hello now `H1,gly,3`), CHANGELOG

## Out of scope
- Long press of L, or the A button
- A configurable light duration
- App-side changes. The app ignores the face version except in logs

## Acceptance criteria
- [ ] In forge_emulator, L on the glycemia face turns the light icon on and it goes off after 3 s; a second press within 3 s keeps it on 3 s from that press
- [ ] Leaving the face (M) while lit turns the light off
- [ ] The hello is `H1,gly,3`; the face stays at most 8158 bytes; `node --test "watchface/test/*.test.js"` and `gradlew testDebugUnitTest` pass
- [ ] A tag push produces a release with both the APK and `glycemia.js`
- [ ] `watchface/VERSIONS.md` exists with rows 1-3, and CLAUDE.md asks for it to stay in sync
- [ ] Checked on a real watch that `led_on()` is the display backlight

## Open questions
- The docs call it "the LED". The emulator draws it as the light icon, but it's unconfirmed that it is the LCD backlight on the watch
- Does the stock L backlight still work while a Forge face is on screen? If so, this duplicates it, which is harmless
