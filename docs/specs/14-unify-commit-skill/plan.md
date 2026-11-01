# 14: Unify the /commit skill with the project release scripts (plan)

## Approach
Keep the global skill generic and let the repo describe its specifics. The global skill gains (a) a Gradle row in its manifest table and (b) a generic hook: if the repo has a release-conventions file, follow it. This repo provides that file and a slimmed-down script.

## Files
- `~/.claude/skills/commit/SKILL.md`, `~/.claude/skills/commit-tag-push/SKILL.md` (global, outside the repo)
- `.claude/release.md` (new): version location, version command, changelog style, tag link format
- `scripts/bump-version.sh`
- `.claude/skills/changelog-*`, `.claude/skills/README.md`, `.claude/skills/changelog-version.md`
- `RELEASING.md`, `CHANGELOG.md` (keeps its `[Unreleased]` section)

## Steps
1. **Global skill, manifest detection**: add `app/build.gradle.kts` (`appVersionName`) to the "Project manifest" block and the "Writing the version" table.
2. **Global skill, repo override**: if `.claude/release.md` exists, read it and let it override the version command, entry style and link handling. Otherwise: "match the style of existing entries" and "if the file keeps reference links, add one for the new version".
3. **Script**: add `bump-version.sh --set X.Y.Z` (gradle only, prints old → new) and `--link X.Y.Z` (adds the reference link). Drop the placeholder-section writing, or keep it behind a `--legacy` flag.
4. **`.claude/release.md`**: document `bash scripts/bump-version.sh --set X.Y.Z` and `--link X.Y.Z`, the bold-name + sub-bullet style, and that the CI version comes from the tag.
5. **Project skills**: replace `/changelog-*` with one-line pointers to `/commit as <level>`, or delete them. Update `.claude/skills/README.md`.
6. **RELEASING.md**: describe the single flow: `/commit as minor` → review → `/commit-tag-push` (or `git push --follow-tags`).
7. **extract-changelog.sh**: also end the section at the first reference-link line (`^\[[^]]+\]: `), so the oldest section's notes don't include the links.
8. **Dry run** on a throwaway branch: feature commit, then `/commit as patch`; check gradle, CHANGELOG, link, tag, `extract-changelog.sh`. Delete the branch and tag afterwards.

## Tests
- Dry run from step 8
- `bash scripts/extract-changelog.sh X.Y.Z`
