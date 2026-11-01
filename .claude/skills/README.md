# Claude Code Skills for OlleeGlycemia

This repo has no project skills of its own for releases. Use the global ones:

- `/commit`: Conventional Commit plus a CHANGELOG entry under `[Unreleased]`. Never pushes.
- `/commit as patch|minor|major|X.Y.Z`: the same, and also cuts the release: sets the gradle version, turns `[Unreleased]` into the version section, adds the tag link and creates the tag locally.
- `/commit-tag-push <level>`: cuts the release and pushes the commits and the tag.

The global skills stay generic. The repo-specific rules (version location, changelog style, links) are in the "Releases" section of the root `CLAUDE.md`, which is in context whenever the skills run. The helpers are `scripts/bump-version.sh` (`--current`, `--release`, `--set`, `--link`) and `scripts/extract-changelog.sh`.

The old `/changelog-patch|minor|major` skills were removed in spec 14; use `/commit as <level>` instead.
