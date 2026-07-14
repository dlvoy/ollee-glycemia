#!/bin/bash
set -e

if [ -z "$1" ]; then
  echo "Usage: extract-changelog.sh <version>"
  echo "Example: extract-changelog.sh 1.4.0"
  exit 1
fi

VERSION="$1"
CHANGELOG_FILE="CHANGELOG.md"

if [ ! -f "$CHANGELOG_FILE" ]; then
  echo "Error: $CHANGELOG_FILE not found"
  exit 1
fi

# Extract section for the version (from version header to next version header)
sed -n "/^## \[${VERSION}\]/,/^## \[/p" "$CHANGELOG_FILE" | sed '1d;$d' | sed -e :a -e '/^\n*$/{$d;N;ba' -e '}'
