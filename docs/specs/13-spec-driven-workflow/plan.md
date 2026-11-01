# 13: Spec-driven workflow (plan)

## Approach
Group the history into specs by feature milestone rather than one per release. Each spec maps to the release(s) that shipped it. Commit hashes refer to the current `master` history, with a mapping for the pre-rewrite tags.

## Files
- `docs/specs/**`
- `CLAUDE.md`
- `CHANGELOG.md`

## Steps
1. Sync local `master` with `origin/master` (it was a stale pre-rewrite copy)
2. Write the README and templates
3. Write specs 00–12
4. Audit the CHANGELOG: missing items, inaccuracies, `[Unreleased]` section
5. Write spec 14
6. CLAUDE.md section

## Tests
- `bash scripts/extract-changelog.sh <version>` still prints the right section for each version
