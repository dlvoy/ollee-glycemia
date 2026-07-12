---
name: changelog-version
description: Increment semantic version and update CHANGELOG
---

This skill provides commands to bump semantic versions automatically:
- `/changelog-patch` - Increment patch version (1.2.0 → 1.2.1)
- `/changelog-minor` - Increment minor version (1.2.0 → 1.3.0)
- `/changelog-major` - Increment major version (1.2.0 → 2.0.0)

Each command:
1. Reads current version from `app/build.gradle.kts`
2. Increments the appropriate semver field
3. Updates version in gradle file
4. Prepares CHANGELOG.md with new version section (dated today)
5. Adds tag link to CHANGELOG.md (for manual tagging with `git tag`)
6. Does NOT create a commit or tag (you handle git manually)

Usage:
```
/changelog-patch
# or
/changelog-minor
# or
/changelog-major
```

Then describe the changes that went into this release, and I'll update the CHANGELOG with your notes.
