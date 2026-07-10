# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-07-11

### Added

- **Multiple data sources**: Support for receiving glucose data from xDrip+ and GlucoDataHandler
- **Multiple watch support**: Pair and manage multiple Ollee watches simultaneously
- **Real-time synchronization**: Automatic glucose data synchronization to paired watches via Bluetooth
- **Persistent background service**: Continuous glucose monitoring even when app is closed
- **Smart watch reconnection**: Automatic detection and reconnection to paired watches
- **Battery optimization**: Efficient background operation with foreground service mode
- **Glucose history visualization**: Graph display of historical glucose readings
- **Pluggable provider architecture**: Extensible design for adding new glucose data sources
- **Bluetooth permission management**: Comprehensive Bluetooth Connect and Scan permission handling
- **Smooth data interpolation**: Synthetic glucose value generation using Hermite interpolation

[1.0.0]: https://github.com/dlvoy/ollee-glycemia/releases/tag/v1.0.0
