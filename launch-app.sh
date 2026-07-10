#!/bin/bash

# Get the selected device based on priority (TCP > real > first)
DEVICE=$(bash ./select-device.sh)

if [ -z "$DEVICE" ]; then
    echo "❌ No device found"
    exit 1
fi

echo "🚀 Launching on device: $DEVICE"
echo ""

APK="app/build/outputs/apk/debug/app-debug.apk"

if [ ! -f "$APK" ]; then
    echo "❌ APK not found: $APK"
    exit 1
fi

echo "📦 Installing app..."
adb -s "$DEVICE" install -r "$APK"

if [ $? -ne 0 ]; then
    echo "❌ Installation failed"
    exit 1
fi

echo "✅ App installed"
echo ""
echo "▶️ Launching app..."

# Launch the main activity
adb -s "$DEVICE" shell am start -n pl.cukrzycowy.ollee.glycemia/.MainActivity

echo "✅ App launched on $DEVICE"
