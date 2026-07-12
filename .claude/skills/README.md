# Claude Code Skills for OlleeGlycemia

## Version Bumping

Three skills for semantic versioning:

### `/changelog-patch`
Bump patch version: 1.2.0 → 1.2.1
```bash
/changelog-patch
```

### `/changelog-minor`
Bump minor version: 1.2.0 → 1.3.0
```bash
/changelog-minor
```

### `/changelog-major`
Bump major version: 1.2.0 → 2.0.0
```bash
/changelog-major
```

## What Each Skill Does

1. Reads current version from `app/build.gradle.kts`
2. Increments the appropriate semver field
3. Updates the version in gradle file
4. Adds new section to `CHANGELOG.md` with today's date and empty change placeholders
5. **Does NOT commit** — you review and fill in changelog before committing

## Usage Workflow

```bash
# Bump version
/changelog-minor

# Edit CHANGELOG.md to add your changes
# Then commit
git commit -m "chore: Release v1.3.0"
git tag v1.3.0
```

## Implementation

- **Core script**: `scripts/bump-version.sh` — Handles version parsing and file updates
- **Skills**: `.claude/skills/changelog-{patch,minor,major}.sh` — Call the core script
