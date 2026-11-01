# 14: Unify the /commit skill with the project release scripts

## Problem
Two overlapping ways exist to record changes and cut versions:
- The global `/commit` skill (Conventional Commits, Keep a Changelog entries under `[Unreleased]`, `as patch|minor|major|X.Y.Z` to bump and tag)
- The project's `/changelog-{patch,minor,major}` skills, backed by `scripts/bump-version.sh`

They don't fit together:
1. `/commit` reads versions only from `Cargo.toml`, `package.json` or `pyproject.toml`. Here the version lives in `app/build.gradle.kts` (`val appVersionName ... ?: "X.Y.Z"`), so `/commit` falls back to the latest tag and never updates the gradle default.
2. `bump-version.sh` inserts a placeholder section (`(Add your changes here)`) and a tag link. `/commit as X` writes real entries under `[Unreleased]` and renames that header. Using both duplicates sections.
3. Styles differ: `/commit` wants plain sentence bullets ending in a period; this CHANGELOG uses a bold item name plus sub-bullets.
4. `/commit` does not add the `[X.Y.Z]: https://github.com/dlvoy/ollee-glycemia/releases/tag/vX.Y.Z` reference link this file keeps for every release.
5. `/commit` never pushes. The release here only happens on tag push (`release.yml`), and the CI version comes from the tag, not from gradle.
6. `scripts/extract-changelog.sh` reads from the version header to the next `## [` header. For the oldest section (1.0.0) there is no next header, so the reference links at the bottom leak into the release notes.

## Goal
One release path: `/commit` for every change, `/commit as <level>` (then `/commit-tag-push` or a manual push) to release. The gradle default version, CHANGELOG section, tag link and tag all stay consistent.

## Scope
- Teach `/commit` and `/commit-tag-push` this repo's version location and changelog conventions through the project `CLAUDE.md`, without changing the global skills
- Reduce `bump-version.sh` to the version-only and link operations those skills need
- Retire or thin the project `/changelog-*` skills
- Update `RELEASING.md` and `.claude/skills/README.md`

## Out of scope
- Changing the CI release workflow
- Rewriting old CHANGELOG entries to a new style

## Acceptance criteria
- [ ] `/commit` on a feature adds a CHANGELOG entry under `[Unreleased]` in house style
- [ ] `/commit as minor` sets `appVersionName` in gradle, cuts the section, adds the tag link and creates the tag
- [ ] `scripts/extract-changelog.sh <new version>` prints the new section
- [ ] No duplicate or placeholder sections remain
- [ ] Docs describe a single release path

## Open questions
- Should house-style rules live in the global skill (a "match the existing entry style" rule) or in a repo file the skill reads, e.g. `.claude/release.md`?
- Keep `/changelog-*` as aliases or delete them?
