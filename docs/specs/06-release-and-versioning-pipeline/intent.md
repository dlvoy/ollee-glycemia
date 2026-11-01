# 06: Release and versioning pipeline

## Problem
APKs were built by hand or as anonymous CI artifacts, with no version history, no release notes and no consistent author identity in commits.

## Goal
Tag-driven releases with versioned, signed APKs, a maintained changelog that becomes the release notes, and tooling that keeps versions and commit metadata consistent.

## Scope
- `build.yml`: APK build on pushes and PRs to `main`/`master`
- `release.yml`: on a `v*.*.*` tag, build a signed (or unsigned) APK named `ollee-glycemia-X.Y.Z-release.apk` and publish a GitHub Release
- Version from the tag (`-PappVersionName`), with `versionCode` derived from semver in `app/build.gradle.kts`
- `CHANGELOG.md` in Keep a Changelog format
- `scripts/bump-version.sh` and the `/changelog-{patch,minor,major}` project skills
- Release tag links added to CHANGELOG by the bump script
- `scripts/extract-changelog.sh`: release notes taken from the matching CHANGELOG section
- `.githooks/commit-msg` + `scripts/setup-git-hooks.sh` and the `verify-authors.yml` workflow, enforcing GitHub noreply emails
- Build/launch helper scripts, `scripts/setup-gradle-env.sh`, Java 21 detection, `BUILDING.md`, `RELEASING.md`

## Out of scope
- Unifying with the global `/commit` skill (spec 14)

## Acceptance criteria
- [x] Pushing a version tag produces a GitHub Release with the APK and the CHANGELOG section as notes
- [x] The version shown in the app matches the tag
- [x] Commits with a non-noreply author email are rejected locally and flagged in CI

## Known issue
Rewriting history to noreply emails (around v1.4.0) left tags `v1.0.0`–`v1.3.0` pointing at the old, now unreachable commits. The equivalent commits on `master` are listed in progress.md. Moving those tags would rewrite published releases, so it was left alone.
