package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class XdripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "com.eveningoutpost.dexdrip.BgEstimate") {
            Log.d("XDRIP", "ACTION ignored: ${intent.action}")
            return
        }

        val extras = intent.extras ?: run {
            Log.e("XDRIP", "❌ No extras")
            return
        }

        // ========================
        // 🔍 DEBUG (to keep temporarily)
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
            Log.e("XDRIP", "❌ BG not found")
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

        // ========================
        // 📉 DELTA (mg/dL change since last reading)
        // ========================
        val delta = when (val d = extras.get("com.eveningoutpost.dexdrip.Extras.BgDelta")) {
            is Double -> d
            is Float -> d.toDouble()
            is Int -> d.toDouble()
            else -> null
        }

        Log.d("XDRIP", "🩸 BG=$bg | 📈 slope=$slope → trend=$trend | 📉 delta=$delta")

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

            // send the trend only if available
            trend?.let {
                putExtra("trend", it)
            }

            // send delta only if available
            delta?.let {
                putExtra("delta", it)
            }
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}