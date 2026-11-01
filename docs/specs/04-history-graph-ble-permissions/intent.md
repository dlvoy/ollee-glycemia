# 04: History graph and BLE permissions

## Problem
The app only showed the latest value, with no context. On Android 12+ BLE also needs runtime `BLUETOOTH_CONNECT`/`BLUETOOTH_SCAN` permissions, which the prototype did not request properly.

## Goal
Keep a rolling glucose history and show it as a graph, and handle Bluetooth permissions explicitly.

## Scope
- `GlycemiaHistoryStore`: persisted rolling 24 h history of `GlycemiaHistoryEntry`
- `GlycemiaGraphView`: graph with adjustable range
- `BlePermissionHelper`: check and request Bluetooth Connect and Scan
- Foreground service permissions in the manifest

## Out of scope
- Exporting history (backups came later, spec 10)

## Acceptance criteria
- [x] Readings are kept for 24 h and survive restarts
- [x] The main screen shows a graph of recent readings
- [x] Missing Bluetooth permissions are detected and can be granted from the app
