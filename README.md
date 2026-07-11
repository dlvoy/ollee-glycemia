# Ollee Glycemia

Ollee Glycemia is an Android app that bridges your glucose monitoring system (xDrip or GlucoDataHandler) with your Ollee Watch, enabling real-time glucose data display on your wrist.

## Features

- **Multiple data sources**: Receive glucose data from xDrip+ or GlucoDataHandler
- **Multiple watch support**: Pair and manage multiple Ollee watches
- **Watch activity states**: Pause or stop individual watches without deleting them
- **Night auto-pause**: Automatically pause watches during configured night time hours
- **Real-time synchronization**: Automatically sends glucose readings to your watch via Bluetooth
- **Persistent service**: Continues monitoring glucose data even when the app is closed
- **Smart watch reconnection**: Automatically detects and reconnects to paired watches
- **Battery optimized**: Runs efficiently in the background with foreground service mode

## ⚠️ Disclaimer

**Do not use any data displayed on the watch to make medical decisions.** The BG values shown on the watch may be inaccurate due to synchronization issues or data source delays. Always verify readings using your primary glucose monitoring device.

## Getting Started

### Prerequisites

- Android device with Bluetooth capability (API 24+)
- Ollee Watch
- xDrip+ or GlucoDataHandler app installed and configured

### Installation

1. **Download the APK**
   - Download the latest APK from the [Releases](https://github.com/dlvoy/ollee-glycemia/releases/) section
   - Or build it yourself, see [BUILDING.md](BUILDING.md)

2. **Install and Open**
   - Install the APK on your Android phone
   - Open Ollee Glycemia app

### Usage Instructions

#### Step 1: Grant Permissions

1. Tap **"Request Permissions"** in the app
2. Grant the following permissions when prompted:
   - Bluetooth Connect
   - Bluetooth Scan
   - Notifications
   - (Optional) Battery optimization exemption

#### Step 2: Pair Your Watch with OS Bluetooth

1. Go to your phone's **Bluetooth Settings**
2. Ensure Bluetooth is enabled on your Ollee Watch
3. Scan for devices and connect to your watch
   - Your watch may appear under "Rarely used devices" if not recently paired
4. Wait for the pairing to complete

#### Step 3: Select Your Watch in the App

1. In Ollee Glycemia, tap **"Select a watch"**
2. Choose your Ollee Watch from the list
3. The app will now start syncing glucose data to this watch

#### Step 4: Managing Your Watches

**Pause or Stop a Watch**
- Tap on the watch pill to open the actions menu, or long-press for options
- **Pause**: Watch stops receiving glucose updates but can be resumed anytime
- **Stop**: Similar to pause; useful to temporarily disable a watch without deleting it

**Resume a Paused/Stopped Watch**
- Tap the Play icon on a paused/stopped watch to resume it
- The watch will immediately start receiving glucose updates again

**Night Auto-Pause**
- Go to **Settings → Night Auto Pause** to enable automatic pausing during night hours
- Configure the pause time range (default: 23:00 - 6:00)
- Active watches will automatically pause at the start time and resume at the end time
- Useful for extending battery life or preventing overnight notifications

#### Step 5: Configure Your Glucose Data Source

**Configure xDrip+:**
- Open xDrip+ app
- Go to **Settings → Inter-app settings**
- Enable "Broadcast data locally" and "Send BG data to other apps"
- Tap **"Identify Receiver"** and enter: `pl.cukrzycowy.ollee.glycemia`
  - If other apps are already listed, add it with a space separator

**Configure GlucoDataHandler:**
- Open GlucoDataHandler
- Go to **Settings → Transfer Values**
- Enable "Local Applications" and click on it
- Enable "Send xDrip+ Broadcast"
- Go to **Identify xDrip+ broadcast receivers** and select "Ollee Glycemia"

### Viewing Glucose Data on Your Watch

Once configured, when a new glucose reading is received:
- **Long-press the bottom-right button** on your watch to display your current glucose level
- The value will remain displayed until you press the button again or a new reading arrives

### Reconnecting Your Watch

If Bluetooth is disconnected (the watch appears offline or the Bluetooth indicator is not visible):
1. **Long-press the bottom-right button twice** on your watch
2. This will re-enable Bluetooth and resync the latest glucose data

## Building & Releasing

For local build instructions, versioning, and CI details, see [BUILDING.md](BUILDING.md).

For how to cut a new tagged release (and configure the release GitHub Action), see [RELEASING.md](RELEASING.md).

## Troubleshooting

### Glucose data not syncing
- Verify your data source (xDrip+ or GlucoDataHandler) is running and sending broadcasts
- Check that "Ollee Glycemia" is added as a broadcast receiver in your source app
- Ensure the app has Bluetooth permissions

### Watch connection drops frequently
- Keep the watch within Bluetooth range (typically 10m)
- Ensure the watch Bluetooth is enabled
- Try re-pairing the watch in your phone's Bluetooth settings

### App not starting at boot
- Verify "Receive Boot Completed" permission is granted
- Check Android system settings for app launch restrictions

## Known Limitations & Future Work

- Currently only displays the most recent glucose value
- Preventing outdated/incorrect values from remaining on the watch (sync issues)
- Low and high glucose level notifications

## Acknowledgments

Ollee Glycemia is based on and extends the original [BGOllee](https://github.com/Arthur86000/BGOllee) project by [Arthur](https://github.com/Arthur86000). We are grateful for the foundation and inspiration provided by this original work.

This application serves as a solid workaround while awaiting official Ollee Watch support for third-party integrations. For more information, see the [Ollee Watch feature roadmap](https://www.olleewatch.com/blog/feature-roadmap-october-2025).

## Contributing

Feel free to fork this repository and submit pull requests with improvements, bug fixes, or new features.

## License

This project is licensed under the **MIT License**. See the LICENSE file for details.

## Repository

This project is maintained at: https://github.com/dlvoy/ollee-glycemia
