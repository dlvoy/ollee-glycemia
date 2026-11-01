# 05: Multi-watch pairing and app UI

## Problem
The prototype supported one hard-coded watch and had almost no UI. People with more than one Ollee watch, or who swap watches, could not manage them.

## Goal
A proper Compose app: pair and manage several watches, see each watch's sync state, and resend or manually sync when something is off.

## Scope
- Navigation (`AppNavHost`, `Route`) with main, settings, pairing and provider screens
- `WatchPairingScreen`: scan and pair, custom names
- `WatchStore` (persisted `PairedWatch` list), one `WatchConnection` per watch
- Per-watch state `CONNECTING / SYNCED / OFFLINE / ERROR`, last sent value, stale-data indicator
- Resend the current value to all watches; manual sync of one watch
- Offline timeout (30 minutes) before a watch is shown as offline
- String resources and translations (EN, PL, FR)
- Package rename to `pl.cukrzycowy.ollee.glycemia`
- Persistent notification with sync status, restart on boot

## Out of scope
- Pausing or stopping watches (spec 07)

## Acceptance criteria
- [x] Several watches can be paired and all receive readings
- [x] Each watch shows its state and last sync
- [x] A user can resend or manually sync
- [x] The UI is fully translated
