# 14: Unify the /commit skill with the project release scripts (progress)

**Status:** in-progress
**Release:** –

## Checklist
- [x] CLAUDE.md "Releases" section (global skills left unchanged)
- [x] Script `--current` / `--set` / `--link` / `--release`; bump keywords and placeholder sections dropped
- [x] Project skills retired (deleted; `.claude/skills/README.md` points to `/commit`)
- [x] `.claude/settings.json` allow-list: `bump-version.sh *` and `extract-changelog.sh *` replace the `patch|minor|major` entries
- [x] RELEASING.md
- [x] extract-changelog.sh link leak fix
- [x] Dry run, scripts only: `--set`, `--link` (idempotent), `extract-changelog.sh` on a throwaway worktree
- [ ] Dry run, full `/commit as patch` on a throwaway branch

## Log
- 2026-11-01: intent and plan written (from the CHANGELOG audit in spec 13)
- 2026-11-01: `/changelog-*` deleted, not aliased
- 2026-11-01: plan changed: global skills stay untouched; rules moved from a draft `.claude/release.md` into CLAUDE.md; added `--release`
