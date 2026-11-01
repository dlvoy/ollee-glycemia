# 08: xDrip+ timestamps and no-data state

## Problem
xDrip+ can deliver back-filled readings after a gap. The app stamped every reading with its arrival time, so the history graph was wrong and an old value could be shown as current. When xDrip+ was connected but had no reading, the watch kept the last value.

## Goal
Place readings at their real measurement time, show only the newest as current, and show a clear "no data" state.

## Scope
- Read the timestamp from the broadcast (`bg.timeStamp` in newer xDrip+, `Extras.Time` in older), falling back to now
- `GlycemiaReading.timestamp`; history entries stored at measurement time, whatever order they arrive in
- Only readings newer than the current one update the UI and watches
- `BgEstimateNoData` handling: show `---` on the watch

## Out of scope
- Back-filling from Nightscout (spec 11 has its own freshness logic)

## Acceptance criteria
- [x] Back-filled readings land at the right place in the graph
- [x] An older reading never replaces a newer one on the watch
- [x] The no-data broadcast shows `---`
