package pl.cukrzycowy.ollee.glycemia

import android.content.Context
import java.time.LocalTime

object NightAutoPauseStore {
    private const val PREFS_NAME = "data"
    private const val KEY_ENABLED = "night_auto_pause_enabled"
    private const val KEY_START_HOUR = "night_auto_pause_start_hour"
    private const val KEY_START_MINUTE = "night_auto_pause_start_minute"
    private const val KEY_END_HOUR = "night_auto_pause_end_hour"
    private const val KEY_END_MINUTE = "night_auto_pause_end_minute"

    private const val DEFAULT_START_HOUR = 23
    private const val DEFAULT_START_MINUTE = 0
    private const val DEFAULT_END_HOUR = 6
    private const val DEFAULT_END_MINUTE = 0

    fun isEnabled(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun getStartTime(context: Context): LocalTime {
        val prefs = prefs(context)
        val hour = prefs.getInt(KEY_START_HOUR, DEFAULT_START_HOUR)
        val minute = prefs.getInt(KEY_START_MINUTE, DEFAULT_START_MINUTE)
        return LocalTime.of(hour, minute)
    }

    fun setStartTime(context: Context, time: LocalTime) {
        prefs(context).edit()
            .putInt(KEY_START_HOUR, time.hour)
            .putInt(KEY_START_MINUTE, time.minute)
            .apply()
    }

    fun getEndTime(context: Context): LocalTime {
        val prefs = prefs(context)
        val hour = prefs.getInt(KEY_END_HOUR, DEFAULT_END_HOUR)
        val minute = prefs.getInt(KEY_END_MINUTE, DEFAULT_END_MINUTE)
        return LocalTime.of(hour, minute)
    }

    fun setEndTime(context: Context, time: LocalTime) {
        prefs(context).edit()
            .putInt(KEY_END_HOUR, time.hour)
            .putInt(KEY_END_MINUTE, time.minute)
            .apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
