#!/bin/bash
# Semantic version bumper for OlleeGlycemia
# Usage: bump-version.sh major|minor|patch

set -e

BUMP_TYPE="${1:-patch}"
GRADLE_FILE="app/build.gradle.kts"
CHANGELOG_FILE="CHANGELOG.md"

if [ ! -f "$GRADLE_FILE" ]; then
    echo "❌ Error: $GRADLE_FILE not found"
    exit 1
fi

if [ ! -f "$CHANGELOG_FILE" ]; then
    echo "❌ Error: $CHANGELOG_FILE not found"
    exit 1
fi

# Extract current version from gradle file (macOS compatible)
CURRENT_VERSION=$(grep "val appVersionName:" "$GRADLE_FILE" | awk -F'"' '{print $(NF-1)}')

if [ -z "$CURRENT_VERSION" ]; then
    echo "❌ Error: Could not extract version from $GRADLE_FILE"
    exit 1
fi

echo "Current version: $CURRENT_VERSION"

# Parse semver
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"

# Bump version based on type
case "$BUMP_TYPE" in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch)
        PATCH=$((PATCH + 1))
        ;;
    *)
        echo "❌ Error: Invalid bump type '$BUMP_TYPE'. Use: major, minor, or patch"
        exit 1
        ;;
esac

NEW_VERSION="$MAJOR.$MINOR.$PATCH"
TODAY=$(date +%Y-%m-%d)

echo "New version: $NEW_VERSION"

# Update gradle file
sed -i '' "s/val appVersionName.*= .*/val appVersionName: String = (project.findProperty(\"appVersionName\") as String?) ?: \"$NEW_VERSION\"/" "$GRADLE_FILE"

echo "✓ Updated $GRADLE_FILE"

# Add CHANGELOG entry (use scratchpad directory to avoid permission issues)
TEMP_CHANGELOG=".changelog.tmp.$$"

# Copy header and then add new version section
head -n 6 "$CHANGELOG_FILE" > "$TEMP_CHANGELOG"

cat >> "$TEMP_CHANGELOG" << EOF

## [$NEW_VERSION] - $TODAY

### Added

- (Add your changes here)

### Fixed

- (Add fixes here)

EOF

# Add rest of original changelog (skip first 6 lines which are headers)
tail -n +7 "$CHANGELOG_FILE" >> "$TEMP_CHANGELOG"

# Check if link definitions section exists (lines starting with [version]:)
if grep -q "^\[.*\]: https://" "$TEMP_CHANGELOG"; then
    # Insert the new link definition before the first existing link (only once)
    # Use a marker to find the first occurrence and insert before it
    LINE_NUM=$(grep -n "^\[.*\]: https://" "$TEMP_CHANGELOG" | head -1 | cut -d: -f1)
    if [ -n "$LINE_NUM" ]; then
        # Use awk to insert at the specific line
        awk -v line="$LINE_NUM" -v newline="[$NEW_VERSION]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v$NEW_VERSION" \
            'NR == line {print newline} {print}' "$TEMP_CHANGELOG" > "$TEMP_CHANGELOG.tmp" && mv "$TEMP_CHANGELOG.tmp" "$TEMP_CHANGELOG"
    fi
else
    # Add link definition at the end if none exist
    echo "" >> "$TEMP_CHANGELOG"
    echo "[$NEW_VERSION]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v$NEW_VERSION" >> "$TEMP_CHANGELOG"
fi

mv "$TEMP_CHANGELOG" "$CHANGELOG_FILE"

echo "✓ Updated $CHANGELOG_FILE with tag link"
echo ""
echo "✅ Version bumped: $CURRENT_VERSION → $NEW_VERSION"
echo ""
echo "Next steps:"
echo "1. Edit CHANGELOG.md to describe the changes"
echo "2. Review changes: git status"
echo "3. Commit: git commit -m 'chore: Release v$NEW_VERSION'"
echo "4. Tag: git tag v$NEW_VERSION"
