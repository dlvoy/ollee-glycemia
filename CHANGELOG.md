# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.6.0] - 2026-07-16

### Added

- **Disclaimer dialog with user acceptance storage**: New on-launch disclaimer dialog for legal acknowledgment
  - Dialog shown on first app launch with accept/decline options
  - User acceptance state persisted in shared preferences
  - Only shown once per app lifecycle unless explicitly reset
- **Documentation enhancements**: Screensaver functionality documentation and visual guides
  - Updated README with description of screensaver features and display modes
  - Added screenshots and setup instructions for improved clarity


## [1.5.0] - 2026-07-16

### Added

- **Nightscout provider support**: Connect directly to your Nightscout instance for remote glucose data
  - Full Nightscout HTTP API integration with authentication via API tokens
  - Configurable Nightscout URL and API token in provider settings
  - Test connection button to verify Nightscout access before saving
  - Adaptive fetch scheduling with intelligent retry and delay calculation
  - Comprehensive error handling and logging for debugging
  - 19 integration tests verifying HTTP client, parsing, and scheduling logic
  - Complete setup documentation with step-by-step screenshots and troubleshooting
- **Provider configuration backup/restore**: Save and restore settings for all configured providers
  - Backs up configurations for all providers (xDrip, GlucoDataHandler, Nightscout, VirtualHuman)
  - Previously only backed up the active provider; now includes all provider configurations
  - Restore automatically applies each provider's saved settings to the correct provider instance
- **Documentation**: Added comprehensive Nightscout setup guide
  - Step-by-step instructions with 5 screenshots showing Nightscout Admin Tools workflow
  - Instructions for creating API tokens and configuring Ollee Glycemia
  - Troubleshooting section for common connection and access issues
  - Updated README to mention Nightscout as a supported data source alongside xDrip+ and GlucoDataHandler

### Fixed

- **Scheduled fetch threading**: Network requests on scheduled fetches now run on background thread
  - Prevents `NetworkOnMainThreadException` when Nightscout fetches are triggered by handler callbacks
  - Uses explicit thread wrapping in scheduler to ensure background execution


## [1.4.0] - 2026-07-14

### Added

- **Backup and restore preferences**: Export and import your complete app configuration
  - Backs up watch list, activity labels, night auto-pause schedule, graph settings, and 24-hour glucose history
  - Backup files stored in `Downloads/OlleeGlycemia/` and survive app uninstall/reinstall
  - Manually import via **Settings → Backup preferences → Import preferences**
  - First-run restore prompt shown automatically if backup found and no watches paired
- **Storage permission management**: Dedicated storage access permission in **Settings → Permissions**
  - Required for backup/restore functionality to persist across reinstalls
  - First-run prompt with option to grant access or skip
  - "User declined prompt" flag prevents repeated prompts until manually enabled
- **Developer options improvements**: Enhanced debug mode visibility and control
  - Debug mode status indicator in **Settings → About** when enabled
  - Developer options unlock status shown in About section
  - Added debug functionality in BleService for connection troubleshooting

### Fixed

- **BleService timeout handling**: Improved error handling in connection timeout logic
  - Better stability during watch reconnection scenarios


## [1.3.0] - 2026-07-12

### Added

- **Splash screen assets**: New glycemia vector drawable and updated splash screen background image
  - Replaces unused launcher images with improved visual assets

### Fixed

- **Settings screen scrollability**: Settings page now scrollable when multiple sections are expanded on smaller screens
  - All UI elements remain reachable regardless of screen height and open sections
- **VSCode Java configuration**: JAVA_HOME environment variable now properly respected on Linux/WSL
  - Cross-platform compatibility: uses `$JAVA_HOME` if set, falls back to platform-specific detection (macOS `/usr/libexec/java_home` or Linux standard paths)


## [1.2.2] - 2026-07-12

### Changed

- **Changelog versioning workflow**: Changelog bump scripts now automatically add release tag links to CHANGELOG.md, eliminating manual link entry
  - Tag links are inserted in markdown format at the end of the file following Keep a Changelog conventions
  - Git tagging remains manual (user controls `git tag` after commit)


## [1.2.1] - 2026-07-12

### Added

- **Semantic versioning skills**: New `/changelog-major`, `/changelog-minor`, and `/changelog-patch` Claude Code skills for version bumping
  - Each skill runs `scripts/bump-version.sh` with the appropriate bump type and guides changelog entry writing
  - Skills split into separate commands per bump type, with refined step-by-step instructions and syntax


## [1.2.0] - 2026-07-12

### Added

- **xDrip SGV timestamp handling**: Back-filled readings now correctly placed in history at measurement time, not arrival time
  - Extracts actual timestamp from xDrip broadcasts (supports both legacy and newer broadcast formats)
  - Only newest reading displayed as current in UI and sent to watches
  - Historical readings added to graph regardless of arrival order
- **No-data state handling**: New BgEstimateNoData broadcast indicates sensor connection with no current reading
  - Shows "---" on watch when xDrip has no data but is still connected
  - Helps distinguish between "no connection" vs "waiting for data"

### Fixed

- **xDrip broadcast receiving**: Fixed timestamp field detection to use correct field names
  - Tries `bg.timeStamp` (newer API) or `Extras.Time` (older API)
  - Fallback to current time if timestamp not available (maintains backward compatibility)
- **Gradle Java configuration**: Automatic detection of Java 21 from multiple sources prevents VSCode restart issues

## [1.1.1] - 2026-07-12

### Fixed

- **Translated "since" label**: Time display for CONNECTING state now uses proper translations instead of hardcoded English
- **Notification message for inactive watches**: Shows "All watches paused or stopped" instead of "No paired watches" when all paired watches are inactive
- **Manual sync offline state**: Updating lastSuccessfulSyncTime when manual sync is triggered to prevent immediate offline timeout and restore active refresh schedule

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

[1.6.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.6.0
[1.5.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.5.0
[1.4.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.4.0
[1.3.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.3.0
[1.2.2]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.2.2
[1.2.1]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.2.1
[1.2.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.2.0
[1.1.1]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.1.1
[1.1.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.1.0
[1.0.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.0.0
