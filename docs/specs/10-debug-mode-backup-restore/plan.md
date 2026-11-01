# 10: Debug mode, backup and restore (plan)

## Approach
The backup is one JSON file per export in the public Downloads folder, so it survives uninstall. That needs `MANAGE_EXTERNAL_STORAGE` on newer Android, requested through a dedicated dialog. Import writes each store back and restarts the service state. `AppNavHost` runs the first-run checks.

## Files
- `PreferencesBackupManager.kt`, `StoragePermissionHelper.kt`, `StoragePermissionStore.kt`
- `ui/components/FirstRunImportDialog.kt`, `StorageAccessDialog.kt`
- `DebugStore.kt`, `BleService.kt`
- `ui/screens/SettingsScreen.kt`, `ui/nav/AppNavHost.kt`, `AndroidManifest.xml`

## Steps
1. Reconnect offline active watches on a new reading; settings refresh trigger
2. Debug mode toggle and developer options
3. Timeout error handling
4. Backup/restore with storage permission
5. Debug indicators in About

## Tests
Manual export/import round trip.
