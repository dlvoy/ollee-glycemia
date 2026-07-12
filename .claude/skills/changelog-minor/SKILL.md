---
name: changelog-minor
description: Bump minor version (1.2.0 → 1.3.0) and write real CHANGELOG entries for it
---

1. Run from the project root:
   ```bash
   cd "$(git rev-parse --show-toplevel)" && bash scripts/bump-version.sh minor
   ```
2. Find the commits included in this release — everything since the last commit that touched CHANGELOG.md:
   ```bash
   git log $(git log -1 --format=%H -- CHANGELOG.md)..HEAD --oneline
   ```
   Use `git diff` on individual commits if a message is too terse to summarize confidently.
3. Edit the new version section the script just added to CHANGELOG.md, replacing the `(Add your changes here)` / `(Add fixes here)` placeholders with real `### Added` / `### Fixed` (etc.) bullets summarizing those commits — match the style of existing entries (bold feature/fix name, sub-bullets for detail).
4. Do not commit — leave the changes for the user to review.
