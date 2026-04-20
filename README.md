# BGOllee

BGOllee is an apk that collects glucose data from xdrip and send it to an Ollee Watch

# Disclaimer

Don't use any data you see on the watch to take a medical decision. It is very likely the bg you see on the watch will be false because of synchronization issues




# Install
 
- Download the apk in the release section (https://github.com/Arthur86000/BGOllee/releases/)
- Install the app and open it
- Tap on the "Request Permissions" button and accept
- Tap on the "Select a watch" button and choose your Ollee Watch
- Open the xdrip app
- Go to Settings -> Inter-app settings 
- Enable "Broadcast data locally" and "Send BG data to other apps"
- Tap on "Identify Receiver" and put "com.arthur.bgollee" (without the quotation marks and separated by a space if there is any other app listening to xdrip)
- When bg data is send to watch, long-press the bottom-right button on the watch. It will show your glucose

  /!\   If bluetooth was disconnected ('*lap*' icon not showing on watch), long-press the bottom-right button twice to reenable bluetooth and then sync the glucose data
  
 

# TODO

- Create ways to prevent wrong values behind on the watch (out-of-sync issues)
- Put delta values
- Notifications for low or high glucose


# Other

This app could be a way to show data from many apps (weather, stock, crypto, reminders, even notifications). The MainActivity.kt could easily be adapted. You are absolutely free to fork this repository and modify it to do whatever you want. Keep in mind that the Ollee Watch roadmap (https://www.olleewatch.com/blog/feature-roadmap-october-2025) shows that many updates are coming, especially opening the app to allow the creation of 3rd party watchfaces


