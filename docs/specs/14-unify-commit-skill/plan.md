# 14: Unify the /commit skill with the project release scripts (plan)

## Approach
The global skills stay untouched. The project `CLAUDE.md` is in context whenever they run, so a short "Releases" section there supplies what they lack: where the version lives, the script to run on a release cut, and the entry style. `CLAUDE.md` instructions override default skill behaviour.

_Changed 2026-11-01: the first draft edited the global skills and added `.claude/release.md`; the user chose to keep the global skills unchanged._

## Files
- `CLAUDE.md`: "Releases" section (version location, `--release` command, entry style)
- `scripts/bump-version.sh`
- `.claude/skills/changelog-*`, `.claude/skills/README.md`, `.claude/skills/changelog-version.md`
- `RELEASING.md`, `CHANGELOG.md` (keeps its `[Unreleased]` section)

## Steps
1. **CLAUDE.md**: add the "Releases" section.
2. _(dropped: global skill changes)_
3. **Script**: `bump-version.sh --current`, `--set X.Y.Z` (gradle only), `--link X.Y.Z` (reference links), `--release X.Y.Z` (both). Drop the bump keywords and placeholder sections.
4. _(dropped: `.claude/release.md`; its content went into CLAUDE.md)_
5. **Project skills**: replace `/changelog-*` with one-line pointers to `/commit as <level>`, or delete them. Update `.claude/skills/README.md`.
6. **RELEASING.md**: describe the single flow: `/commit as minor` → review → `/commit-tag-push` (or `git push --follow-tags`).
7. **extract-changelog.sh**: also end the section at the first reference-link line (`^\[[^]]+\]: `), so the oldest section's notes don't include the links.
8. **Dry run** on a throwaway branch: feature commit, then `/commit as patch`; check gradle, CHANGELOG, link, tag, `extract-changelog.sh`. Delete the branch and tag afterwards.

## Tests
- Dry run from step 8
- `bash scripts/extract-changelog.sh X.Y.Z`
