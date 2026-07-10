# Building Ollee Glycemia

## Prerequisites

- JDK 17
- Android SDK (compileSdk 36, minSdk 24)

## Build locally

```bash
./gradlew build
```

To build a debug APK:
```bash
./gradlew assembleDebug
```

To build a release APK (unsigned unless a keystore is configured, see below):
```bash
./gradlew assembleRelease
```

## Versioning

The app uses [Semantic Versioning](https://semver.org/) (`MAJOR.MINOR.PATCH`). The default version is defined in `app/build.gradle.kts` (`appVersionName`, currently `1.0.0`).

To build with a specific version (this is how the release GitHub Action works — see [RELEASING.md](RELEASING.md)):
```bash
./gradlew assembleRelease -PappVersionName=1.2.3
```

The version code is derived automatically from the version name (`major * 10000 + minor * 100 + patch`), so it never needs to be maintained by hand.

Every build — debug and release alike — embeds the git commit hash and the build timestamp into `BuildConfig`. These are shown in the app under **Settings → About**, alongside the version number.

## Continuous Integration

`.github/workflows/build.yml` builds a debug APK on every push and pull request to `main`/`master`, and uploads it as a workflow artifact named `ollee-glycemia-<short-sha>-debug.apk`.

This workflow does not build release APKs — release builds are only produced by tagging a commit, see [RELEASING.md](RELEASING.md).

## Signing release builds locally

Release builds are signed automatically if the following environment variables are set when running `assembleRelease`; otherwise an unsigned APK is produced.

| Variable | Description |
|---|---|
| `KEYSTORE_FILE` | Path to your `.keystore` file |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

To create a keystore:
```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore
```

⚠️ **Keep your keystore safe.** Never commit `release.keystore` or your passwords to the repository.
