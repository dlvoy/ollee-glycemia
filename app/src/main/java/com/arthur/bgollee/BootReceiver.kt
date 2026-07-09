package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {

            val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
            val addr = prefs.getString("device_address", null)
            val selectedProvider = prefs.getString(GlycemiaProviderManager.PREF_KEY, GlycemiaProviderManager.DEFAULT_ID)

            if (addr != null || selectedProvider != GlycemiaProviderManager.DEFAULT_ID) {

                val serviceIntent = Intent(context, BleService::class.java)
                addr?.let { serviceIntent.putExtra("device_address", it) }

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    // Log the error or handle it
                }
            }
        }
    }
}