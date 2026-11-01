# 01: Delta, broadcast compatibility and branding (plan)

## Approach
Small, incremental changes on the prototype codebase (still `com.arthur.bgollee`), from here on maintained by dlvoy.

## Files
- `XdripReceiver.kt`: delta extraction, extra intent actions
- `BleService.kt`: delta formatting in the watch payload
- `MainActivity.kt`: button statuses, improved UI
- `AndroidManifest.xml`: extra intent filters, locale config
- `.github/workflows/`: APK build, artifact naming, signing from secrets
- `res/`: logo, icon, splash, translations

## Steps
1. Clean up translations
2. Delta support and display fix
3. CI APK build with signing, artifact naming
4. Compatible broadcasts
5. UI improvements, button statuses
6. Logo, icon, splash
7. Locale configuration

## Tests
Manual.
