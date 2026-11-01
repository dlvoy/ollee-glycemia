# 11: Nightscout provider

## Problem
Users whose CGM data lives in Nightscout, or who don't run xDrip+ on the same phone, could not use the app.

## Goal
A Nightscout provider that fetches the latest entries over HTTP, on a schedule that follows the CGM's 5-minute rhythm without hammering the server.

## Scope
- `NightscoutHttpClient` (OkHttp): `GET /api/v1/status.json` and `/api/v1/entries.json`, token auth
- `NightscoutUrlNormalizer`: URL cleanup and token validation
- `NightscoutModels` parsing, `NightscoutProvider` as a `ConfigurableGlycemiaProvider`
- `NightscoutScheduler`: adaptive fetch (5 min regular; 15 s retries in delayed mode after 60 s without a new reading; stale after 10 min; sliding delay window up to 2 min)
- `NightscoutFetchStateStore`: last fetch status shown in the config screen
- `NightscoutProviderConfigScreen`: URL, token, test-connection button, help text
- Backup/restore of all configurable providers' settings, not just the selected one
- Fetches on a background thread
- xDrip+ and Nightscout setup guides in `docs/setup/`
- PL and FR translations

## Out of scope
- Uploading to Nightscout

## Acceptance criteria
- [x] With a valid URL and token, readings arrive about every 5 minutes
- [x] The connection test reports success or the error
- [x] Network calls never run on the main thread
- [x] A backup includes the Nightscout settings

