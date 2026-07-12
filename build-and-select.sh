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

echo "✓ Using Java from: $JAVA_HOME"

bash ./gradlew assembleDebug
if [ $? -ne 0 ]; then
    exit 1
fi

echo ""
echo "Available devices:"
adb devices | grep -E 'device|emulator'
echo ""
echo "Recommended device (priority: TCP > real > first):"
bash ./select-device.sh
