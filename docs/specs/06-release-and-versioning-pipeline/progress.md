# 06: Release and versioning pipeline (progress)

**Status:** done
**Release:** v1.0.0 to v1.4.0

## Checklist
- [x] Build and release workflows
- [x] CHANGELOG.md
- [x] Build scripts, Java 21
- [x] Bump script and skills
- [x] Changelog extraction
- [x] noreply enforcement

## Log
- 2026-07-10: build/launch scripts (`d0e1b0f`, `93712a2`), APK naming (`9a335f7`)
- 2026-07-11: release workflow and versioning (`086c114`)
- 2026-07-11: CHANGELOG.md added (`392098f`), tagged `v1.0.0` (old commit `dd47d3e`)
- 2026-07-11: JAVA_HOME and GRADLE_OPTS in build scripts (`c652a58`)
- 2026-07-12: OpenJDK 21 configuration (`9bbdbec`)
- 2026-07-12: bump script and changelog skills (`88b0000`, `d7dab01`, `c43c556`, `b600085`, `4e9cde2`), released in `v1.2.1` (`12dd635`)
- 2026-07-12: automatic release tag links, released in `v1.2.2` (`16f8fd8`)
- 2026-07-12: JAVA_HOME handling in VSCode settings (`1c0d447`), released in `v1.3.0`
- 2026-07-14: changelog extraction into the release workflow (`001f998`, fixed in `30a2f6a`)
- 2026-07-14: noreply git hooks and verify-authors workflow (`1f5b7ca`, `6c55adc`), released in `v1.4.0`

Tag mapping after the history rewrite (tag → equivalent `master` commit): `v1.0.0` → `392098f`, `v1.1.0` → `285f871`, `v1.1.1` → `e1b8d3f`, `v1.2.0` → `f9a9c5a`, `v1.2.1` → `12dd635`, `v1.2.2` → `16f8fd8`, `v1.3.0` → `eb16ae7`.
