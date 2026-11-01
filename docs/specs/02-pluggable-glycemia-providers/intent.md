# 02: Pluggable glycemia providers

## Problem
Glucose input was hard-wired to the xDrip+ receiver, so adding Nightscout or a test source would have meant touching the BLE code.

## Goal
A provider abstraction: the service consumes `GlycemiaReading`s from whichever provider the user picked, and new sources can be added without touching the BLE side.

## Scope
- `GlycemiaProvider` interface (`id`, `displayName`, `start`, `stop`)
- `GlycemiaProviderManager` registry, with the selected provider stored in preferences (default `xdrip`)
- `XdripProvider` wrapping the existing broadcast handling
- `ConstantProvider` and `VirtualHumanProvider` for testing without a sensor
- `ConfigurableGlycemiaProvider` for providers with settings (config spec, saved config, summary)
- Provider selection and configuration UI

## Out of scope
- Nightscout (spec 11)

## Acceptance criteria
- [x] The provider can be switched in the app without a restart
- [x] Readings from any provider reach the watches the same way
- [x] Test providers produce data without a CGM
