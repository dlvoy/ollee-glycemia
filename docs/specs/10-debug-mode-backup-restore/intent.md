# 10: Debug mode, backup and restore

## Problem
Reinstalling the app or moving to a new phone lost all paired watches and settings. Diagnosing connection problems had no in-app tools.

## Goal
Back up and restore the full configuration to a location that survives reinstall, and add hidden developer options for troubleshooting.

## Scope
- `PreferencesBackupManager`: export/import watches, activity labels, night pause, graph settings, provider selection and 24 h history to `Downloads/OlleeGlycemia/`
- First-run restore prompt (`FirstRunImportDialog`) when a backup exists and no watches are paired
- All-files storage access (`StoragePermissionHelper`, `StorageAccessDialog`), with a "declined" flag (`StoragePermissionStore`) so the prompt is not repeated
- Developer options unlocked by tapping the version 7 times (`DebugStore`), debug mode toggle, indicators in About
- Debug actions in `BleService` (e.g. force a watch offline)
- Active watches that went offline reconnect when a new reading arrives
- More robust timeout handling in `BleService`

## Out of scope
- Cloud backup

## Acceptance criteria
- [x] Export, uninstall, reinstall, import restores watches and settings
- [x] A fresh install with a backup present offers to restore it
- [x] Developer options stay hidden until unlocked

