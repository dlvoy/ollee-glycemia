#!/bin/bash

# Detect Java 21 with intelligent fallback
# Tries multiple common installation methods in order
JAVA_HOME="${JAVA_HOME:-}"  # Use existing JAVA_HOME if set

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    # Try macOS system detection first
    JAVA_HOME=$(/usr/libexec/java_home -v 21 2>/dev/null)
fi

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    # Fallback to Homebrew (macOS Apple Silicon)
    [ -d "/opt/homebrew/opt/openjdk@21" ] && JAVA_HOME="/opt/homebrew/opt/openjdk@21"
fi

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    # Fallback to Intel Homebrew path
    [ -d "/usr/local/opt/openjdk@21" ] && JAVA_HOME="/usr/local/opt/openjdk@21"
fi

if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    # Fallback to SDKMAN (common on Linux)
    [ -d "$HOME/.sdkman/candidates/java/21.current" ] && JAVA_HOME="$HOME/.sdkman/candidates/java/21.current"
fi

# Validate Java was found
if [ -z "$JAVA_HOME" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    echo "❌ Java 21 not found"
    echo ""
    echo "To install Java 21:"
    echo "  macOS (Homebrew): brew install openjdk@21"
    echo "  Linux (SDKMAN):   sdk install java 21.current"
    echo "  Or set JAVA_HOME: export JAVA_HOME=/path/to/java"
    exit 1
fi

export JAVA_HOME="$JAVA_HOME"
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
