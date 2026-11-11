# 17: Watch row status icons

## Problem
Each watch row on the main screen shows its state only as a colored word (Synced, Connecting, Offline, Error, Paused, Stopped). It takes reading to tell the rows apart at a glance. The Forge part of the row is a plain "Watchface" text marker, which only says the firmware has the Forge service. It says nothing about whether the glycemia face is on the watch.

## Goal
A small vector icon left of the status label, one per state, in the label's color. On Forge watches a second icon next to it shows whether the glycemia face was detected.

## Scope
- Status icon (Material, from `material-icons-extended`, already a dependency), tinted with the status color:

  | Label | Icon |
  |---|---|
  | Synced | `CheckCircle` |
  | Connecting | `Sync` |
  | Offline | `LinkOff` |
  | Error | `Error` |
  | Paused | `PauseCircle` |
  | Stopped | `StopCircle` |

  The icon follows the label and the tint follows the existing color rules, so a Synced row with stale data (`--- --`, `Err`) keeps its gray color and gets a gray check.
- Watchface icon, after the status icon, only when `PairedWatch.forgeSupported`:
  - face detected: `Watch` in the status color
  - not detected: `WatchOff` in gray
  - no Forge service: no icon
- `PairedWatch.faceDetected`, persisted by `WatchStore` like `forgeSupported`, with `WatchStore.setFaceDetected`. Nothing sets it yet: spec 16 (handshake) does. Until then Forge watches show the gray icon.
- The "Watchface" text marker and its strings are removed, since the icon replaces them. Content descriptions for the watchface icon in EN, PL and FR.
- The color, label and icon choice move into one pure function, so a unit test can pin it down.

## Out of scope
- Detecting the face (spec 16).
- Changing any status color, label text or click behaviour.
- Icons in the status banner or the watch actions dialog.

## Acceptance criteria
- [ ] Every status label has its own icon in the label's color.
- [ ] Forge watches show `WatchOff` in gray while `faceDetected` is false and `Watch` in the status color when it is true. Watches without Forge show no watchface icon.
- [ ] The "Watchface" text marker is gone from the row and from the string resources.
- [ ] Colors and labels for every state are the same as before (unit test).
- [ ] `faceDetected` survives a `WatchStore` round trip and reads as false from data saved by older versions.
- [ ] `gradlew testDebugUnitTest` passes.
