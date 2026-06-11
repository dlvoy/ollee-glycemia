package com.arthur.bgollee

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat

class XdripReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action

        // ========================
        // 🔍 DEBUG (to keep temporarily)
        // ========================
        val extras = intent.extras
        if (extras != null) {
            for (key in extras.keySet()) {
                Log.d("XDRIP", "[$action] KEY: $key = ${extras.get(key)}")
            }
        }

        when (action) {

            // ─── Compatible broadcast (Nightscout / GlucoDataHandler compatible) ───
            // Keys: glucose_value (Double), delta (Double), trend_arrow (Int), timestamp (Long)
            "org.nightscout.android.broadcast" -> {
                handleNightscoutBroadcast(context, intent)
            }

            // ─── xDrip+ native broadcast ─────────────────────────────────────────
            // Keys: com.eveningoutpost.dexdrip.Extras.BgEstimate (Double),
            //       com.eveningoutpost.dexdrip.Extras.BgSlope (Double),
            //       com.eveningoutpost.dexdrip.Extras.Time (Long)
            "com.eveningoutpost.dexdrip.BgEstimate",
            "com.eveningoutpost.dexdrip.BROADCAST" -> {
                handleXdripBroadcast(context, intent)
            }

            else -> {
                Log.d("XDRIP", "ACTION ignored: $action")
            }
        }
    }

    // ========================
    // 🩸 NIGHTSCOUT COMPATIBLE BROADCAST
    // ========================

    private fun handleNightscoutBroadcast(context: Context, intent: Intent) {

        val glucoseValue = intent.getDoubleExtra("glucose_value", Double.NaN)
        if (glucoseValue.isNaN() || glucoseValue == 0.0) {
            Log.e("XDRIP", "❌ [Nightscout] glucose_value not found or zero")
            return
        }

        val delta: Double? = if (intent.hasExtra("delta"))
            intent.getDoubleExtra("delta", 0.0)
        else null

        val trendArrow = if (intent.hasExtra("trend_arrow"))
            intent.getIntExtra("trend_arrow", 0)
        else null

        val bg = glucoseValue.toInt().toString()

        val trend = when (trendArrow) {
            6, 7 -> "UP2"     // DoubleUp, TripleUp
            5    -> "UP"      // SingleUp
            4    -> "FLAT"    // Flat
            3    -> "DOWN"    // SingleDown
            1, 2 -> "DOWN2"   // DoubleDown, TripleDown
            else -> null
        }

        Log.d("XDRIP", "🩸 [Nightscout] BG=$bg | trend=$trend | 📉 delta=$delta")

        dispatchToService(context, bg, trend, delta)
    }

    // ========================
    // 🩸 XDRIP+ NATIVE BROADCAST
    // ========================

    private fun handleXdripBroadcast(context: Context, intent: Intent) {

        val extras = intent.extras ?: run {
            Log.e("XDRIP", "❌ No extras")
            return
        }

        // ── BG VALUE ──────────────────────────────────────────────────────────
        val bgValue = extras.get("com.eveningoutpost.dexdrip.Extras.BgEstimate")

        val bg = when (bgValue) {
            is Double -> bgValue.toInt().toString()
            is Float  -> bgValue.toInt().toString()
            is Int    -> bgValue.toString()
            else      -> bgValue?.toString()
        } ?: run {
            Log.e("XDRIP", "❌ BG not found")
            return
        }

        // ── TREND via SLOPE ───────────────────────────────────────────────────
        val slope = when (val s = extras.get("com.eveningoutpost.dexdrip.Extras.BgSlope")) {
            is Double -> s
            is Float  -> s.toDouble()
            is Int    -> s.toDouble()
            else      -> null
        }

        val trend = when {
            slope == null -> null
            slope > 3     -> "UP2"
            slope > 1     -> "UP"
            slope < -3    -> "DOWN2"
            slope < -1    -> "DOWN"
            else          -> "FLAT"
        }

        // ── DELTA (plain "delta" key — present in some xDrip+ versions) ──────
        val delta: Double? = when (val d = extras.get("delta")) {
            is Double -> d
            is Float  -> d.toDouble()
            is Int    -> d.toDouble()
            else      -> null
        }

        Log.d("XDRIP", "🩸 [xDrip] BG=$bg | 📈 slope=$slope → trend=$trend | 📉 delta=$delta")

        dispatchToService(context, bg, trend, delta)
    }

    // ========================
    // 🚀 DISPATCH TO BLE SERVICE
    // ========================

    private fun dispatchToService(context: Context, bg: String, trend: String?, delta: Double?) {

        // Save locally for UI
        val prefs = context.getSharedPreferences("data", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("last_bg", bg)
            .putFloat("last_delta", delta?.toFloat() ?: Float.NaN)
            .putLong("last_time", System.currentTimeMillis())
            .apply()

        // Notify UI
        context.sendBroadcast(
            Intent("BG_UPDATED").setPackage(context.packageName)
        )

        // Start BLE service with all data
        val serviceIntent = Intent(context, BleService::class.java).apply {
            putExtra("bg", bg)

            // send the trend only if available
            trend?.let { putExtra("trend", it) }

            // send delta only if available
            delta?.let { putExtra("delta", it) }
        }

        ContextCompat.startForegroundService(context, serviceIntent)
    }
}