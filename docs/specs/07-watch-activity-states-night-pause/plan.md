# 07: Watch activity states and night auto-pause (plan)

## Approach
`BleService.setWatchActivity()` updates `WatchStore`, sends the label once (`activityLabelSent`) and filters connections by `ACTIVE` everywhere readings are sent or timeouts checked. `NightAutoPauseScheduler` sets `RTC_WAKEUP` alarms handled by `NightAutoPauseReceiver`. `checkAndApply()` pauses active watches inside the window and resumes the ones it paused when the window ends. Settings changes apply immediately.

## Files
- `PairedWatch.kt`, `WatchStore.kt`, `WatchActivityLabelStore.kt`
- `NightAutoPauseScheduler.kt`, `NightAutoPauseReceiver.kt`, `NightAutoPauseStore.kt`
- `BleService.kt`
- `ui/screens/MainScreen.kt` (watch pills, action menu), `SettingsScreen.kt`
- `ui/components/FoldableSection.kt`, `TimeRangeSelector.kt`, `ToggleRow.kt`
- `res/values*/strings.xml` (EN, PL, FR)

## Steps
1. Activity state model and service filtering
2. Labels and settings UI components
3. Night auto-pause scheduler and settings
4. Activity state display and action menu in MainScreen
5. Notification and banner logic for active watches
6. Fixes: translated "since" label, inactive-watch notification, manual sync offline state

## Tests
- `WatchStoreTest`, manual background checks of alarm scheduling
