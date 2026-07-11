#!/bin/bash

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export GRADLE_OPTS="-Dorg.gradle.java.home=$JAVA_HOME"

GRADLE_CACHE=".gradle_build_cache"

# Get the most recent modification time of source files
SOURCE_TIMESTAMP=$(find app/src -type f \( -name '*.kt' -o -name '*.xml' -o -name 'build.gradle*' \) -newer /dev/null 2>/dev/null | xargs stat -f '%m' 2>/dev/null | sort -n | tail -1)

if [ -z "$SOURCE_TIMESTAMP" ]; then
    SOURCE_TIMESTAMP=0
fi

# Get the timestamp of the last build
LAST_BUILD=0
if [ -f "$GRADLE_CACHE" ]; then
    LAST_BUILD=$(cat "$GRADLE_CACHE")
fi

# Only build if sources are newer than the cache
if [ "$SOURCE_TIMESTAMP" -gt "$LAST_BUILD" ]; then
    echo "⚙️ Building app (sources changed)..."
    bash ./gradlew assembleDebug
    if [ $? -eq 0 ]; then
        echo "$SOURCE_TIMESTAMP" > "$GRADLE_CACHE"
        echo "✅ Build complete"
    fi
else
    echo "⏭️ Skipping build (no changes since last build)"
fi
