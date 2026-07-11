#!/bin/bash

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export GRADLE_OPTS="-Dorg.gradle.java.home=$JAVA_HOME"

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
