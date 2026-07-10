#!/bin/bash

bash ./gradlew assembleDebug
if [ $? -ne 0 ]; then
    echo "❌ Build failed"
    exit 1
fi

bash ./launch-app.sh
