#!/bin/bash
# Force Gradle to use Homebrew Java 21 instead of VSCode's bundled Java
# This prevents the jlink executable errors after VSCode restart

export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
export PATH="$JAVA_HOME/bin:$PATH"

echo "✓ Gradle environment configured:"
echo "  JAVA_HOME=$JAVA_HOME"
echo "  Java version: $(java -version 2>&1 | head -1)"
