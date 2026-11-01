# 08: xDrip+ timestamps and no-data state (plan)

## Approach
`XdripProvider` maps each intent action to a reading with an extracted timestamp. `BleService.onGlycemiaReading()` always appends to history but only calls `handleBg()` when the reading is the newest seen.

## Files
- `XdripProvider.kt`, `GlycemiaReading.kt`, `BleService.kt`, `GlycemiaHistoryStore.kt`

## Steps
1. Timestamp on `GlycemiaReading`, newest-only display logic
2. Timestamp extraction for both xDrip+ formats
3. `BgEstimateNoData` handling

## Tests
- `XdripProviderTest`, `BleServiceGlycemiaReadingTest`, `GlycemiaHistoryStoreTest`
