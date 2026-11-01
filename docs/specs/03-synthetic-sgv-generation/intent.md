# 03: Synthetic SGV generation

## Problem
Developing and demoing the app needed a live CGM, and real readings arrive only every 5 minutes, which makes UI and watch testing slow.

## Goal
Generate realistic synthetic glucose curves on demand, so the app can run end to end with no sensor.

## Scope
- Scenario-based anchor points (`SgvScenarioFactory`)
- Smooth curves through the anchors with Hermite interpolation
- Physiological constraints (rate of change, range) and noise
- Deltas derived from the generated series
- Used by `VirtualHumanProvider`

## Out of scope
- Prediction, or filling gaps in real data

## Acceptance criteria
- [x] The generated series is smooth, within range, and noisy like real CGM data
- [x] Deltas match the generated values
- [x] Unit tests cover the generator
