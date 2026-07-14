#!/usr/bin/env bash
set -euo pipefail

git config core.hooksPath .githooks

echo "Configured hooks path:"
git config core.hooksPath
