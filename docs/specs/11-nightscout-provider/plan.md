# 11: Nightscout provider (plan)

## Approach
Keep the pure logic (parsing, URL normalisation, scheduling maths) in plain Kotlin objects that are easy to test, and keep Android/threading concerns in `NightscoutProvider`. `calculateNextFetchDelay()` predicts the next reading from the last one's timestamp and switches to short retries when it is late.

## Files
- `NightscoutHttpClient.kt`, `NightscoutModels.kt`, `NightscoutUrlNormalizer.kt`
- `NightscoutProvider.kt`, `NightscoutScheduler.kt`, `NightscoutFetchState.kt`
- `ui/screens/NightscoutProviderConfigScreen.kt`, `ProviderConfigScreen.kt`, `ui/nav/*`
- `GlycemiaProviderManager.kt`, `PreferencesBackupManager.kt`, `BleService.kt`
- `docs/setup/NIGHTSCOUT_SETUP.md`, `docs/setup/XDRIP_SETUP.md`, `README.md`

## Steps
1. xDrip+ setup guide
2. Provider core
3. HTTP client
4. Adaptive scheduling
5. Config screen and navigation
6. Integration tests
7. Background threading, logging, UI polish, translations
8. Backup of all provider configs
9. Nightscout setup guide

## Tests
- `NightscoutHttpClientTest`, `NightscoutParsingTest`, `NightscoutSchedulerTest`, `NightscoutProviderIntegrationTest`
