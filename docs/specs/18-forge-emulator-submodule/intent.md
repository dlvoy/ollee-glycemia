# 18: forge_emulator as a git submodule

## Problem
`scripts/forge/Start-ForgeTest.ps1` clones https://github.com/olleewatch/forge_emulator into the gitignored `.forge-test/` and fast-forwards it to upstream `main` on every run. So:
- Two developers, or one developer on two days, test against different emulator versions, and nothing records which one a test run used.
- An upstream change can break the bridge snippet or invalidate the watchface harness without notice. Beta 2 moved fast: eleven commits in the week after spec 15 was written.
- The tests never look at the emulator, so they can't catch that drift.

## Goal
forge_emulator lives in the repo as a submodule at `tools/forge_emulator`, pinned to a known commit. The scripts and docs use it, and a test checks the bridge and the harness against the pinned version.

## Scope
- Submodule `tools/forge_emulator`, tracking `main`, pinned to `0857ea7` (tag `Fall_Q4_2026_Beta`, Beta 2).
- `scripts/forge`:
  - `_Common.ps1`: the emulator path and a helper that initializes the submodule when it is missing
  - `Start-ForgeTest.ps1`: serve the submodule instead of cloning, print the pinned commit, warn when the checkout differs from the pin
  - `Invoke-ForgeTests.ps1`: report whether the submodule is there
  - `.forge-test/` stays, for the web server pid and logs only
- New Node test `watchface/test/emulator-compat.test.js`, skipped when the submodule is not initialized. It checks against the pinned emulator:
  - the Bluetooth panel element ids that `tools/forge-bridge/forge-bridge.js` uses
  - the `E_*` event bits and background bits in `watchface/test/harness.js`
  - the script byte limit, against the real size of `watchface/glycemia.js`
- Docs: `docs/watchface/TESTING.md`, `tools/forge-bridge/README.md`, the Forge sections of `README.md` and `watchface/README.md`. They also drop "Beta 1, emulator only", since the pin is Beta 2.
- Remove the old local clone `.forge-test/forge_emulator`.

## Out of scope
- CI. It only builds the app, which doesn't need the submodule, and running tests in CI is still its own spec.
- Automatic updates of the pin. Bumping it is a deliberate commit (`git submodule update --remote tools/forge_emulator`), described in TESTING.md.
- Running `glycemia.js` inside the real emulator code from Node.

## Acceptance criteria
- [ ] `git submodule status` shows `tools/forge_emulator` at `0857ea7`.
- [ ] On a fresh clone without `--recurse-submodules`, `Start-ForgeTest.ps1` initializes the submodule and serves it.
- [ ] No script clones or pulls into `.forge-test/` any more.
- [ ] `emulator-compat.test.js` passes against the pin and skips with a clear message when the submodule is missing.
- [ ] `Invoke-ForgeTests.ps1` passes.
- [ ] The docs describe the submodule, how to initialize it and how to bump it.
