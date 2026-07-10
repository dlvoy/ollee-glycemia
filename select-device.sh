#!/bin/bash

# Device selection strategy:
# 1. Prefer USB/real devices (not emulator-*, not TCP)
# 2. Then prefer TCP devices (network connections with : or ._adb-tls-connect._tcp)
# 3. Finally take first from list

# Get list of connected devices (skip header "List of devices attached")
DEVICES=$(adb devices | tail -n +2 | grep -E 'device|emulator' | awk '{print $1}' | grep -v '^$')

if [ -z "$DEVICES" ]; then
    echo "No devices found"
    exit 1
fi

DEVICE_COUNT=$(echo "$DEVICES" | wc -l)

if [ "$DEVICE_COUNT" -eq 1 ]; then
    # Only one device, use it
    echo "$DEVICES" | head -1
    exit 0
fi

# Multiple devices - apply priority logic

# 1. Look for USB/real devices (not emulator-*, not containing : or _adb-tls-connect)
USB_DEVICE=$(echo "$DEVICES" | grep -v -E '(^emulator-|:|_adb-tls-connect)' | head -1)
if [ -n "$USB_DEVICE" ]; then
    echo "$USB_DEVICE"
    exit 0
fi

# 2. Look for TCP devices (contain : or ._adb-tls-connect._tcp which indicates network connection)
TCP_DEVICE=$(echo "$DEVICES" | grep -E '(:|_adb-tls-connect)' | head -1)
if [ -n "$TCP_DEVICE" ]; then
    echo "$TCP_DEVICE"
    exit 0
fi

# 3. Take first from list (fallback to emulator or any remaining device)
echo "$DEVICES" | head -1
