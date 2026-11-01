# 07: Watch activity states and night auto-pause

## Problem
Every paired watch got every update. A watch left in a drawer or not worn at night kept showing as offline, drained its battery and inflated the "not synced" count, and the only fix was deleting it.

## Goal
Let users pause or stop individual watches, and pause them automatically at night, without losing the pairing.

## Scope
- `WatchActivityState`: `ACTIVE`, `PAUSED`, `STOPPED` on `PairedWatch`
- A configurable label sent to the watch on pause/stop (`WatchActivityLabelStore`), so the watch does not show a stale value
- Paused/stopped watches excluded from broadcasts, offline timeouts and the sync count
- Watch action menu (long-press, action icon, or tapping a synced watch) with pause/stop/delete and confirmation
- Night auto-pause: configurable window (default 23:00–06:00), alarm-based, auto-resume
- Notification and banner logic based on active watches only
- Collapsible settings sections with a time range selector

## Out of scope
- Per-watch schedules

## Acceptance criteria
- [x] A paused or stopped watch gets no readings and its label is shown
- [x] The sync count and notification ignore inactive watches
- [x] Watches pause and resume automatically in the night window, also in the background
- [x] With all watches inactive, the notification says so instead of "No paired watches"
