package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {

            val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
            val addr = prefs.getString("device_address", null)

            if (addr == null) return // 🔥 évite lancement inutile

            val serviceIntent = Intent(context, BleService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                context.startForegroundService(serviceIntent)
            else
                context.startService(serviceIntent)
        }
    }
}