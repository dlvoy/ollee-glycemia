#!/bin/bash
# Version helper for OlleeGlycemia, run during /commit releases (see CLAUDE.md).
#
# Usage:
#   bump-version.sh --current        print the default appVersionName from gradle
#   bump-version.sh --release X.Y.Z  --set and --link in one go
#   bump-version.sh --set X.Y.Z      set the default appVersionName in gradle
#   bump-version.sh --link X.Y.Z     add the [X.Y.Z] release link to CHANGELOG.md
#                                    and repoint [Unreleased] at vX.Y.Z
#
# Changelog sections are written by /commit, not by this script.

set -e

GRADLE_FILE="app/build.gradle.kts"
CHANGELOG_FILE="CHANGELOG.md"
REPO_URL="https://github.com/dlvoy/ollee-glycemia"

usage() {
    sed -n '4,10p' "$0" | sed 's/^# \{0,1\}//'
    exit 1
}

require_semver() {
    if ! [[ "$1" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        echo "Error: '$1' is not X.Y.Z" >&2
        exit 1
    fi
}

current_version() {
    grep "val appVersionName:" "$GRADLE_FILE" | awk -F'"' '{print $(NF-1)}'
}

[ -f "$GRADLE_FILE" ] || { echo "Error: $GRADLE_FILE not found (run from repo root)" >&2; exit 1; }

case "$1" in
    --current)
        current_version
        ;;
    --release)
        bash "$0" --set "$2"
        bash "$0" --link "$2"
        ;;
    --set)
        NEW="${2#v}"
        require_semver "$NEW"
        OLD=$(current_version)
        [ -n "$OLD" ] || { echo "Error: appVersionName not found in $GRADLE_FILE" >&2; exit 1; }
        TMP="$GRADLE_FILE.tmp.$$"
        awk -v new="$NEW" '
            /val appVersionName:/ && !done { sub(/"[^"]*"[[:space:]]*$/, "\"" new "\""); done=1 }
            { print }
        ' "$GRADLE_FILE" > "$TMP" && mv "$TMP" "$GRADLE_FILE"
        echo "$OLD -> $(current_version)"
        ;;
    --link)
        NEW="${2#v}"
        require_semver "$NEW"
        [ -f "$CHANGELOG_FILE" ] || { echo "Error: $CHANGELOG_FILE not found" >&2; exit 1; }
        if grep -q "^\[$NEW\]: " "$CHANGELOG_FILE"; then
            echo "Link for $NEW already present"
            exit 0
        fi
        TMP="$CHANGELOG_FILE.tmp.$$"
        awk -v new="$NEW" -v url="$REPO_URL" '
            /^\[Unreleased\]: / {
                if (added) next
                print "[Unreleased]: " url "/compare/v" new "...HEAD"
                print "[" new "]: " url "/releases/tag/v" new
                added=1; next
            }
            /^\[[^]]+\]: https:\/\// && !added {
                print "[Unreleased]: " url "/compare/v" new "...HEAD"
                print "[" new "]: " url "/releases/tag/v" new
                added=1
            }
            { print }
            END {
                if (!added) {
                    print ""
                    print "[Unreleased]: " url "/compare/v" new "...HEAD"
                    print "[" new "]: " url "/releases/tag/v" new
                }
            }
        ' "$CHANGELOG_FILE" > "$TMP" && mv "$TMP" "$CHANGELOG_FILE"
        echo "Added [$NEW] link, [Unreleased] now compares from v$NEW"
        ;;
    *)
        usage
        ;;
esac
