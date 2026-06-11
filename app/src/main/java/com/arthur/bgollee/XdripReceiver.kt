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
        // 🔍 DEBUG (detailed intent log)
        // ========================
        logIntent(intent)

        when (action) {

            // ─── Compatible broadcast (Nightscout / GlucoDataHandler compatible) ───
            // Keys: glucose_value (Double), delta (Double), trend_arrow (Int), timestamp (Long)
            "org.nightscout.android.broadcast" -> {
                handleNightscoutBroadcast(context, intent)
            }

            // ─── Compatible broadcast (Modern JSON format) ──────────────────────────
            // Key: status (String/JSON)
            "com.eveningoutpost.dexdrip.ExternalStatusChange" -> {
                handleCompatibleBroadcast(context, intent)
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

        // ── DELTA (plain "delta" key — or calculated from slope) ──────────────────
        val delta: Double? = when (val d = extras.get("delta")) {
            is Double -> d
            is Float  -> d.toDouble()
            is Int    -> d.toDouble()
            else      -> {
                // xDrip+ slope is change per millisecond.
                // Convert to 5-minute delta: slope * 1000 * 60 * 5
                slope?.let { it * 300000.0 }
            }
        }

        Log.d("XDRIP", "🩸 [xDrip] BG=$bg | 📈 slope=$slope → trend=$trend | 📉 delta=$delta")

        dispatchToService(context, bg, trend, delta)
    }

    // ========================
    // 🩸 COMPATIBLE BROADCAST (JSON)
    // ========================

    private fun handleCompatibleBroadcast(context: Context, intent: Intent) {
        val statusJson = intent.getStringExtra("status") ?: run {
            Log.e("XDRIP", "❌ [Compatible] status JSON string not found")
            return
        }

        try {
            val json = org.json.JSONObject(statusJson)
            val sgv = json.optDouble("sgv", Double.NaN)
            if (sgv.isNaN()) {
                Log.e("XDRIP", "❌ [Compatible] sgv not found or NaN")
                return
            }

            val bg = sgv.toInt().toString()

            val delta = if (json.has("delta")) {
                json.getDouble("delta")
            } else {
                null
            }

            val direction = json.optString("direction", "")
            val trend = when (direction.lowercase()) {
                "doubleup", "tripleup" -> "UP2"
                "up", "singleup", "fortyfiveup" -> "UP"
                "flat" -> "FLAT"
                "down", "singledown", "fortyfivedown" -> "DOWN"
                "doubledown", "tripledown" -> "DOWN2"
                else -> null
            }

            Log.d("XDRIP", "🩸 [Compatible] BG=$bg | trend=$trend | 📉 delta=$delta")
            dispatchToService(context, bg, trend, delta)
        } catch (e: Exception) {
            Log.e("XDRIP", "❌ [Compatible] Error parsing JSON status", e)
        }
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

        try {
            ContextCompat.startForegroundService(context, serviceIntent)
        } catch (e: Exception) {
            Log.e("XDRIP", "❌ Failed to start foreground service: ${e.message}")
            // Fallback: try regular startService if already running or if system allows
            try {
                context.startService(serviceIntent)
            } catch (e2: Exception) {
                Log.e("XDRIP", "❌ Failed to start service: ${e2.message}")
            }
        }
    }

    private fun logIntent(intent: Intent) {
        val sb = StringBuilder()
        sb.append("\n==================================================\n")
        sb.append("📥 RECEIVED INTENT\n")
        sb.append("Action:     ${intent.action}\n")
        sb.append("Component:  ${intent.component?.flattenToString()}\n")
        sb.append("Package:    ${intent.`package`}\n")
        sb.append("Data:       ${intent.dataString}\n")
        sb.append("Type:       ${intent.type}\n")
        sb.append("Flags:      0x${Integer.toHexString(intent.flags)}\n")
        intent.categories?.let {
            sb.append("Categories: ${it.joinToString(", ")}\n")
        }
        val extras = intent.extras
        if (extras != null) {
            sb.append("Extras:\n")
            for (key in extras.keySet()) {
                val value = extras.get(key)
                val valueStr = when (value) {
                    is android.os.Bundle -> "Bundle{${value.keySet().associateWith { value.get(it) }}}"
                    is Array<*> -> value.contentToString()
                    is IntArray -> value.contentToString()
                    is LongArray -> value.contentToString()
                    is FloatArray -> value.contentToString()
                    is DoubleArray -> value.contentToString()
                    is BooleanArray -> value.contentToString()
                    is ByteArray -> value.contentToString()
                    is ShortArray -> value.contentToString()
                    is CharArray -> value.contentToString()
                    else -> value?.toString()
                }
                val typeName = value?.javaClass?.simpleName ?: "null"
                sb.append("  - $key ($typeName) = $valueStr\n")
            }
        } else {
            sb.append("Extras:     null\n")
        }
        sb.append("==================================================")
        Log.d("XDRIP", sb.toString())
    }
}