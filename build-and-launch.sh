#!/bin/bash

export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export GRADLE_OPTS="-Dorg.gradle.java.home=$JAVA_HOME"

bash ./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

bash ./launch-app.sh
