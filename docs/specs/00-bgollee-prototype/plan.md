# 00: bgollee prototype (plan)

## Approach
Single-module app, package `com.arthur.bgollee`, by the upstream author (Wear86). A `BroadcastReceiver` parses xDrip+ intents and hands the value to a foreground `BleService`. The service keeps a GATT connection to the watch and writes the formatted string to the Nordic UART-style characteristic (service `6e400001-…`, RX characteristic `6e400002-…`) in a CRC16-framed payload.

## Files
- `BleService.kt`: GATT connection, payload framing, sending
- `XdripReceiver.kt`: broadcast parsing, trend and error detection
- `BootReceiver.kt`: restart the service after boot
- `MainActivity.kt`: minimal launcher UI

## Steps
1. Basic BLE send of a fixed value
2. xDrip+ broadcast receiver
3. mmol/L support
4. Error detection and trend arrow
5. README with setup instructions

## Tests
Manual, on a real watch.
