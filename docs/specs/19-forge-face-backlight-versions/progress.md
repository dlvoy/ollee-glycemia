# 19: Forge face backlight and version tracking (progress)

**Status:** in-progress
**Release:** v2.1.0

## Checklist
- [x] Intent and plan written
- [x] Harness and tests
- [x] Face change, `FACE_VERSION` 3, fixture hello
- [x] `VERSIONS.md`, README, docs
- [x] Release workflow attaches `glycemia.js`
- [x] CLAUDE.md rule, CHANGELOG, spec index
- [ ] forge_emulator check: L lights, off after 3 s, M while lit turns it off
- [ ] First tag release shows `glycemia.js` next to the APK
- [ ] Real watch: `led_on()` is the display backlight

## Log
- 2026-11-11: the user asked to implement L-as-backlight, attach the face to releases and add the version table; intent, plan and code done in the same session. Node 40 green, Gradle 191 green, face 6079 of 8158 bytes. Not committed yet
- 2026-11-11: released in v2.1.0
