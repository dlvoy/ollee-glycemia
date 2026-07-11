# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-07-11

### Added

- **Watch activity states**: Pause or stop individual watches to exclude them from glucose updates without deleting them
  - Three states: ACTIVE (normal operation), PAUSED, and STOPPED
  - Automatic state-specific labels sent to watch (configurable in settings)
  - Paused/stopped watches excluded from sync status count
- **Night auto-pause feature**: Automatically pause active watches during configured night time hours
  - Configurable pause/resume time range (default: 23:00 - 6:00)
  - Auto-resumes paused watches when exiting pause window
  - Works reliably in background with persistent alarm scheduling
- **Watch action menu**: Long-press on watch pill to access pause/stop/delete actions
  - Also available via action icon button or click on healthy synced watches
  - Confirmation dialogs for pause and stop actions
  - Localized labels for different pause/stop states
- **Improved watch pill UI**:
  - Displays watch state (Paused, Stopped) with distinct visual styling
  - Shows state-specific labels when sent to watch
  - Displays sync timestamps or state indicators intelligently
  - Hides hardware ID when label has been sent to reduce clutter
  - Click on healthy synced watch opens action menu instead of manual sync
- **Collapsible settings sections**: Improved settings UI with expandable/collapsible sections
  - Auto-collapse permission sections when all permissions granted
  - Auto-collapse battery optimization when already optimized
  - Settings sections open by default for easier discovery
- **Enhanced UI/UX**:
  - Switch controls with improved unchecked state visibility
  - Better time display formatting (e.g., "@ 14:32")
  - Translated labels for all new features (EN, PL, FR)
  - Immediate application of pause/resume settings without waiting for scheduled check
- **Improved sync status reporting**:
  - Status indicator and notification only count active watches
  - More accurate representation of device synchronization status

### Changed

- Watch state logic now considers activity state (ACTIVE/PAUSED/STOPPED) for all operations
- Glycemia broadcasts filtered to only active watches
- Offline timeout calculation only applies to active watches
- Click behavior on watch pill: synced watches open menu, offline watches trigger manual sync

### Fixed

- Sync count no longer includes paused or stopped watches in status reporting

[1.1.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.1.0
[1.0.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.0.0

## [1.0.0] - 2026-07-10

### Added

- **Multiple data sources**: Support for receiving glucose data from xDrip+ and GlucoDataHandler
- **Multiple watch support**: Pair and manage multiple Ollee watches simultaneously
- **Real-time synchronization**: Automatic glucose data synchronization to paired watches via Bluetooth
- **Persistent background service**: Continuous glucose monitoring even when app is closed
- **Smart watch reconnection**: Automatic detection and reconnection to paired watches
- **Battery optimization**: Efficient background operation with foreground service mode
- **Glucose history visualization**: Graph display of historical glucose readings
- **Pluggable provider architecture**: Extensible design for adding new glucose data sources
- **Bluetooth permission management**: Comprehensive Bluetooth Connect and Scan permission handling
- **Smooth data interpolation**: Synthetic glucose value generation using Hermite interpolation
