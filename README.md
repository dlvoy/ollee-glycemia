# BGOllee

BGOllee is an APK that collects glucose data from xDrip (or GLucoDataHandler) and sends it to an Ollee Watch.

## Disclaimer

Do not use any data displayed on the watch to make medical decisions. The BG values shown on the watch may be inaccurate due to synchronization issues.


## Install

- Close the official Ollee Watch app
- Go into your phone's bluetooth settings
- Connect to your watch (make sure bluetooth is on on your watch). You may find it under "Rarely used devices"
- Download the APK from the releases section: https://github.com/Arthur86000/BGOllee/releases/
- Install and open the app
- Tap on "Request Permissions" and accept
- Tap on "Select a watch" and choose your Ollee Watch
  
For xDrip
<pre>- Open the xDrip app
- Go to Settings → Inter-app settings
- Enable "Broadcast data locally" and "Send BG data to other apps"
- Tap on "Identify Receiver" and enter com.arthur.bgollee (without quotation marks; separate with a space if other apps are already listed)
</pre>

For GlucoDataHandler
<pre>- Open the GlucoDataHandler
- Go to Settings -> Transfer Values -> Enable "Local Applications" and click on it 
- Enable "Send xDrip+ Broadcast" 
- Identify xDrip+ broadcast receivers -> Choose "BG Ollee"
</pre>


- When BG data is sent to the watch, long-press the bottom-right button to display your glucose level

⚠️ If Bluetooth is disconnected (the *'lap'* icon is not visible on the watch or blinking), long-press the bottom-right button twice to re-enable Bluetooth and sync the glucose data again

## Building Your Own APK with GitHub Actions

This repository includes a GitHub Actions workflow ([`.github/workflows/build.yml`](.github/workflows/build.yml)) that automatically builds both a **debug** and a **release** APK on every push to `main` and on every pull request.

### Download a pre-built APK

After each workflow run, the APKs are available as build artifacts:

1. Go to the **Actions** tab of your forked repository.
2. Click the latest **Build APK** run.
3. Under **Artifacts**, download `BGOllee-debug` or `BGOllee-release`.

The debug APK is always produced. The release APK is signed only when signing secrets are configured (see below); otherwise it is produced unsigned.

---

### Configure release signing (optional but recommended)

Android requires a signed APK to install on devices without enabling "Unknown sources for unsigned apps". To sign the release build automatically:

#### Step 1 – Create a keystore (skip if you already have one)

Run the following on your local machine:

```bash
keytool -genkeypair \
  -alias release \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -keystore release.keystore
```

You will be prompted to set a **keystore password** and a **key password** (these can be the same).

#### Step 2 – Encode the keystore to Base64

```bash
# macOS / Linux
base64 -i release.keystore | tr -d '\n'

# Windows (PowerShell)
[Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))
```

Copy the resulting Base64 string.

#### Step 3 – Add GitHub Secrets

In your forked repository, go to **Settings → Secrets and variables → Actions → New repository secret** and add the following four secrets:

| Secret name | Value |
|---|---|
| `KEYSTORE_BASE64` | The full Base64 string from Step 2 |
| `KEYSTORE_PASSWORD` | The keystore password you chose in Step 1 |
| `KEY_ALIAS` | The alias you used (e.g. `release`) |
| `KEY_PASSWORD` | The key password you chose in Step 1 |

#### Step 4 – Trigger a build

Push any commit or go to **Actions → Build APK → Run workflow**. The signed release APK will appear under Artifacts when the run completes.

> ⚠️ **Keep your keystore safe.** Never commit `release.keystore` to the repository. The workflow deletes the decoded file immediately after signing.

---

## TODO
- Prevent outdated/incorrect values from remaining on the watch (sync issues)
- Add notifications for low and high glucose levels


## Others

I find that GlucoDataHandler is more reliable than xDrip

Feel free to fork this repository and modify it.

According to the Ollee Watch roadmap (https://www.olleewatch.com/blog/feature-roadmap-october-2025
), many updates are planned, including support for third-party watch faces. In the meantime, this application is a solid workaround!
