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

## TODO
- Prevent outdated/incorrect values from remaining on the watch (sync issues)
- Add delta values
- Add notifications for low and high glucose levels


## Others

I find that GlucoDataHandler is more reliable than xDrip

Feel free to fork this repository and modify it.

According to the Ollee Watch roadmap (https://www.olleewatch.com/blog/feature-roadmap-october-2025
), many updates are planned, including support for third-party watch faces. In the meantime, this application is a solid workaround!
