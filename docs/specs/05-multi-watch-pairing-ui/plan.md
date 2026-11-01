# 05: Multi-watch pairing and app UI (plan)

## Approach
`BleService` keeps a map of `WatchConnection`s, synced with `WatchStore` (`syncConnectionsWithStore`). Each connection manages its own GATT lifecycle, staggered connects and retries, and reports state changes. The service publishes `WatchStatus` lists to the UI and updates the notification. A timeout watcher marks watches offline when there has been no successful sync for 30 minutes.

## Files
- `ui/nav/AppNavHost.kt`, `ui/nav/Route.kt`
- `ui/screens/MainScreen.kt`, `WatchPairingScreen.kt`, `SettingsScreen.kt`
- `ui/components/*` (pill buttons, status banner, header, scaffold), `ui/theme/*`
- `WatchStore.kt`, `PairedWatch.kt`, `WatchConnection.kt`, `WatchConnState.kt`
- `BleService.kt`, `AppState.kt`, `BootReceiver.kt`, `MainActivity.kt`

## Steps
1. Navigation and base UI components
2. Move strings to resources, add translations
3. Watch store, multi-connection service, pairing screen
4. `lastSentValue` and stale-data display
5. Package rename
6. Resend action
7. Manual sync and connection status handling
8. 30-minute offline timeout

## Tests
- `WatchStoreTest`, `WatchConnectionTest`
