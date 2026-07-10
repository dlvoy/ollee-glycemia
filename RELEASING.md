# Releasing Ollee Glycemia

## Overview

Releases are produced by pushing a git tag that matches `vMAJOR.MINOR.PATCH` (e.g. `v1.2.3`). This triggers `.github/workflows/release.yml`, which:

1. Builds a release APK, signed if keystore secrets are configured (see setup below), unsigned otherwise.
2. Names the artifact after the version number, e.g. `ollee-glycemia-1.2.3-release.apk`.
3. Uploads it as a workflow artifact and publishes it as a GitHub Release attached to the tag.

## Cutting a release

1. Decide the next version number, following [semantic versioning](https://semver.org/):
   - **MAJOR** — breaking changes
   - **MINOR** — new features, backward compatible
   - **PATCH** — bug fixes only
2. Tag the commit you want to release and push the tag:
   ```bash
   git tag v1.2.3
   git push origin v1.2.3
   ```
3. Watch the **Release APK** workflow run under the **Actions** tab. When it finishes, the signed (or unsigned) APK is attached to the new entry under **Releases**.

The version baked into the APK (`versionName`/`versionCode`, and the version shown in Settings → About) always comes from the tag itself — you don't need to edit `app/build.gradle.kts` before tagging. Updating the default `appVersionName` there is optional and only affects local/debug builds you run without `-PappVersionName=...`.

## One-time setup: configuring the release GitHub Action

Signing is optional — the workflow builds an unsigned APK if any of the four secrets below are missing — but required if you intend to distribute the APK for real use (unsigned APKs can't be upgraded in place and Android will warn about the missing signature).

**Step 1: Create a signing keystore** (skip if you already have one)
```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore
```
You'll be prompted for a keystore password and a key password (these can be the same).

**Step 2: Encode the keystore to Base64**
```bash
# macOS / Linux
base64 -i release.keystore | tr -d '\n'

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```
Copy the entire Base64 string.

**Step 3: Add repository secrets**

In the repository, go to **Settings → Secrets and variables → Actions** and create these four secrets:

| Secret | Value |
|--------|-------|
| `KEYSTORE_BASE64` | The Base64 string from Step 2 |
| `KEYSTORE_PASSWORD` | The keystore password from Step 1 |
| `KEY_ALIAS` | The alias used (e.g., `release`) |
| `KEY_PASSWORD` | The key password from Step 1 |

**Step 4: Confirm workflow permissions**

`release.yml` requests `permissions: contents: write`, which lets it publish a GitHub Release using the default `GITHUB_TOKEN` — no extra token or secret is needed for that part.

**Step 5: Tag and push** as described above to trigger the workflow.

⚠️ **Keep your keystore safe.** Never commit `release.keystore` to the repository, and treat the Base64 secret the same as the raw file.
