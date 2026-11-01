# 00: bgollee prototype

## Problem
Ollee watches can show a short custom text, but there was no way to get a live CGM reading onto one. People using xDrip+ had to look at the phone.

## Goal
A minimal Android app that listens for xDrip+ glucose broadcasts and pushes the current value to an Ollee watch over BLE.

## Scope
- Receive xDrip+ `BgEstimate` broadcasts
- Format the value for the watch's small display, in mg/dL and mmol/L
- Show a trend arrow
- Detect error or missing values and show an error marker instead of a stale number
- Foreground service plus boot receiver so it keeps running

## Out of scope
- Multiple watches, a settings UI, other data sources, history

## Acceptance criteria
- [x] A new xDrip+ reading shows up on a paired Ollee watch
- [x] mmol/L readings are shown with one decimal
- [x] The trend arrow is shown next to the value
- [x] Invalid input shows an error marker (`Err`)
