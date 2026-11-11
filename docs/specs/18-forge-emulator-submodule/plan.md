# 18: forge_emulator as a git submodule (plan)

## Approach
- `git submodule add -b main https://github.com/olleewatch/forge_emulator tools/forge_emulator`, then check out `0857ea7` in it and stage the gitlink. `.gitmodules` records `branch = main`, so a later `git submodule update --remote` moves the pin forward on purpose.
- Not shallow. The repo is small, and a shallow submodule can fail to fetch a pinned commit once upstream moves on.
- forge_emulator has no LICENSE file. A submodule is only a pointer to the upstream repo, so this repo doesn't redistribute its code.
- The emulator stays untrusted input. The web server keeps running Python with `-I` from that folder, and nothing in it is executed outside the browser.
- `.forge-test/` keeps the web server pid and logs. The old clone in it is deleted.

## Files
- `.gitmodules` (new), gitlink `tools/forge_emulator`
- `scripts/forge/_Common.ps1`: `$script:EmulatorDir`, `Initialize-ForgeEmulator` (runs `git submodule update --init tools/forge_emulator` when `index.html` is missing, returns `git submodule status` info)
- `scripts/forge/Start-ForgeTest.ps1`: step 5 uses the helper; header text
- `scripts/forge/Invoke-ForgeTests.ps1`: submodule status line before the Node tests
- `watchface/test/harness.js`: export `E` and `BACKGROUND_BITS`
- `watchface/test/emulator-compat.test.js` (new)
- `.gitignore`: comment for `/.forge-test/`
- Docs: `docs/watchface/TESTING.md`, `tools/forge-bridge/README.md`, `README.md`, `watchface/README.md`

## Steps
1. Add the submodule at the pin, delete `.forge-test/forge_emulator`.
2. Script changes.
3. Harness exports and the compat test.
4. Docs, CHANGELOG entry.
5. Run `Invoke-ForgeTests.ps1` and a `Start-ForgeTest.ps1 -SkipBuild -NoBrowser` smoke run up to the web server, then `Stop-ForgeTest.ps1`.

## Tests
- `emulator-compat.test.js`:
  - `index.html` has `id="ble-input"`, `id="ble-send-btn"`, `id="ble-connected"`
  - every `const E_X = 1 << n;` in `js/ollee.js` that the harness also defines has the same value, and `FORGE_BG_EVENTS` equals the harness background bits
  - `FORGE_SCRIPT_BYTE_LIMIT` in `js/app.js` is at least the byte size of `glycemia.js`
  - all of it skipped when `tools/forge_emulator/index.html` is missing
- PowerShell parse check (already in `Invoke-ForgeTests.ps1`).
- Manual: the smoke run in step 5.
