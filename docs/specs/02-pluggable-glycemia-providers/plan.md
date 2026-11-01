# 02: Pluggable glycemia providers (plan)

## Approach
`BleService` asks `GlycemiaProviderManager.getSelected()` for a provider and calls `start(context, onReading)`. On a switch it stops the old provider and starts the new one. Configurable providers describe their fields with a `ProviderConfigSpec` (`ProviderConfigModels.kt`), which the generic `ProviderConfigScreen` renders. Values are saved through `ProviderConfigStore`.

## Files
- `GlycemiaProvider.kt`, `ConfigurableGlycemiaProvider.kt`: contracts
- `GlycemiaProviderManager.kt`: registry and selection
- `GlycemiaReading.kt`: `bg`, `trend`, `delta`, `timestamp`
- `XdripProvider.kt`, `XdripReceiver.kt`: xDrip+ broadcasts
- `ConstantProvider.kt`, `VirtualHumanProvider.kt`: test sources
- `ProviderConfigModels.kt`, `ProviderConfigStore.kt`, `ui/screens/ProviderConfigScreen.kt`

## Steps
1. Interface and manager; port the xDrip+ handling into `XdripProvider`
2. Constant provider
3. VirtualHuman provider on top of the synthetic generator (spec 03)
4. Configurable-provider contract and generic config screen

## Tests
- `XdripProviderTest`, `ProviderConfigurationTest`, `BleServiceGlycemiaReadingTest`
