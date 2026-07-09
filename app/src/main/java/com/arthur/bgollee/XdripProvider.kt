package com.arthur.bgollee

import android.content.Context
import android.content.Intent
import android.util.Log
import org.json.JSONObject

class XdripProvider : GlycemiaProvider {

    override val id: String = "xdrip"
    override val displayName: String = "xDrip / Nightscout"

    override fun start(context: Context, onReading: (GlycemiaReading) -> Unit) {
        setCallback(onReading)
    }

    override fun stop(context: Context) {
        setCallback(null)
    }

    fun parseIntent(intent: Intent): GlycemiaReading? {
        return when (intent.action) {
            "org.nightscout.android.broadcast" -> handleNightscoutBroadcast(intent)
            "com.eveningoutpost.dexdrip.ExternalStatusChange" -> handleCompatibleBroadcast(intent)
            "com.eveningoutpost.dexdrip.BgEstimate",
            "com.eveningoutpost.dexdrip.BROADCAST" -> handleXdripBroadcast(intent)
            else -> null
        }
    }

    private fun handleNightscoutBroadcast(intent: Intent): GlycemiaReading? {
        val glucoseValue = intent.getDoubleExtra("glucose_value", Double.NaN)
        if (glucoseValue.isNaN() || glucoseValue == 0.0) {
            Log.e("XDRIP", "[Nightscout] glucose_value not found or zero")
            return null
        }

        val delta = if (intent.hasExtra("delta")) intent.getDoubleExtra("delta", 0.0) else null
        val trendArrow = if (intent.hasExtra("trend_arrow")) intent.getIntExtra("trend_arrow", 0) else null
        val trend = when (trendArrow) {
            6, 7 -> "UP2"
            5 -> "UP"
            4 -> "FLAT"
            3 -> "DOWN"
            1, 2 -> "DOWN2"
            else -> null
        }

        return GlycemiaReading(
            bg = glucoseValue.toInt().toString(),
            trend = trend,
            delta = delta,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun handleXdripBroadcast(intent: Intent): GlycemiaReading? {
        intent.extras ?: run {
            Log.e("XDRIP", "No extras in xDrip broadcast")
            return null
        }

        val bg = readNumericExtra(intent, "com.eveningoutpost.dexdrip.Extras.BgEstimate")
            ?.toInt()
            ?.toString()
            ?: intent.getStringExtra("com.eveningoutpost.dexdrip.Extras.BgEstimate")
            ?: run {
            Log.e("XDRIP", "BG not found in xDrip broadcast")
            return null
        }

        val slope = readNumericExtra(intent, "com.eveningoutpost.dexdrip.Extras.BgSlope")

        val trend = when {
            slope == null -> null
            slope > 3 -> "UP2"
            slope > 1 -> "UP"
            slope < -3 -> "DOWN2"
            slope < -1 -> "DOWN"
            else -> "FLAT"
        }

        val delta = readNumericExtra(intent, "delta") ?: slope?.let { it * 300000.0 }

        return GlycemiaReading(
            bg = bg,
            trend = trend,
            delta = delta,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun handleCompatibleBroadcast(intent: Intent): GlycemiaReading? {
        val statusJson = intent.getStringExtra("status") ?: run {
            Log.e("XDRIP", "[Compatible] status JSON string not found")
            return null
        }

        return try {
            val json = JSONObject(statusJson)
            val sgv = json.optDouble("sgv", Double.NaN)
            if (sgv.isNaN()) {
                Log.e("XDRIP", "[Compatible] sgv not found or NaN")
                return null
            }

            val trend = when (json.optString("direction", "").lowercase()) {
                "doubleup", "tripleup" -> "UP2"
                "up", "singleup", "fortyfiveup" -> "UP"
                "flat" -> "FLAT"
                "down", "singledown", "fortyfivedown" -> "DOWN"
                "doubledown", "tripledown" -> "DOWN2"
                else -> null
            }

            GlycemiaReading(
                bg = sgv.toInt().toString(),
                trend = trend,
                delta = if (json.has("delta")) json.getDouble("delta") else null,
                timestamp = System.currentTimeMillis()
            )
        } catch (error: Exception) {
            Log.e("XDRIP", "[Compatible] Error parsing JSON status", error)
            null
        }
    }

    companion object {
        private var callback: ((GlycemiaReading) -> Unit)? = null

        fun dispatch(reading: GlycemiaReading) {
            callback?.invoke(reading)
        }

        private fun setCallback(onReading: ((GlycemiaReading) -> Unit)?) {
            callback = onReading
        }
    }

    private fun readNumericExtra(intent: Intent, key: String): Double? {
        if (!intent.hasExtra(key)) return null

        val doubleValue = intent.getDoubleExtra(key, Double.NaN)
        if (!doubleValue.isNaN()) return doubleValue

        val floatValue = intent.getFloatExtra(key, Float.NaN)
        if (!floatValue.isNaN()) return floatValue.toDouble()

        val intSentinel = Int.MIN_VALUE
        val intValue = intent.getIntExtra(key, intSentinel)
        if (intValue != intSentinel) return intValue.toDouble()

        val longSentinel = Long.MIN_VALUE
        val longValue = intent.getLongExtra(key, longSentinel)
        if (longValue != longSentinel) return longValue.toDouble()

        return intent.getStringExtra(key)?.toDoubleOrNull()
    }
}