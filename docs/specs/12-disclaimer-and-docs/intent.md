# 12: Disclaimer and documentation

## Problem
The app displays medical data but did not tell users it is not a medical device, and the README lacked screenshots of the watch display.

## Goal
Users acknowledge a disclaimer before using the app, and the docs show what the app and watch look like.

## Scope
- `DisclaimerDialog` (Markdown rendered with Markwon), shown on launch until accepted; cannot be dismissed without accepting
- `DisclaimerStore`: acceptance flag in shared preferences
- First-run checks (backup import, storage access) run only after acceptance
- README: screensaver display description and images; setup guide images
- PL and FR translations

## Out of scope
- Versioned disclaimers that ask again after a text change

## Acceptance criteria
- [x] A new install shows the disclaimer before anything else
- [x] Once accepted, it is not shown again
- [x] The README shows the watch display
