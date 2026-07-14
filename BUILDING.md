# Building Ollee Glycemia

## Prerequisites

- **Java 21** (required by Android Gradle plugin; Homebrew, SDKMAN, or system installation)
- Android SDK (compileSdk 36, minSdk 24)

### Java Setup

The build requires Java 21. Install via:

**macOS (Homebrew):**
```bash
brew install openjdk@21
```

**Linux (SDKMAN):**
```bash
sdk install java 21.current
```

**Or use an existing Java 21 installation:**
```bash
export JAVA_HOME=/path/to/java21
```

The build system auto-detects Java from common installation paths. If your setup isn't detected, set `JAVA_HOME` before building.

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

## Developer & Debug Mode

### Unlocking Developer Options

Developer options are hidden by default. To unlock them:

1. Go to **Settings → About**
2. Tap the **Version** line 7 times
3. You'll see a countdown toast ("X taps remaining")
4. Once unlocked, a **Developer Options** section appears in Settings

### What's Available in Developer Options

**Debug Mode Toggle**
- In debug mode, context menu on watch pill allow overriding/setting watch offline mode.

### Build-Time Debug Features

**Debug APK Build**
- Built with `./gradlew assembleDebug`
- Includes full symbol information and debugging support
- Can be installed alongside release builds (different package name variant)
- Allows debugging via Android Studio's debugger

**BuildConfig Embedded Data**
- Git commit hash (shown in **Settings → About** as "Commit")
- Build timestamp (shown in **Settings → About** as "Built")
- Useful for tracking which exact code version is running

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
