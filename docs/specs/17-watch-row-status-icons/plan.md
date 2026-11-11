# 17: Watch row status icons (plan)

## Approach
Decisions taken with the user (2026-11-11):
- The icons ship now. The watchface icon reads a stored flag that spec 16 will set.
- The watchface icon is hidden on firmware without Forge, and the text marker goes away.

`MainScreen.kt` computes the row color, label and background in two `when` chains inside the composable. They move to `ui/screens/WatchStatusStyle.kt` as a pure function `watchStatusStyle(status: WatchStatus): WatchStatusStyle`. It returns the label string resource, the status icon, the color, the pill background, and the watchface icon with its tint and description (or no watchface icon). The two chains keep their order, because they disagree on purpose in a few cases. For example, a SYNCED watch that is offline by timeout reads "Synced" in gray. The icon comes from the label branch, the tint from the color branch.

`isStaleData` becomes an extension property `WatchStatus.isStaleData` in the same file, since `MainScreen` still uses it for click handling.

Row layout: `[status icon][watchface icon] Label  detail  "value"`. The icons are 14 dp, in a nested `Row` with 3 dp spacing together with the label, inside the existing detail row.

## Files
- `ui/screens/WatchStatusStyle.kt` (new): `WatchStatusStyle`, `watchStatusStyle()`, `WatchStatus.isStaleData`
- `ui/screens/MainScreen.kt`: use the style, draw the icons, drop the text marker and the unused `effectiveState`
- `PairedWatch.kt`: `faceDetected: Boolean = false`
- `WatchStore.kt`: read and write `faceDetected` (default false), `setFaceDetected()`
- `res/values/strings.xml`, `values-pl`, `values-fr`: remove `watch_watchface_marker`, add `watch_face_detected` and `watch_face_not_detected`
- `PreferencesBackupManager.kt`: no change. The backup holds no per-watch Forge state, and the flag is set again on the next connection.

## Steps
1. `PairedWatch.faceDetected` and `WatchStore` persistence, with tests.
2. `WatchStatusStyle.kt` and its test.
3. `MainScreen.kt` layout, strings.
4. CHANGELOG entry under `[Unreleased]`.

## Tests
- `WatchStatusStyleTest` (plain JUnit): label, icon and color for each active state, the stale-data and offline-by-timeout cases, paused and stopped, and the three watchface icon cases.
- `WatchStoreTest`: `faceDetected` round trip, false for entries saved without the key.
- Manual: the watch list in the app with a synced, an offline and a paused watch.
