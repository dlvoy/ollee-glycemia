# BGOllee

BGOllee is an apk that collects glucose information from xdrip and send them to an Ollee Watch

# Disclaimer

Don't use any data you see on the watch to take a medical decision.




# Install
 
- Download the apk in the release section (https://github.com/Arthur86000/BGOllee/releases/)
- Install the app and open it
- Tap on the "Request Permissions" button and accept
- Tap on the "Select a watch" button and choose your Ollee Watch
- Open the xdrip app
- Go to Settings -> Inter-app settings 
- Enable "Broadcast data locally" and "Send BG data to other apps"
- Tap on "Identify Receiver" and put "com.arthur.bgollee" (without the quotation marks and separated by a space if there is any other app listening to xdrip)
- When bg data is send to watch, push the bottom-right button on the watch. It will show your glucose

  /!\   If bluetooth was disconnected ('lap' icon not showing on watch), press the bottom-right button twice to reenable bluetooth and then sync the glucose data
  
 

# TODO
- Improve Bluetooth interractions to save battery life
- Put delta values (but might be useless if an Ollee Watch update allow to create custom watchfaces)
- Notifications for low or high glucose

# Other

This app could be a way to show data from many apps (weather, stock, crypto, reminders, even notifications). The MainActivity.kt could easily be adapted but I won't do it. However, you are absolutely free to fork this repository and modify it to do whatever you want. Keep in mind that the Ollee Watch roadmap (https://www.olleewatch.com/blog/feature-roadmap-october-2025) shows that many updates are coming, especially opening the app to allow the creation of 3rd party watchfaces


