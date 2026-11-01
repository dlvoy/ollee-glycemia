#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "Usage: extract-changelog.sh <version>"
  echo "Example: extract-changelog.sh 1.4.0"
  exit 1
fi

VERSION="${1#v}"
CHANGELOG_FILE="CHANGELOG.md"

if [ ! -f "$CHANGELOG_FILE" ]; then
  echo "Error: $CHANGELOG_FILE not found"
  exit 1
fi

# Print the section body: after the version header, up to the next "## [" header
# or the reference-link block ("[x]: url"), whichever comes first.
# Leading and trailing blank lines are dropped.
awk -v ver="$VERSION" '
  index($0, "## [" ver "]") == 1 { on=1; next }
  on && (/^## \[/ || /^\[[^]]+\]: /) { exit }
  on { lines[++n]=$0 }
  END {
    s=1; while (s<=n && lines[s] ~ /^[[:space:]]*$/) s++
    e=n; while (e>=s && lines[e] ~ /^[[:space:]]*$/) e--
    for (i=s; i<=e; i++) print lines[i]
  }
' "$CHANGELOG_FILE"
