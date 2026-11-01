# 04: History graph and BLE permissions (plan)

## Approach
Each reading from a provider is appended to `GlycemiaHistoryStore` (shared preferences, entries older than 24 h dropped). The graph reads `getRecentEntries(maxAgeMs)` for the chosen range. `BlePermissionHelper` wraps the version-specific permission checks used by the UI and service.

## Files
- `GlycemiaHistoryStore.kt`, `GlycemiaHistoryEntry.kt`
- `GlycemiaGraphView.kt`
- `BlePermissionHelper.kt`
- `BleService.kt`, `AndroidManifest.xml`

## Steps
1. History store and entry model
2. Graph view with range selection
3. Bluetooth permission helper and UI wiring
4. Service permission and graph range adjustments

## Tests
- `GlycemiaHistoryStoreTest`
