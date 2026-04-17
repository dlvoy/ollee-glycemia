package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

class XdripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action == "com.eveningoutpost.dexdrip.BgEstimate") {

            val extras = intent.extras ?: return

            // 🔍 DEBUG (à garder au début)
            for (key in extras.keySet()) {
                android.util.Log.d("XDRIP", "KEY: $key = ${extras.get(key)}")
            }

            // 🔥 ULTRA ROBUSTE (corrigé)
            val bgValue = extras.get("com.eveningoutpost.dexdrip.Extras.BgEstimate")

            val bg = when (bgValue) {
                is Double -> bgValue.toInt().toString()
                is Float -> bgValue.toInt().toString()
                is Int -> bgValue.toString()
                else -> bgValue?.toString()
            } ?: run {
                android.util.Log.e("XDRIP", "❌ Aucune valeur trouvée")
                return
            }

            val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

            prefs.edit()
                .putString("last_bg", bg)
                .putLong("last_time", System.currentTimeMillis())
                .apply()

            // 🔥 update UI instantané
            context.sendBroadcast(
                Intent("BG_UPDATED").setPackage(context.packageName)
            )

            val serviceIntent = Intent(context, BleService::class.java)
            serviceIntent.putExtra("bg", bg)

            ContextCompat.startForegroundService(context, serviceIntent)
        }
        android.util.Log.e("XDRIP", "ACTION: ${intent.action}")
        android.util.Log.e("XDRIP", "EXTRAS: ${intent.extras}")
    }
}