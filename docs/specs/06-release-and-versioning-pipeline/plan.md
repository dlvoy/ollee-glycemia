# 06: Release and versioning pipeline (plan)

## Approach
The tag is the source of truth for releases: CI passes it as `-PappVersionName`. The default in `app/build.gradle.kts` only matters for local builds and is kept in sync by `bump-version.sh`. The changelog is written by hand (helped by the bump skills) and sliced per version by `extract-changelog.sh` for the GitHub Release body.

## Files
- `.github/workflows/build.yml`, `release.yml`, `verify-authors.yml`
- `app/build.gradle.kts`: `appVersionName`, `versionCodeFromSemver`, signing config
- `CHANGELOG.md`, `RELEASING.md`, `BUILDING.md`
- `scripts/bump-version.sh`, `scripts/extract-changelog.sh`, `scripts/setup-git-hooks.sh`, `scripts/setup-gradle-env.sh`
- `.claude/skills/changelog-*`
- `.githooks/commit-msg`
- `build-and-launch.sh`, `build-and-select.sh`, `build-if-changed.sh`, `launch-app.sh`, `select-device.sh`

## Steps
1. APK naming, build workflow refactor, release workflow
2. CHANGELOG.md
3. Build scripts and JAVA_HOME/GRADLE_OPTS handling, Java 21
4. Bump script and changelog skills, tag links
5. Changelog extraction into release notes
6. noreply-email git hook and CI check

## Tests
Manual: tag a release and check the GitHub Release page.
