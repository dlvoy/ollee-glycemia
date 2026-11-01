# 01: Delta, broadcast compatibility and branding

## Problem
The prototype showed only the value and arrow, accepted only xDrip+'s own broadcast, had a generic look, and had to be built locally.

## Goal
Turn the prototype into something worth sharing: show the delta, accept compatible broadcasts from other apps, give it an identity and localisation, and build APKs in CI.

## Scope
- Delta (change since the last reading) shown on the watch
- Accept xDrip+-compatible broadcasts (`com.eveningoutpost.dexdrip.BROADCAST`, `org.nightscout.android.broadcast`), which GlucoDataHandler can send
- Button and connection status in the main UI
- App logo, launcher icon, splash screen
- Translation cleanup and per-app locale configuration
- GitHub Actions workflow building APKs, with release signing support

## Out of scope
- Architecture changes (see spec 02)

## Acceptance criteria
- [x] The watch shows the delta alongside the value
- [x] Readings from compatible broadcasts are accepted
- [x] CI produces an APK artifact
- [x] The app has its own icon and splash screen
