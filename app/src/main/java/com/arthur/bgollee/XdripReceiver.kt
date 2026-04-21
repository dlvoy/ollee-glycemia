package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class XdripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "com.eveningoutpost.dexdrip.BgEstimate") {
            Log.d("XDRIP", "ACTION ignorée: ${intent.action}")
            return
        }

        val extras = intent.extras ?: run {
            Log.e("XDRIP", "❌ Pas d'extras")
            return
        }

        // ========================
        // 🔍 DEBUG (à garder temporairement)
        // ========================
        for (key in extras.keySet()) {
            Log.d("XDRIP", "KEY: $key = ${extras.get(key)}")
        }

        // ========================
        // 🩸 BG VALUE
        // ========================
        val bgValue = extras.get("com.eveningoutpost.dexdrip.Extras.BgEstimate")

        val bg = when (bgValue) {
            is Double -> bgValue.toInt().toString()
            is Float -> bgValue.toInt().toString()
            is Int -> bgValue.toString()
            else -> bgValue?.toString()
        } ?: run {
            Log.e("XDRIP", "❌ BG introuvable")
            return
        }

        // ========================
        // 📈 TREND via SLOPE (IMPORTANT)
        // ========================
        val slope = when (val s = extras.get("com.eveningoutpost.dexdrip.Extras.BgSlope")) {
            is Double -> s
            is Float -> s.toDouble()
            is Int -> s.toDouble()
            else -> null
        }

        val trend = when {
            slope == null -> null

            slope > 3 -> "UP2"
            slope > 1 -> "UP"
            slope < -3 -> "DOWN2"
            slope < -1 -> "DOWN"
            else -> "FLAT"
        }

        Log.d("XDRIP", "🩸 BG=$bg | 📈 slope=$slope → trend=$trend")

        // ========================
        // 💾 SAVE LOCAL
        // ========================
        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)

        prefs.edit()
            .putString("last_bg", bg)
            .putLong("last_time", System.currentTimeMillis())
            .apply()

        // ========================
        // 🔄 UPDATE UI
        // ========================
        context.sendBroadcast(
            Intent("BG_UPDATED").setPackage(context.packageName)
        )

        // ========================
        // 🚀 START BLE SERVICE
        // ========================
        val serviceIntent = Intent(context, BleService::class.java).apply {
            putExtra("bg", bg)

            // on envoie le trend seulement si dispo
            trend?.let {
                putExtra("trend", it)
            }
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}