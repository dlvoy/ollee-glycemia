# 12: Disclaimer and documentation (plan)

## Approach
`AppNavHost` checks `DisclaimerStore.hasUserAcceptedDisclaimer()` first. If not accepted, it shows the dialog and runs the existing first-run checks from the dialog's accept callback.

## Files
- `DisclaimerStore.kt`, `ui/components/DisclaimerDialog.kt`, `ui/nav/AppNavHost.kt`
- `app/build.gradle.kts` (Markwon dependency)
- `res/values*/strings.xml`
- `README.md`, `docs/setup/*`, images

## Steps
1. README and guide images, screensaver section
2. Disclaimer store and dialog, first-run ordering

## Tests
Manual on a fresh install.
