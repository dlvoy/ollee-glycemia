package pl.cukrzycowy.ollee.glycemia

import android.content.Context

object WatchActivityLabelStore {
    private const val PREFS_NAME = "data"
    private const val KEY_PAUSE_LABEL = "watch_pause_label"
    private const val KEY_STOP_LABEL = "watch_stop_label"
    private const val DEFAULT_PAUSE_LABEL = "PAUSE"
    private const val DEFAULT_STOP_LABEL = "STOP"

    fun getPauseLabel(context: Context): String {
        return prefs(context).getString(KEY_PAUSE_LABEL, DEFAULT_PAUSE_LABEL)?.take(5) ?: DEFAULT_PAUSE_LABEL
    }

    fun setPauseLabel(context: Context, label: String) {
        val trimmed = label.trim().take(5)
        prefs(context).edit().putString(KEY_PAUSE_LABEL, trimmed).apply()
    }

    fun getStopLabel(context: Context): String {
        return prefs(context).getString(KEY_STOP_LABEL, DEFAULT_STOP_LABEL)?.take(5) ?: DEFAULT_STOP_LABEL
    }

    fun setStopLabel(context: Context, label: String) {
        val trimmed = label.trim().take(5)
        prefs(context).edit().putString(KEY_STOP_LABEL, trimmed).apply()
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
